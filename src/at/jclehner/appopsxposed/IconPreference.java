package at.jclehner.appopsxposed;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

public class IconPreference extends Preference implements AdapterView.OnItemSelectedListener
{
	private LayoutInflater mInflater;
	private Spinner mSpinner;
	private int[] mIcons;

	private int mValue;
	private boolean mWasValueSet = false;

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
	protected void onClick()
	{
		super.onClick();

		if(mSpinner != null)
			mSpinner.performClick();
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
	{
		if(callChangeListener(position))
			setValue(position);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent)
	{

	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index)
	{
		return a.getInt(index, 0);
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
	{
		setValue(restoreValue ? getPersistedInt(mValue) : (int) defaultValue);
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);
		mSpinner = (Spinner) view.findViewById(R.id.spinnerWidget);

		final ViewParent parent = mSpinner.getParent();
		if(parent instanceof View)
		{
			final Rect rect  = new Rect(0, 0, ((View) parent).getWidth(),
					((View) parent).getHeight());
			((View) parent).setTouchDelegate(new TouchDelegate(rect, (View) parent));

		}

		updateSpinner();
		mSpinner.setOnItemSelectedListener(this);
	}

	private void setValue(int value)
	{
		final boolean changed = mValue != value;
		if(changed || !mWasValueSet)
		{
			mValue = value;
			mWasValueSet = true;
			persistInt(value);
			if(changed)
				notifyChanged();
		}
	}

	private void updateSpinner()
	{
		if(mSpinner == null)
			return;

		mSpinner.setAdapter(null);
		mSpinner.setAdapter(mAdapter);
		mSpinner.setSelection(mValue);
	}

	private final SpinnerAdapter mAdapter = new BaseAdapter() {
		@Override
		public int getCount()
		{
			return mIcons != null ? mIcons.length : 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			return getView(position, null, R.layout.icon_spinner);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent)
		{
			final View v = getView(position, null, R.layout.icon_dropdown);
			v.setBackgroundResource(position != mValue ? R.drawable.checkerboard
					: R.drawable.checkerboard_framed);
			return v;
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

		private View getView(int position, View view, int layoutResId)
		{
			if(view == null)
				view = mInflater.inflate(layoutResId, null);

			((ImageView) view).setImageResource(mIcons[position]);

			return view;
		}
	};
}
