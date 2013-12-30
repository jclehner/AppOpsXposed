package at.jclehner.appopsxposed;

import android.app.Activity;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class LauncherActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final Intent intent = new Intent("android.settings.SETTINGS");
		intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, AppOpsXposed.APP_OPS_FRAGMENT);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

		/*TaskStackBuilder tsb = TaskStackBuilder.create(this);
		tsb.addNextIntent(new Intent("android.settings.SETTINGS"));
		tsb.addNextIntent(intent);

		tsb.startActivities();*/

		startActivity(intent);
		finish();
	}
}
