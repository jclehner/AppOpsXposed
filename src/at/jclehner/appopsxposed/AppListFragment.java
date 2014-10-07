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
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import at.jclehner.appopsxposed.AppOpsManagerWrapper.OpEntryWrapper;
import at.jclehner.appopsxposed.AppOpsManagerWrapper.PackageOpsWrapper;

public class AppListFragment extends ListFragment implements LoaderCallbacks<List<PackageInfo>>
{
	private static final String TAG = "AOX:AppListFragment";

	private AppListAdapter mAdapter;
	private LayoutInflater mInflater;

	class AppListAdapter extends BaseAdapter
	{
		private final PackageManager mPm;
		private List<PackageInfo> mList;

		public AppListAdapter(Context context) {
			mPm = context.getPackageManager();
		}

		public void setData(List<PackageInfo> list)
		{
			mList = list;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mList != null ? mList.size() : 0;
		}

		@Override
		public PackageInfo getItem(int position) {
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
			final ApplicationInfo appInfo = getItem(position).applicationInfo;

			if(convertView == null)
			{
				convertView = mInflater.inflate(Util.appListItemLayout, parent, false);

				holder = new ViewHolder();
				holder.appIcon = (ImageView) convertView.findViewWithTag("app_icon");
				holder.appPackage = (TextView) convertView.findViewWithTag("app_package");
				holder.appName = (TextView) convertView.findViewWithTag("app_name");

				convertView.setTag(holder);
			}
			else
				holder = (ViewHolder) convertView.getTag();

			holder.appIcon.setImageDrawable(null);
			holder.appName.setText(appInfo.packageName);

			if(true)
			{
				if(holder.task != null)
					holder.task.cancel(true);

				holder.task = new AsyncTask<Void, Void, Object[]>() {
					@Override
					protected Object[] doInBackground(Void... params)
					{
						final Object[] result = new Object[2];
						result[0] = appInfo.loadIcon(mPm);
						result[1] = appInfo.loadLabel(mPm);

						return result;
					}

					@Override
					protected void onPostExecute(Object[] result)
					{
						holder.appIcon.setImageDrawable((Drawable) result[0]);
						holder.appName.setText((CharSequence) result[1]);

						if(!appInfo.packageName.equals(result[1].toString()))
						{
							holder.appPackage.setText(appInfo.packageName);
							holder.appPackage.setVisibility(View.VISIBLE);
						}
						else
							holder.appPackage.setVisibility(View.GONE);
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
		TextView appPackage;
	}

	static class PkgInfoComparator implements Comparator<PackageInfo>
	{
		private static Collator sCollator = Collator.getInstance();
		private final PackageManager mPm;

		public PkgInfoComparator(PackageManager pm) {
			mPm = pm;
		}

		@Override
		public int compare(PackageInfo lhs, PackageInfo rhs)
		{
			return sCollator.compare(lhs.applicationInfo.loadLabel(mPm),
					rhs.applicationInfo.loadLabel(mPm));
		}
	}

	static class AppListLoader extends AsyncTaskLoader<List<PackageInfo>>
	{
		private static String[] sOpPerms = getOpPermissions();

		private final PackageManager mPm;
		private List<PackageInfo> mData;

		private final boolean mRemoveAppsWithUnchangedOps;

		public AppListLoader(Context context, boolean removeAppsWithUnchangedOps)
		{
			super(context);
			mPm = context.getPackageManager();
			mRemoveAppsWithUnchangedOps = removeAppsWithUnchangedOps;
		}

		@Override
		public List<PackageInfo> loadInBackground()
		{
			final List<PackageInfo> data = mPm.getInstalledPackages(PackageManager.GET_PERMISSIONS);

			if(sOpPerms != null)
				removeAppsWithoutOps(data);

			if(mRemoveAppsWithUnchangedOps)
				removeAppsWithUnchangedOps(data);

			Collections.sort(data, new PkgInfoComparator(mPm));
			return data;
		}

		@Override
		public void deliverResult(List<PackageInfo> data)
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

		private void removeAppsWithUnchangedOps(List<PackageInfo> data)
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

		private boolean hasUnchangedOps(PackageInfo info)
		{
			final AppOpsManagerWrapper appOps = AppOpsManagerWrapper.from(getContext());
			final List<PackageOpsWrapper> pkgOpsList = appOps.getOpsForPackage(info.applicationInfo.uid, info.packageName, null);
			for(PackageOpsWrapper pkgOps : pkgOpsList)
			{
				for(OpEntryWrapper op : pkgOps.getOps())
				{
					if(op.getMode() != AppOpsManager.MODE_ALLOWED)
					{
						Log.d(TAG, pkgOps.getPackageName() + ": op " + appOps.opToName(op.getOp()) + ": mode " + op.getMode());
						return false;
					}
				}
			}

			return true;
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
	public Loader<List<PackageInfo>> onCreateLoader(int id, Bundle args) {
		return new AppListLoader(getActivity(), getArguments().getBoolean("show_changed_ops_only"));
	}

	@Override
	public void onLoadFinished(Loader<List<PackageInfo>> loader, List<PackageInfo> data)
	{
		mAdapter.setData(data);

		if(isResumed())
			setListShown(true);
		else
			setListShownNoAnimation(true);
	}

	@Override
	public void onLoaderReset(Loader<List<PackageInfo>> data) {
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
					Util.getSettingsIdentifier("string/app_ops_settings"),
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
