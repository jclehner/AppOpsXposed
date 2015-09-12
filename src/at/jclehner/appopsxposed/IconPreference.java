package at.jclehner.appopsxposed;

import android.content.Context;
import android.graphics.Color;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

public class IconPreference extends Preference implements AdapterView.OnItemClickListener
{
	private LayoutInflater mInflater;
	private Spinner mSpinner;
	private int[] mIcons;
	private int mSelection;

	public IconPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setWidgetLayoutResource(R.layout.spinner);
		mInflater = LayoutInflater.from(context);
	}

	public void setIcons(int[] icons)
	{
		mIcons = icons;
		updateSpinner();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if(callChangeListener(position))
			persistInt(position);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValueRaw)
	{
		final int defaultValue = defaultValueRaw != null ? Integer.parseInt(defaultValueRaw.toString()) : 0;
		mSelection = restorePersistedValue ? getPersistedInt(defaultValue) : defaultValue;

		updateSpinner();
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);
		mSpinner = (Spinner) view.findViewById(R.id.spinnerWidget);
		mSpinner.setBackgroundColor(Color.LTGRAY);
		updateSpinner();
	}

	private void updateSpinner()
	{
		if(mSpinner == null)
			return;

		mSpinner.setAdapter(null);
		mSpinner.setAdapter(mAdapter);
		mSpinner.setSelection(mSelection);
	}

	private final SpinnerAdapter mAdapter = new BaseAdapter() {
		@Override
		public int getCount()
		{
			return mIcons != null ? mIcons.length : 0;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent)
		{
			if(view == null)
				view = (ImageView) mInflater.inflate(R.layout.icon, null);

			((ImageView) view).setImageResource(mIcons[position]);
			view.setBackgroundColor(position % 2 == 0 ? Color.DKGRAY : Color.TRANSPARENT);

			return view;
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public Object getItem(int position)
		{
			return mIcons[position];
		}
	};
}
