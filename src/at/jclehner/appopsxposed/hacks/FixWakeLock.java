package at.jclehner.appopsxposed.hacks;

import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.PowerManager.WakeLock;
import at.jclehner.appopsxposed.Hack;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XposedHelpers;


@TargetApi(19)
public class FixWakeLock extends Hack
{
	private static final boolean DEBUG = false;

	private static Unhook[] sUnhooks;

	@Override
	public void initZygote(StartupParam param) throws Throwable
	{
		sUnhooks = new Unhook[2];
		sUnhooks[0] = XposedHelpers.findAndHookMethod(WakeLock.class, "acquire", mAcquireHook);
		sUnhooks[1] = XposedHelpers.findAndHookMethod(WakeLock.class, "acquire", long.class, mAcquireHook);
	}

	private final XC_MethodHook mAcquireHook = new XC_MethodHook() {
		@Override
		protected void beforeHookedMethod(MethodHookParam param) throws Throwable
		{
			if(DEBUG)
				log("AOX:FixWakeLock: WakeLock.acquire() called");

			try
			{
				final WakeLock lock = (WakeLock) param.thisObject;

				final Context context = (Context) XposedHelpers.getObjectField(
						XposedHelpers.getSurroundingThis(lock), "mContext");

				final AppOpsManager appOps = (AppOpsManager)
						context.getSystemService(Context.APP_OPS_SERVICE);

				final int op = XposedHelpers.getStaticIntField(AppOpsManager.class, "OP_WAKE_LOCK");
				final String packageName = (String) XposedHelpers.getObjectField(lock, "mPackageName");
				final int uid = context.getPackageManager().getApplicationInfo(packageName, 0).uid;

				log("  op=" + op + ", uid=" + uid + ", packageName=" + packageName);

				final Method checkOp = AppOpsManager.class.getMethod("checkOp", int.class, int.class, String.class);
				final int status = (Integer) checkOp.invoke(appOps, op, uid, packageName);

				log("  status=" + status);

				if(status == AppOpsManager.MODE_IGNORED)
					param.setResult(null);
				else if(status != AppOpsManager.MODE_ALLOWED)
					log("checkOpNoThrow returned unknown status " + status);
			}
			catch(Throwable t)
			{
				log("Exception caught - disabling hack");
				log(t);

				if(sUnhooks != null)
				{
					for(Unhook u: sUnhooks)
						u.unhook();
				}
			}
		}
	};
}

