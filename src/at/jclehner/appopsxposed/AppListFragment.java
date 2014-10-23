/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013 Joseph C. Lehner
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of  MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.jclehner.appopsxposed;

import java.lang.reflect.Field;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.OpsLabelHelper;
import at.jclehner.appopsxposed.util.Res;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper.OpEntryWrapper;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper.PackageOpsWrapper;

public class AppListFragment extends ListFragment implements LoaderCallbacks<List<AppListFragment.PackageInfoData>>
{
	private static final String TAG = "AOX:AppListFragment";

	private AppListAdapter mAdapter;
	private LayoutInflater mInflater;

	class AppListAdapter extends BaseAdapter
	{
		private final PackageManager mPm;
		private List<PackageInfoData> mList;

		public AppListAdapter(Context context) {
			mPm = context.getPackageManager();
		}

		public void setData(List<PackageInfoData> list)
		{
			mList = list;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mList != null ? mList.size() : 0;
		}

		@Override
		public PackageInfoData getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@SuppressWarnings("unused")
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			final ViewHolder holder;
			final PackageInfoData data = getItem(position);
			final ApplicationInfo appInfo = data.packageInfo.applicationInfo;

			if(convertView == null)
			{
				convertView = mInflater.inflate(Res.appListItemLayout, parent, false);

				holder = new ViewHolder();
				holder.appIcon = (ImageView) convertView.findViewWithTag("app_icon");
				holder.appLine2 = (TextView) convertView.findViewWithTag("app_package");
				holder.appName = (TextView) convertView.findViewWithTag("app_name");

				convertView.setTag(holder);
			}
			else
				holder = (ViewHolder) convertView.getTag();

			holder.appIcon.setImageDrawable(null);
			holder.appName.setText(data.label);
			holder.appLine2.setText(data.line2);

			if(true && !data.packageInfo.packageName.equals(holder.packageName))
			{
				if(holder.task != null)
					holder.task.cancel(true);

				holder.task = new AsyncTask<Void, Void, Object[]>() {
					@Override
					protected Object[] doInBackground(Void... params)
					{
						final Object[] result = new Object[2];
						result[0] = appInfo.loadIcon(mPm);
						//result[1] = appInfo.loadLabel(mPm);
						return result;
					}

					@Override
					protected void onPostExecute(Object[] result)
					{
						holder.appIcon.setImageDrawable((Drawable) result[0]);
						/*holder.appName.setText((CharSequence) result[1]);

						if(!appInfo.packageName.equals(result[1].toString()))
						{
							holder.appLine2.setText(appInfo.packageName);
							holder.appLine2.setVisibility(View.VISIBLE);
						}
						else
							holder.appLine2.setVisibility(View.GONE);*/
					}

				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

				holder.packageName = appInfo.packageName;
			}
			else
			{
				holder.appIcon.setImageDrawable(appInfo.loadIcon(mPm));
				holder.appName.setText(appInfo.loadLabel(mPm));
			}

			return convertView;
		}
	}

	static class ViewHolder
	{
		String packageName;
		AsyncTask<Void, Void, Object[]> task;

		ImageView appIcon;
		TextView appName;
		TextView appLine2;
	}

	static class LoaderDataComparator implements Comparator<PackageInfoData>
	{
		private static Collator sCollator = Collator.getInstance();

		@Override
		public int compare(PackageInfoData lhs, PackageInfoData rhs) {
			return sCollator.compare(lhs.label, rhs.label);
		}
	}

	static class PackageInfoData
	{
		final PackageInfo packageInfo;
		final CharSequence label;
		CharSequence line2;

		PackageInfoData(PackageInfo packageInfo, CharSequence label)
		{
			this.packageInfo = packageInfo;
			this.label = label;
			line2 = packageInfo.packageName;
		}
	}

	static class AppListLoader extends AsyncTaskLoader<List<PackageInfoData>>
	{
		private static String[] sOpPerms = getOpPermissions();

		private final PackageManager mPm;
		private List<PackageInfoData> mData;

		private final boolean mRemoveAppsWithUnchangedOps;

		public AppListLoader(Context context, boolean removeAppsWithUnchangedOps)
		{
			super(context);
			mPm = context.getPackageManager();
			mRemoveAppsWithUnchangedOps = removeAppsWithUnchangedOps;
		}

		@Override
		public List<PackageInfoData> loadInBackground()
		{
			final List<PackageInfo> packageInfos = mPm.getInstalledPackages(
					PackageManager.GET_PERMISSIONS | PackageManager.GET_DISABLED_COMPONENTS);

			if(sOpPerms != null)
				removeAppsWithoutOps(packageInfos);

			final List<PackageInfoData> data = new ArrayList<PackageInfoData>();

			for(PackageInfo packageInfo : packageInfos)
			{
				CharSequence label;
				try
				{
					label = packageInfo.applicationInfo.loadLabel(mPm);
				}
				catch(Resources.NotFoundException e)
				{
					label = packageInfo.packageName;
				}

				data.add(new PackageInfoData(packageInfo, label));
			}

			if(mRemoveAppsWithUnchangedOps)
				removeAppsWithUnchangedOps(data);

			Collections.sort(data, new LoaderDataComparator());

			return data;
		}

		@Override
		public void deliverResult(List<PackageInfoData> data)
		{
			mData = data;

			if(isStarted())
				super.deliverResult(data);
		}

		@Override
		protected void onStartLoading()
		{
			onContentChanged();

			if(mData != null)
				deliverResult(mData);

			if(takeContentChanged() || mData == null)
				forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		protected void onReset()
		{
			super.onReset();
			onStopLoading();
			mData = null;
		}

		private void removeAppsWithUnchangedOps(List<PackageInfoData> data)
		{
			for(int i = 0; i != data.size(); ++i)
			{
				if(hasUnchangedOps(data.get(i)))
				{
					data.remove(i);
					--i;
				}
			}
		}

		private boolean hasUnchangedOps(PackageInfoData info)
		{
			final AppOpsManagerWrapper appOps = AppOpsManagerWrapper.from(getContext());
			final List<PackageOpsWrapper> pkgOpsList = appOps.getOpsForPackage(info.packageInfo.applicationInfo.uid,
					info.packageInfo.applicationInfo.packageName, null);

			boolean hasUnchangedOps = true;

			final SpannableStringBuilder ssb = new SpannableStringBuilder();

			for(PackageOpsWrapper pkgOps : pkgOpsList)
			{
				for(OpEntryWrapper op : pkgOps.getOps())
				{
					if(op.getMode() != AppOpsManager.MODE_ALLOWED)
					{
						if(ssb.length() != 0)
							ssb.append(", ");

						final SpannableString opSummary = new SpannableString(OpsLabelHelper.getOpSummary(getContext(), op.getOp()));
						opSummary.setSpan(new StrikethroughSpan(), 0, opSummary.length(), 0);
						ssb.append(opSummary);
						hasUnchangedOps = false;
					}
				}
			}

			if(!hasUnchangedOps)
				info.line2 = ssb;

			return hasUnchangedOps;
		}

		private void removeAppsWithoutOps(List<PackageInfo> data)
		{
			Log.i(TAG, "removeAppsWithoutOps: ");

			for(int i = 0; i != data.size(); ++i)
			{
				if(!hasAppOps(data.get(i)))
				{
					data.remove(i);
					--i;
				}
			}
		}

		private boolean hasAppOps(PackageInfo info)
		{
			if(info.requestedPermissions != null)
			{
				for(String permName : info.requestedPermissions)
				{
					if(isAppOpsPermission(permName))
						return true;
				}
			}

			return false;
		}

		private boolean isAppOpsPermission(String permName)
		{
			for(String opPerm : sOpPerms)
			{
				if(opPerm != null && permName.equals(opPerm))
					return true;
			}

			return false;
		}

		@TargetApi(19)
		private static String[] getOpPermissions()
		{
			try
			{
				final Field f = AppOpsManager.class.getDeclaredField("sOpPerms");
				f.setAccessible(true);
				return (String[]) f.get(null);
			}
			catch(NoSuchFieldException e)
			{
				Log.w(TAG, e);
			}
			catch(IllegalAccessException e)
			{
				Log.w(TAG, e);
			}
			catch(IllegalArgumentException e)
			{
				Log.w(TAG, e);
			}

			return null;
		}
	}

	public static AppListFragment newInstance(boolean showAppsWithChangedOpsOnly)
	{
		final Bundle args = new Bundle();
		args.putBoolean("show_changed_ops_only", showAppsWithChangedOpsOnly);
		final AppListFragment f = new AppListFragment();
		f.setArguments(args);
		return f;
	}

	@Override
	public Loader<List<PackageInfoData>> onCreateLoader(int id, Bundle args) {
		return new AppListLoader(getActivity(), getArguments().getBoolean("show_changed_ops_only"));
	}

	@Override
	public void onLoadFinished(Loader<List<PackageInfoData>> loader, List<PackageInfoData> data)
	{
		mAdapter.setData(data);

		if(isResumed())
			setListShown(true);
		else
			setListShownNoAnimation(true);
	}

	@Override
	public void onLoaderReset(Loader<List<PackageInfoData>> data) {
		mAdapter.setData(null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		Log.d(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);

		mInflater = getActivity().getLayoutInflater();
		mAdapter = new AppListAdapter(getActivity());

		setListAdapter(mAdapter);
		setListShown(false);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		final Bundle args = new Bundle();
		args.putString("package", ((ViewHolder) v.getTag()).packageName);

		if(!isInAppOpsXposedApp())
		{
			((PreferenceActivity) getActivity()).startPreferencePanel(
					AppOpsXposed.APP_OPS_DETAILS_FRAGMENT, args,
					Res.getSettingsIdentifier("string/app_ops_settings"),
					null, null, 0);
		}
		else
		{
			final Intent intent = new Intent("android.settings.SETTINGS");
			intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_DETAILS_FRAGMENT);
			intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

			startActivity(intent);
		}
	}

	private boolean isInAppOpsXposedApp() {
		return AppOpsXposed.MODULE_PACKAGE.equals(getActivity().getApplicationInfo().packageName);
	}
}
