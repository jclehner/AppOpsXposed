package at.jclehner.appopsxposed;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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

public class AppListFragment extends ListFragment implements LoaderCallbacks<List<ApplicationInfo>>
{
	private static final String TAG = "AppListFragment";

	private AppListAdapter mAdapter;
	private LayoutInflater mInflater;

	class AppListAdapter extends BaseAdapter
	{
		private final PackageManager mPm;
		private List<ApplicationInfo> mList;

		public AppListAdapter(Context context) {
			mPm = context.getPackageManager();
		}

		public void setData(List<ApplicationInfo> list)
		{
			mList = list;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mList != null ? mList.size() : 0;
		}

		@Override
		public ApplicationInfo getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			final View v;
			if(convertView == null)
				v = mInflater.inflate(R.layout.app_list_item, parent, false);
			else
				v = convertView;

			final ApplicationInfo info = getItem(position);
			v.setTag(info.packageName);

			final CharSequence label = info.loadLabel(mPm);
			if(label.toString().equals(info.packageName))
				v.findViewById(R.id.app_package).setVisibility(View.GONE);
			else
				v.findViewById(R.id.app_package).setVisibility(View.VISIBLE);

			((ImageView) v.findViewById(R.id.app_icon)).setImageDrawable(info.loadIcon(mPm));
			((TextView) v.findViewById(R.id.app_name)).setText(label);
			((TextView) v.findViewById(R.id.app_package)).setText(info.packageName);

			return v;
		}
	}

	static class AppInfoComparator implements Comparator<ApplicationInfo>
	{
		private static Collator sCollator = Collator.getInstance();
		private final PackageManager mPm;

		public AppInfoComparator(PackageManager pm) {
			mPm = pm;
		}

		@Override
		public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
			return sCollator.compare(lhs.loadLabel(mPm), rhs.loadLabel(mPm));
		}
	}

	static class AppListLoader extends AsyncTaskLoader<List<ApplicationInfo>>
	{
		private List<ApplicationInfo> mData;
		private PackageManager mPm;

		public AppListLoader(Context context)
		{
			super(context);
			mPm = context.getPackageManager();
		}

		@Override
		public List<ApplicationInfo> loadInBackground()
		{
			List<ApplicationInfo> data = mPm.getInstalledApplications(0);
			Collections.sort(data, new AppInfoComparator(mPm));
			return data;
		}

		@Override
		public void deliverResult(List<ApplicationInfo> data)
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
	}

	@Override
	public Loader<List<ApplicationInfo>> onCreateLoader(int id, Bundle args) {
		return new AppListLoader(getActivity());
	}

	@Override
	public void onLoadFinished(Loader<List<ApplicationInfo>> loader, List<ApplicationInfo> data)
	{
		mAdapter.setData(data);

		if(isResumed())
			setListShown(true);
		else
			setListShownNoAnimation(true);
	}

	@Override
	public void onLoaderReset(Loader<List<ApplicationInfo>> data) {
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
		args.putString("package", (String) v.getTag());

		final Intent intent = new Intent("android.settings.SETTINGS");
		intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_DETAILS_FRAGMENT);
		intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

		startActivity(intent);
	}
}
