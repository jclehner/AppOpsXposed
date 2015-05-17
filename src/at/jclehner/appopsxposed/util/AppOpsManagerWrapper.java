/*
 * AppOpsXposed - AppOps for Android 4.3+
 * Copyright (C) 2013-2015 Joseph C. Lehner
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

package at.jclehner.appopsxposed.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.util.Log;

@TargetApi(19)
public class AppOpsManagerWrapper extends ObjectWrapper
{
	// These are all ops included in AOSP Lollipop
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
	public static final int OP_GET_USAGE_STATS = getOpInt("OP_GET_USAGE_STATS");
	public static final int OP_MUTE_MICROPHONE = getOpInt("OP_MUTE_MICROPHONE");
	public static final int OP_TOAST_WINDOW = getOpInt("OP_TOAST_WINDOW");
	public static final int OP_PROJECT_MEDIA = getOpInt("OP_PROJECT_MEDIA");
	public static final int OP_ACTIVATE_VPN = getOpInt("OP_ACTIVATE_VPN");

	// CyanogenMod (also seen on some Xperia ROMs!)
	public static final int OP_WIFI_CHANGE = getOpInt("OP_WIFI_CHANGE");
	public static final int OP_BLUETOOTH_CHANGE = getOpInt("OP_BLUETOOTH_CHANGE");
	public static final int OP_DATA_CONNECT_CHANGE = getOpInt("OP_DATA_CONNECT_CHANGE");
	public static final int OP_SEND_MMS = getOpInt("OP_SEND_MMS");
	public static final int OP_READ_MMS = getOpInt("OP_READ_MMS");
	public static final int OP_WRITE_MMS = getOpInt("OP_WRITE_MMS");
	public static final int OP_ALARM_WAKEUP = getOpInt("OP_ALARM_WAKEUP");
	public static final int OP_NFC_CHANGE = getOpInt("OP_NFC_CHANGE");

	// MiUi
	public static final int OP_AUDIO_FM_VOLUME = getOpInt("OP_AUDIO_FM_VOLUME");
	public static final int OP_AUDIO_MATV_VOLUME = getOpInt("OP_AUDIO_MATV_VOLUME");
	public static final int OP_AUTO_START = getOpInt("OP_AUTO_START");
	public static final int OP_DELETE_SMS = getOpInt("OP_DELETE_SMS");
	public static final int OP_DELETE_MMS = getOpInt("OP_DELETE_MMS");
	public static final int OP_DELETE_CONTACTS = getOpInt("OP_DELETE_CONTACTS");
	public static final int OP_DELETE_CALL_LOG = getOpInt("OP_DELETE_CALL_LOG");
	public static final int OP_EXACT_ALARM = getOpInt("OP_EXACT_ALARM");
	public static final int OP_ACCESS_XIAOMI_ACCOUNT = getOpInt("OP_ACCESS_XIAOMI_ACCOUNT");

	public static final int OP_BOOT_COMPLETED = getBootCompletedOp();

	public static final int _NUM_OP = getNumOp();

	public static final int MODE_ALLOWED = getOpInt("MODE_ALLOWED");
	public static final int MODE_IGNORED = getOpInt("MODE_IGNORED");
	public static final int MODE_ERRORED = getOpInt("MODE_ERRORED");
	public static final int MODE_DEFAULT = getOpInt("MODE_DEFAULT");
	public static final int MODE_HINT = getOpInt("MODE_HINT");

	// CyanogenMod, Sony ROMs, etc.
	public static final int MODE_ASK = getOpInt("MODE_ASK");

	private final Context mContext;

	public static AppOpsManagerWrapper from(Context context) {
		return new AppOpsManagerWrapper(context);
	}

	@TargetApi(19)
	private AppOpsManagerWrapper(Context context)
	{
		super(context.getSystemService(Context.APP_OPS_SERVICE));
		mContext = context;
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

	public int checkOpNoThrow(int op, int uid, String packageName)
	{
		return call("checkOpNoThrow", new Class<?>[] { int.class, int.class, String.class },
				op, uid, packageName);
	}

	public void setMode(int code, int uid, String packageName, int mode)
	{
		call("setMode", new Class<?>[] { int.class, int.class, String.class, int.class },
				code, uid, packageName, mode);
	}

	public Object getService()
	{
		try
		{
			final Class<?> sm = Class.forName("android.os.ServiceManager");
			final IBinder service = callStatic(sm, "getService", Context.APP_OPS_SERVICE);
			final Class<?> aos = Class.forName("com.android.internal.app.IAppOpsService$Stub");
			return callStatic(aos, "asInterface", new Class<?>[] { IBinder.class }, service);
		}
		catch(Exception e)
		{
			throw new ReflectiveException(e);
		}
	}

	public boolean resetAllModes()
	{
		try
		{
			new ObjectWrapper(getService()).call("resetAllModes");
			return true;
		}
		catch(ReflectiveException e)
		{
			Util.debug(e);
			Log.w("AOX", e);
			return false;
		}
	}

	public void resetAllModes(int uid, String packageName)
	{
		for(PackageOpsWrapper pow : getOpsForPackage(uid, packageName, null))
		{
			for(OpEntryWrapper oew : pow.getOps())
			{
				final int op = oew.getOp();
				if(!AppOpsManagerWrapper.opAllowsReset(op))
					continue;

				int defMode =  AppOpsManagerWrapper.opToDefaultMode(op);

				// When calling setMode with the ops default mode, the op
				// is pruned by AppOpsService. However, setMode call is
				// ignored if that mode is already ser for this op. We
				// simply set it to another mode than the current one,
				// and then to its default.
				// Note that we can only use MODE_{IGNORED,ALLOWED,ERRORED},
				// as these are universal across all ROMs.

				if(defMode != AppOpsManagerWrapper.MODE_IGNORED)
					setMode(op, uid, packageName, AppOpsManagerWrapper.MODE_IGNORED);
				else
					setMode(op, uid, packageName, AppOpsManagerWrapper.MODE_ALLOWED);

				setMode(op, uid, packageName, defMode);
			}
		}
	}

	public List<PackageOpsWrapper> getAllOpsForPackage(int uid, String packageName, int[] ops)
	{
		final PackageManager pm = mContext.getPackageManager();
		final List<PackageOpsWrapper> pows = getOpsForPackage(uid, packageName, ops);

		for(PackageOpsWrapper pow : pows)
		{
			final PackageInfo pi;

			try
			{
				pi = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
			}
			catch(NameNotFoundException e)
			{
				Util.debug(e);
				continue;
			}

			if(pi.applicationInfo.uid != uid || pi.sharedUserId == null)
				continue;

			final Set<OpEntryWrapper> pkgOps = new HashSet<>(pow.getOps());
			final PackageOpsWrapper spow = getPackageOpsFromAllOps(uid, packageName, ops);

			for(OpEntryWrapper soew : spow.getOps())
			{
				if(!pkgOps.contains(soew))
				{
					soew.mUid = spow.mUid;
					soew.mPackageName = spow.mPackageName;
					pkgOps.add(soew);
					pow.getOps().add(soew);
				}
			}
		}

		return pows;
	}

	private List <PackageInfo> getPackagesWithSharedUid(PackageManager pm, String sharedUid)
	{
		final List<PackageInfo> pkgs = new ArrayList<>();

		for(PackageInfo pi : pm.getInstalledPackages(PackageManager.GET_PERMISSIONS))
		{
			if(sharedUid.equals(pi.sharedUserId))
				pkgs.add(pi);
		}

		return pkgs;
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

	private static boolean sUseOpToDefaultMode = true;

	public static int opToDefaultMode(int op)
	{
		if(sUseOpToDefaultMode)
		{
			try
			{
				return callStatic(AppOpsManager.class, "opToDefaultMode", new Class<?>[] { int.class }, op);
			}
			catch(ReflectiveException e)
			{
				Util.debug(e);
				sUseOpToDefaultMode = false;
			}
		}

		return MODE_ALLOWED;
	}

	private static boolean sUseOpAllowsReset = true;

	public static boolean opAllowsReset(int op)
	{
		if(sUseOpAllowsReset)
		{
			try
			{
				return callStatic(AppOpsManager.class, "opAllowsReset", new Class<?>[] { int.class }, op);
			}
			catch(ReflectiveException e)
			{
				Util.debug(e);
				sUseOpAllowsReset = false;
			}
		}

		// This op is used to control which app is the current
		// SMS app, so we don't reset it. At the moment, this
		// is the only op in AOSP which doesn't allow a reset.
		return op != OP_WRITE_SMS;
	}

	public static int opFromName(String opName)
	{
		if(!opName.startsWith("OP_"))
			opName = "OP_" + opName;

		return getStatic(AppOpsManagerWrapper.class, opName, -1);
	}

	public static String modeToName(int mode)
	{
		if(mode >= 0)
		{
			if(mode == MODE_ALLOWED)
				return "ALLOWED";
			if(mode == MODE_ERRORED)
				return "ERRORED";
			if(mode == MODE_IGNORED)
				return "IGNORED";
			if(mode == MODE_DEFAULT)
				return "DEFAULT";
			if(mode == MODE_ASK)
				return "ASK";
			if(mode == MODE_HINT)
				return "HINT";
		}

		return "mode #" + mode;
	}

	public static boolean isBootCompletedHackEnabled()
	{
		return AppOpsManagerWrapper.OP_BOOT_COMPLETED ==
				AppOpsManagerWrapper.OP_POST_NOTIFICATION;
	}

	public static boolean hasTrueBootCompletedOp() {
		return getOpInt("OP_BOOT_COMPLETED") != -1;
	}

	public static int[] getAllValidOps()
	{
		// TODO if HTC ops are ever implemented, this function
		// must be adapted
		final int[] ops = new int[_NUM_OP];
		for(int i = 0; i != ops.length; ++i)
			ops[i] = i;

		return ops;
	}

	public static int[] getAllValidModes()
	{
		final int[] allModes = {
				MODE_ALLOWED,
				MODE_IGNORED,
				MODE_ERRORED,
				MODE_DEFAULT,
				MODE_ASK,
				MODE_HINT
		};

		final int[] modes = new int[allModes.length];
		int i = 0;

		for(int mode : allModes)
		{
			if(mode != -1)
				modes[i++] = mode;
		}

		return Arrays.copyOf(modes, i);
	}

	public static int permissionToOp(String permission)
	{
		for(int op : getAllValidOps())
		{
			if(permission.equals(opToPermission(op)))
				return op;
		}

		return OP_NONE;
	}

	private static int getOpInt(String opName)
	{
		try
		{
			return getStatic(AppOpsManager.class, opName);
		}
		catch(ReflectiveException e)
		{
			return -1;
		}
	}

	private static int getOpWithPermission(String permission)
	{
		final int num = getOpInt("_NUM_OP");
		for(int op = 0; op < num; ++op)
		{
			if(permission.equals(opToPermission(op)))
			{
				Util.log("Found op #" + op + " with permission " + permission);
				return op;
			}
		}

		Util.debug("No op found for permission " + permission);

		return -1;
	}

	private static int getBootCompletedOp()
	{
		final int op = getOpInt("OP_BOOT_COMPLETED");
		return op == -1 ? getOpWithPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED)
				: op;
	}

	private static int getNumOp()
	{
		if(Util.containsManufacturer("HTC"))
		{
			// Some HTC ROMs have added ops, prefixed with HTC_OP_. These ops are
			// offset by 1000, so our usual assumption of op doubling as an array
			// index is not valid anymore; HTC has added an opToIndex method to
			// AppOpsManager which takes care of this problem.
			// On these ROMs, _NUM_OP == _NUM_GOOGLE_OP + _NUM_HTC_OP!
			//
			// For now, we only use _NUM_GOOGLE_OP.

			int numGoogle = getOpInt("_NUM_GOOGLE_OP");
			if(numGoogle != -1)
				return numGoogle;
		}

		return getOpInt("_NUM_OP");
	}

	private PackageOpsWrapper getPackageOpsFromAllOps(int uid, String packageName, int[] ops)
	{
		for(PackageOpsWrapper pow : getPackagesForOps(ops))
		{
			if(pow.mUid == uid && pow.mPackageName == packageName)
				return pow;
		}

		return null;
	}

	public static class PackageOpsWrapper extends ObjectWrapper
	{
		private final String mPackageName;
		private final int mUid;
		private final List<OpEntryWrapper> mEntries;
		private boolean mInitialized = false;

		private PackageOpsWrapper(Object obj)
		{
			super(obj);
			mPackageName = getPackageName();
			mUid = getUid();
			mEntries = getOps();
			mInitialized = true;
		}

		public PackageOpsWrapper(String packageName, int uid, List<OpEntryWrapper> entries)
		{
			super(null);
			mPackageName = packageName;
			mUid = uid;
			mEntries = entries;
			mInitialized = true;
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
			if(!mInitialized)
				return call("getPackageName");

			return mPackageName;
		}

		public int getUid()
		{
			if(!mInitialized)
				return (Integer) call("getUid");

			return mUid;
		}

		public List<OpEntryWrapper> getOps()
		{
			if(!mInitialized)
				return OpEntryWrapper.convertList((List<?>) call("getOps"));

			return mEntries;
		}
	}

	public static class OpEntryWrapper extends ObjectWrapper
	{
		private OpEntryWrapper(Object obj)
		{
			super(obj);
			mOp = getOp();
			mMode = getMode();
			mTime = getTime();
			mRejectTime = getRejectTime();
			mDuration = getDuration();
			mInitialized = true;
		}

		private final int mOp;
		private final int mMode;
		private final long mTime;
		private final long mRejectTime;
		private final int mDuration;

		private int mUid = -1;
		private String mPackageName = null;

		private boolean mInitialized = false;

		public OpEntryWrapper(int op, int mode, long time, long rejectTime, int duration)
		{
			super(null);

			mOp = op;
			mMode = mode;
			mTime = time;
			mRejectTime = rejectTime;
			mDuration = duration;
			mInitialized = true;
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
			if(!mInitialized)
				return (Integer) call("getOp");

			return mOp;
		}

		public int getMode()
		{
			if(!mInitialized)
				return (Integer) call("getMode");

			return mMode;
		}

		public long getTime()
		{
			if(!mInitialized)
				return (Long) call("getTime");

			return mTime;
		}

		public long getRejectTime()
		{
			if(!mInitialized)
				return (Long) call("getRejectTime");

			return mRejectTime;
		}

		public boolean isRunning()
		{
			if(!mInitialized)
				return (Boolean) call("isRunning");

			return mDuration == -1;
		}

		public int getDuration()
		{
			if(!mInitialized)
				return (Integer) call("getDuration");

			return mDuration == -1 ? (int)(System.currentTimeMillis() - mTime) : mDuration;
		}

		@Override
		public String toString()
		{
			return
					"OpEntryWrapper@" + Integer.toHexString(hashCode()) + "{" +
					" op=" + opToName(mOp) + ", mode=" + modeToName(mMode) +
					", time=" + mTime + ", rejectTime=" + mRejectTime +
					", duration=" + mDuration + " }";
		}

		@Override
		public int hashCode()
		{
			return (mOp + 1) * 0x0f1f1f1f
					/*^ mMode << 23
					^ (int) (mTime ^ mTime >>> 32)
					^ (int) (mRejectTime ^ mRejectTime >>> 32)
					^ mDuration*/;
		}

		@Override
		public boolean equals(Object o)
		{
			if(o == null || !(o instanceof OpEntryWrapper))
				return false;

			final OpEntryWrapper other = (OpEntryWrapper) o;

			if(mOp != other.mOp)
				return false;
			/*if(mMode != other.mMode)
				return false;
			if(mTime != other.mTime)
				return false;
			if(mRejectTime != other.mRejectTime)
				return false;
			if(mDuration != other.mDuration)
				return false;*/

			return true;
		}
	}
}
