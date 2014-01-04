package at.jclehner.appopsxposed;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import at.jclehner.appopsxposed.variants.Sony;

public class LauncherActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final Intent intent = new Intent();

		if(Build.MANUFACTURER.toLowerCase().contains("sony"))
		{
			final TextView tv = new TextView(this);
			tv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			tv.setGravity(Gravity.CENTER);
			tv.setText("You are using a Sony device. Please try to launch AppOps from the Settings app!");
			tv.setTextAppearance(this, android.R.attr.textAppearanceMedium);
			setContentView(tv);
			return;
		}

		if(false)
		{
			Log.i("AppOpsXposed", "Using Sony-specific Intent");
			intent.setClassName(AppOpsXposed.SETTINGS_PACKAGE, Sony.SLAVE_ACTIVITY_NAME);
			intent.putExtra(Sony.EXTRA_LAUNCH_APP_OPS, true);
		}
		else
		{
			Log.i("AppOpsXposed", "Using standard Intent");
			intent.setAction("android.settings.SETTINGS");
			intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);
		}

		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

		startActivity(intent);
		finish();
	}
}
