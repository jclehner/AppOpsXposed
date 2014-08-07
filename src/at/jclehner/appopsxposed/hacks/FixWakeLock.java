package at.jclehner.appopsxposed.hacks;

import java.lang.reflect.Method;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.os.PowerManager.WakeLock;
import at.jclehner.appopsxposed.Hack;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;


@TargetApi(19)
public class FixWakeLock extends Hack
{
	private static final boolean DEBUG = false;


	private static final int OP_WAKE_LOCK =
			XposedHelpers.getStaticIntField(AppOpsManager.class, "OP_WAKE_LOCK");

	private Set<Unhook> mUnhooks;
	private Method mCheckOpNoThrow;

	@Override
	public void initZygote(StartupParam param) throws Throwable
	{
		mCheckOpNoThrow = XposedHelpers.findMethodExact(AppOpsManager.class, "checkOpNoThrow",
				int.class, int.class, String.class);
		mUnhooks = XposedBridge.hookAllMethods(WakeLock.class, "acquire", mAcquireHook);

		log("Hooked WakeLock.acquire(): " + mUnhooks.size() + " functions");
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

				final String packageName = (String) XposedHelpers.getObjectField(lock, "mPackageName");
				final int uid = context.getPackageManager().getApplicationInfo(packageName, 0).uid;

				if(DEBUG)
					log("  op=" + OP_WAKE_LOCK + ", uid=" + uid + ", packageName=" + packageName);

				final int status = (Integer) mCheckOpNoThrow.invoke(appOps, OP_WAKE_LOCK, uid, packageName);

				if(DEBUG)
					log("  status=" + status);

				if(status == AppOpsManager.MODE_IGNORED)
					param.setResult(null);
			}
			catch(Throwable t)
			{
				log("Exception caught - disabling hack");
				log(t);

				if(mUnhooks != null)
				{
					for(Unhook u: mUnhooks)
						u.unhook();
				}
			}
		}
	};
}

