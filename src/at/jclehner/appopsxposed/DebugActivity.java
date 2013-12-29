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
		// All layouting done in code so this ugly interface
		// occupies one file only

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
