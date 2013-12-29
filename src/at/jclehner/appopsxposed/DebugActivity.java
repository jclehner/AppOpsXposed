package at.jclehner.appopsxposed;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DebugActivity extends Activity
{
	private TextView mText;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		setTheme(android.R.style.Theme_Holo_Light_DarkActionBar);
		setTitle("AppOpsXposed");

		super.onCreate(savedInstanceState);

		final LinearLayout ll = new LinearLayout(this);
		ll.setPadding(20, 20, 20, 20);
		ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		ll.setGravity(Gravity.CENTER);
		ll.setOrientation(LinearLayout.VERTICAL);

		Button b = new Button(this);
		b.setText("Fragment");
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent("android.settings.SETTINGS");
				intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);
				startActivity(intent);
			}
		});
		ll.addView(b);

		b = new Button(this);
		b.setText("Activity");
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent();
				intent.setClassName("com.android.settings", "com.android.settings.Settings$AppOpsSummaryActivity");
				startActivity(intent);
			}
		});
		ll.addView(b);

		b = new Button(this);
		b.setText("Action");
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				startActivity(new Intent("android.settings.APP_OPS_SETTINGS"));
			}
		});
		ll.addView(b);

		mText = new TextView(this);
		mText.setMinLines(10);
		mText.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		mText.setTextAppearance(this, android.R.style.TextAppearance_Small);
		mText.setTypeface(Typeface.MONOSPACE);

		ll.addView(mText);

		setContentView(ll);
	}

	@Override
	public void startActivity(Intent intent)
	{
		try
		{
			super.startActivity(intent);
			mText.setText("");
		}
		catch(RuntimeException e)
		{
			mText.setText(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
