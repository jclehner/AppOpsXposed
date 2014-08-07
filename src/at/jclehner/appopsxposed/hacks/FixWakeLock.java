package at.jclehner.appopsxposed.hacks;

import java.lang.reflect.Method;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.PowerManager.WakeLock;
import at.jclehner.appopsxposed.Hack;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.Unhook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;


@TargetApi(19)
public class FixWakeLock extends Hack
{
	public static final FixWakeLock INSTANCE = new FixWakeLock();

	private static final String UNKNOWN_TAG = "(unknown tag)";
	private static final boolean ENABLE_PER_TAG_FILTERING = false;
	private static final boolean DEBUG = true;


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

	@Override
	protected String onGetKeySuffix() {
		return "wake_lock";
	}

	private boolean canAcquireTag(String tag)
	{
		// TODO actually implement this
		return false;
	}

	private final XC_MethodHook mAcquireHook = new XC_MethodHook() {
		@Override
		protected void beforeHookedMethod(MethodHookParam param) throws Throwable
		{
			try
			{
				final WakeLock lock = (WakeLock) param.thisObject;

				// The surrounding this is android.os.PowerManager, which has its
				// context stored in mContext
				final Context context = (Context) XposedHelpers.getObjectField(
						XposedHelpers.getSurroundingThis(lock), "mContext");

				final AppOpsManager appOps = (AppOpsManager)
						context.getSystemService(Context.APP_OPS_SERVICE);

				final ApplicationInfo info = context.getApplicationInfo();
				final int status = (Integer) mCheckOpNoThrow.invoke(appOps, OP_WAKE_LOCK, info.uid, info.packageName);

				if(status == AppOpsManager.MODE_IGNORED)
				{
					final String tag = getWakeLockTag(lock);
					if(tag != null && canAcquireTag(tag))
					{
						if(DEBUG)
						{
							log("Allowing acquisition of WakeLock " + tag +
									" for app " + info.packageName);
						}

						return;
					}

					if(DEBUG)
					{
						log("Prevented acquisition of WakeLock " + tag +
								" for app " + info.packageName);
					}

					// Otherwise WakeLock.release() might throw an exception
					lock.setReferenceCounted(false);
					param.setResult(null);
				}
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

	private static String getWakeLockTag(WakeLock lock)
	{
		try
		{
			return (String) XposedHelpers.getObjectField(lock, "mTag");
		}
		catch(Throwable t)
		{
			return null;
		}
	}
}

