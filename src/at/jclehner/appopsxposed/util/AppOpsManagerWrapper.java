package at.jclehner.appopsxposed.util;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.util.Log;

@TargetApi(19)
public class AppOpsManagerWrapper extends ObjectWrapper
{
	// These are all ops included in AOSP KitKat
	public static final int OP_NONE = getOpInt("OP_NONE");
	public static final int OP_COARSE_LOCATION = getOpInt("OP_COARSE_LOCATION");
	public static final int OP_FINE_LOCATION = getOpInt("OP_FINE_LOCATION");
	public static final int OP_GPS = getOpInt("OP_GPS");
	public static final int OP_VIBRATE = getOpInt("OP_VIBRATE");
	public static final int OP_READ_CONTACTS = getOpInt("OP_READ_CONTACTS");
	public static final int OP_WRITE_CONTACTS = getOpInt("OP_WRITE_CONTACTS");
	public static final int OP_READ_CALL_LOG = getOpInt("OP_READ_CALL_LOG");
	public static final int OP_WRITE_CALL_LOG = getOpInt("OP_WRITE_CALL_LOG");
	public static final int OP_READ_CALENDAR = getOpInt("OP_READ_CALENDAR");
	public static final int OP_WRITE_CALENDAR = getOpInt("OP_WRITE_CALENDAR");
	public static final int OP_WIFI_SCAN = getOpInt("OP_WIFI_SCAN");
	public static final int OP_POST_NOTIFICATION = getOpInt("OP_POST_NOTIFICATION");
	public static final int OP_NEIGHBORING_CELLS = getOpInt("OP_NEIGHBORING_CELLS");
	public static final int OP_CALL_PHONE = getOpInt("OP_CALL_PHONE");
	public static final int OP_READ_SMS = getOpInt("OP_READ_SMS");
	public static final int OP_WRITE_SMS = getOpInt("OP_WRITE_SMS");
	public static final int OP_RECEIVE_SMS = getOpInt("OP_RECEIVE_SMS");
	public static final int OP_RECEIVE_EMERGECY_SMS = getOpInt("OP_RECEIVE_EMERGECY_SMS");
	public static final int OP_RECEIVE_MMS = getOpInt("OP_RECEIVE_MMS");
	public static final int OP_RECEIVE_WAP_PUSH = getOpInt("OP_RECEIVE_WAP_PUSH");
	public static final int OP_SEND_SMS = getOpInt("OP_SEND_SMS");
	public static final int OP_READ_ICC_SMS = getOpInt("OP_READ_ICC_SMS");
	public static final int OP_WRITE_ICC_SMS = getOpInt("OP_WRITE_ICC_SMS");
	public static final int OP_WRITE_SETTINGS = getOpInt("OP_WRITE_SETTINGS");
	public static final int OP_SYSTEM_ALERT_WINDOW = getOpInt("OP_SYSTEM_ALERT_WINDOW");
	public static final int OP_ACCESS_NOTIFICATIONS = getOpInt("OP_ACCESS_NOTIFICATIONS");
	public static final int OP_CAMERA = getOpInt("OP_CAMERA");
	public static final int OP_RECORD_AUDIO = getOpInt("OP_RECORD_AUDIO");
	public static final int OP_PLAY_AUDIO = getOpInt("OP_PLAY_AUDIO");
	public static final int OP_READ_CLIPBOARD = getOpInt("OP_READ_CLIPBOARD");
	public static final int OP_WRITE_CLIPBOARD = getOpInt("OP_WRITE_CLIPBOARD");
	public static final int OP_TAKE_MEDIA_BUTTONS = getOpInt("OP_TAKE_MEDIA_BUTTONS");
	public static final int OP_TAKE_AUDIO_FOCUS = getOpInt("OP_TAKE_AUDIO_FOCUS");
	public static final int OP_AUDIO_MASTER_VOLUME = getOpInt("OP_AUDIO_MASTER_VOLUME");
	public static final int OP_AUDIO_VOICE_VOLUME = getOpInt("OP_AUDIO_VOICE_VOLUME");
	public static final int OP_AUDIO_RING_VOLUME = getOpInt("OP_AUDIO_RING_VOLUME");
	public static final int OP_AUDIO_MEDIA_VOLUME = getOpInt("OP_AUDIO_MEDIA_VOLUME");
	public static final int OP_AUDIO_ALARM_VOLUME = getOpInt("OP_AUDIO_ALARM_VOLUME");
	public static final int OP_AUDIO_NOTIFICATION_VOLUME = getOpInt("OP_AUDIO_NOTIFICATION_VOLUME");
	public static final int OP_AUDIO_BLUETOOTH_VOLUME = getOpInt("OP_AUDIO_BLUETOOTH_VOLUME");
	public static final int OP_WAKE_LOCK = getOpInt("OP_WAKE_LOCK");
	public static final int OP_MONITOR_LOCATION = getOpInt("OP_MONITOR_LOCATION");
	public static final int OP_MONITOR_HIGH_POWER_LOCATION = getOpInt("OP_MONITOR_HIGH_POWER_LOCATION");

	// CyanogenMod (also seen on some Xperia ROMs!)
	public static final int OP_WIFI_CHANGE = getOpInt("OP_WIFI_CHANGE");
	public static final int OP_BLUETOOTH_CHANGE = getOpInt("OP_BLUETOOTH_CHANGE");
	public static final int OP_DATA_CONNECT_CHANGE = getOpInt("OP_DATA_CONNECT_CHANGE");
	public static final int OP_SEND_MMS = getOpInt("OP_SEND_MMS");
	public static final int OP_READ_MMS = getOpInt("OP_READ_MMS");
	public static final int OP_WRITE_MMS = getOpInt("OP_WRITE_MMS");
	public static final int OP_ALARM_WAKEUP = getOpInt("OP_ALARM_WAKEUP");
	public static final int OP_NFC_CHANGE = getOpInt("OP_NFC_CHANGE");

	public static final int OP_BOOT_COMPLETED = getOpWithPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED);

	public static final int _NUM_OP = getOpInt("_NUM_OP");

	public static final int MODE_ALLOWED = AppOpsManager.MODE_ALLOWED;
	public static final int MODE_IGNORED = AppOpsManager.MODE_IGNORED;
	public static final int MODE_ERRORED = AppOpsManager.MODE_ERRORED;

	public static AppOpsManagerWrapper from(Context context) {
		return new AppOpsManagerWrapper(context);
	}

	@TargetApi(19)
	private AppOpsManagerWrapper(Context context) {
		super(context.getSystemService(Context.APP_OPS_SERVICE));
	}

	public List<PackageOpsWrapper> getOpsForPackage(int uid, String packageName, int[] ops)
	{
		return PackageOpsWrapper.convertList((List<?>) call("getOpsForPackage",
				new Class<?>[] { int.class, String.class, int[].class },
				uid, packageName, ops));
	}

	public List<PackageOpsWrapper> getPackagesForOps(int[] ops)
	{
		return PackageOpsWrapper.convertList((List<?>) call(
				"getPackagesForOps", new Class<?>[] { int[].class }, ops));
	}

	public int checkOp(String op, int uid, String packageName)
	{
		return call("checkOp", new Class<?>[] { String.class, int.class, String.class },
				op, uid, packageName);
	}

	public int checkOp(int op, int uid, String packageName)
	{
		return call("checkOp", new Class<?>[] { int.class, int.class, String.class },
				op, uid, packageName);
	}

	public void setMode(int code, int uid, String packageName, int mode)
	{
		call("setMode", new Class<?>[] { int.class, int.class, String.class, int.class },
				code, uid, packageName, mode);
	}

	public static String opToName(int op) {
		return callStatic(AppOpsManager.class, "opToName", new Class<?>[] { int.class }, op);
	}

	public static String opToPermission(int op) {
		return callStatic(AppOpsManager.class, "opToPermission", new Class<?>[] { int.class }, op);
	}

	public static int opToSwitch(int op) {
		return callStatic(AppOpsManager.class, "opToSwitch", new Class<?>[] { int.class }, op);
	}

	private static int getOpInt(String opName) {
		return getStatic(AppOpsManager.class, opName, -1);
	}

	private static int getOpWithPermission(String permission)
	{
		final int num = getOpInt("_NUM_OP");
		for(int op = 0; op < num; ++op)
		{
			if(permission.equals(opToPermission(op)))
			{
				Log.d("AppOpsManagerWrapper", "Found op #" + op + " with permission " + permission);
				return op;
			}
		}

		Log.d("AppOpsManagerWrapper", "No op found for permission " + permission);

		return -1;
	}

	public static class PackageOpsWrapper extends ObjectWrapper
	{
		private String mPackageName;
		private int mUid;
		private List<OpEntryWrapper> mEntries;

		private PackageOpsWrapper(Object obj) {
			super(obj);
		}

		public PackageOpsWrapper(String packageName, int uid, List<OpEntryWrapper> entries)
		{
			super(null);

			mPackageName = packageName;
			mUid = uid;
			mEntries = entries;
		}

		public static List<PackageOpsWrapper> convertList(List<?> list)
		{
			final List<PackageOpsWrapper> converted = new ArrayList<PackageOpsWrapper>();
			if(list != null)
			{
				for(Object o : list)
					converted.add(new PackageOpsWrapper(o));
			}

			return converted;
		}

		public String getPackageName()
		{
			if(mObj != null)
				return call("getPackageName");

			return mPackageName;
		}

		public int getUid()
		{
			if(mObj != null)
				return (Integer) call("getUid");

			return mUid;
		}

		public List<OpEntryWrapper> getOps()
		{
			if(mObj != null)
				return OpEntryWrapper.convertList((List<?>) call("getOps"));

			return mEntries;
		}
	}

	public static class OpEntryWrapper extends ObjectWrapper
	{
		private OpEntryWrapper(Object obj) {
			super(obj);
		}

		private int mOp;
		private int mMode;
		private long mTime;
		private long mRejectTime;
		private int mDuration;

		public OpEntryWrapper(int op, int mode, long time, long rejectTime, int duration)
		{
			super(null);

			mOp = op;
			mMode = mode;
			mTime = time;
			mRejectTime = rejectTime;
			mDuration = duration;
		}

		public static List<OpEntryWrapper> convertList(List<?> list)
		{
			final List<OpEntryWrapper> converted = new ArrayList<OpEntryWrapper>();
			if(list != null)
			{
				for(Object o : list)
					converted.add(new OpEntryWrapper(o));
			}

			return converted;
		}

		public int getOp()
		{
			if(mObj != null)
				return (Integer) call("getOp");

			return mOp;
		}

		public int getMode()
		{
			if(mObj != null)
				return (Integer) call("getMode");

			return mMode;
		}

		public long getTime()
		{
			if(mObj != null)
				return (Long) call("getTime");

			return mTime;
		}

		public long getRejectTime()
		{
			if(mObj != null)
				return (Long) call("getRejectTime");

			return mRejectTime;
		}

		public boolean isRunning()
		{
			if(mObj != null)
				return (Boolean) call("isRunning");

			return mDuration == -1;
		}

		public int getDuration()
		{
			if(mObj != null)
				return (Integer) call("getDuration");

			return mDuration == -1 ? (int)(System.currentTimeMillis() - mTime) : mDuration;
		}
	}
}
