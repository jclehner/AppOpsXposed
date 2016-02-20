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
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

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
	public static final int OP_WRITE_WALLPAPER = getOpInt("OP_WRITE_WALLPAPER");
	public static final int OP_ASSIST_STRUCTURE = getOpInt("OP_ASSIST_STRUCTURE");
	public static final int OP_ASSIST_SCREENSHOT = getOpInt("OP_ASSIST_SCREENSHOT");
	public static final int OP_READ_PHONE_STATE = getOpInt("OP_READ_PHONE_STATE");
	public static final int OP_ADD_VOICEMAIL = getOpInt("OP_ADD_VOICEMAIL");
	public static final int OP_USE_SIP = getOpInt("OP_USE_SIP");
	public static final int OP_PROCESS_OUTGOING_CALLS = getOpInt("OP_PROCESS_OUTGOING_CALLS");
	public static final int OP_USE_FINGERPRINT = getOpInt("OP_USE_FINGERPRINT");
	public static final int OP_BODY_SENSORS = getOpInt("OP_BODY_SENSORS");
	public static final int OP_READ_CELL_BROADCASTS = getOpInt("OP_READ_CELL_BROADCASTS");
	public static final int OP_MOCK_LOCATION = getOpInt("OP_MOCK_LOCATION");
	public static final int OP_READ_EXTERNAL_STORAGE = getOpInt("OP_READ_EXTERNAL_STORAGE");
	public static final int OP_WRITE_EXTERNAL_STORAGE = getOpInt("OP_WRITE_EXTERNAL_STORAGE");
	public static final int OP_TURN_SCREEN_ON = getOpInt("OP_TURN_SCREEN_ON");
	public static final int OP_GET_ACCOUNTS = getOpInt("OP_GET_ACCOUNTS");

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
	public static final int OP_WAKEUP_ALARM = getOpInt("OP_WAKEUP_ALARM");

	/**
	 * @deprecated In module code, use #getBootCompletedOp() instead!
	 */
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
	private AppOpsManagerWrapper(Context context) {
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

	public List<PackageOpsWrapper> getAllPackagesForOps(int[] ops)
	{
		final List<PackageOpsWrapper> pkgs = new ArrayList<>();

		for(PackageInfo pi : mContext.getPackageManager().getInstalledPackages(0))
		{
			final List<PackageOpsWrapper> pkg = getOpsForPackage(pi.applicationInfo.uid,
					pi.packageName, ops);

			if(pkg.size() == 1)
				pkgs.add(pkg.get(0));
			else
				Util.log(pi.packageName + ": pkg.size()=" + pkg.size());
		}

		return pkgs;
	}

	public List<PackageOpsWrapper> getPackagesForOpsMerged(int[] ops)
	{
		final HashMap<String, PackageOpsWrapper> pkgMap = new HashMap<>();
		for(PackageOpsWrapper pkg : getPackagesForOps(ops))
		{
			PackageOpsWrapper pow = pkgMap.get(pkg.getPackageName());
			if(pow == null)
			{
				pow = new PackageOpsWrapper(pkg.getPackageName(), pkg.getUid(), pkg.getOps());
				pkgMap.put(pkg.getPackageName(), pow);
			}
			else
			{
				Util.debug(pkg.getPackageName() + ": merging uid " + pkg.getUid() + " with " + pow.getUid());
				mergeOpEntryWrappers(pow.getOps(), pkg.getOps());
			}
		}

		return new ArrayList<>(pkgMap.values());
	}

	public List<PackageOpsWrapper> getAllOpsForPackage(int uid, String packageName, int[] ops)
	{
		final List<PackageOpsWrapper> pkgs = getPackagesForOps(ops);
		final List<PackageOpsWrapper> ret = new ArrayList<>();

		Util.debug("getAllOpsForPackage(" + uid + ", " + packageName + ")");

		for(PackageOpsWrapper pkg : pkgs)
		{
			Util.debug("  uid=" + pkg.getUid() + ", pkg=" + pkg.getPackageName()
					+ ", count=" + pkg.getOps().size());
			if(packageName.equals(pkg.getPackageName()) || uid == pkg.getUid())
			{
				Util.debug("  added to list");
				ret.add(pkg);
			}
		}

		return ret;
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

	public static String opToName(int op) {
		return callStatic(AppOpsManager.class, "opToName", new Class<?>[] { int.class }, op);
	}

	public static String opToPermission(int op) {
		return callStatic(AppOpsManager.class, "opToPermission", new Class<?>[] { int.class }, op);
	}

	public static int opToSwitch(int op) {
		return callStatic(AppOpsManager.class, "opToSwitch", new Class<?>[] { int.class }, op);
	}

	// 0        AppOpsManager.opToDefaultMode(op)
	// 1        AppOpsManager.opToDefaultMode(op, false)
	// 2        AppOpsManager.sOpDefaultMode[op]
	// 3        (disabled)
	private static int sOpToDefaultModeType = 0;
	private static int[] sOpDefaultModes;

	public static int opToDefaultMode(int op)
	{
		// default op modes were introduced in KitKat
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			return MODE_ALLOWED;

		try
		{
			switch(sOpToDefaultModeType)
			{
				case 0:
					return callStatic(AppOpsManager.class, "opToDefaultMode",
							new Class[]{ int.class }, op);
				case 1:
					return callStatic(AppOpsManager.class, "opToDefaultMode",
							new Class[]{ int.class, boolean.class }, op, false);
				case 2:
					sOpDefaultModes = getStatic(AppOpsManager.class, "sOpDefaultMode");
					// fall through
				default:
					break;
			}
		}
		catch(ReflectiveException e)
		{
			Util.debug(e);
			++sOpToDefaultModeType;
			return opToDefaultMode(op);
		}
		catch(Exception e)
		{
			Util.log(e);
		}

		if(sOpDefaultModes == null)
			sOpDefaultModes = getFallbackDefaultModes();

		if(op < sOpDefaultModes.length)
			return sOpDefaultModes[op];

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

	public static boolean hasTrueBootCompletedOp() {
		return getOpInt("OP_BOOT_COMPLETED") != -1;
	}

	public static boolean isValidOp(int op)
	{
		return op >= 0 && op < _NUM_OP;
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
		// Must not use _NUM_OP here, has this will causes problems
		// with static initialization order
		for(int op = 0; op < getNumOp(); ++op)
		{
			if(permission.equals(opToPermission(op)))
			{
				//Util.debug("Found op #" + op + " with permission " + permission);
				return op;
			}
		}

		//Util.debug("No op found for permission " + permission);

		return -1;
	}

	public static int getBootCompletedOp()
	{
		final int op = getOpInt("OP_BOOT_COMPLETED");
		return op == -1 ? getOpWithPermission(Manifest.permission.RECEIVE_BOOT_COMPLETED) : op;
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

	private static int[] getFallbackDefaultModes()
	{
		final int[] defaults = new int[_NUM_OP];
		for(int op = 0; op != defaults.length; ++op)
			defaults[op] = opToDefaultModeInternal(op);

		return defaults;
	}

	private static int opToDefaultModeInternal(int op)
	{
		if(op == OP_WRITE_SETTINGS || op == OP_SYSTEM_ALERT_WINDOW)
			return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? MODE_ALLOWED : MODE_DEFAULT;

		if(op == OP_WRITE_SMS)
			return MODE_IGNORED;

		if(op == OP_PROJECT_MEDIA || op == OP_ACTIVATE_VPN || op == OP_GET_USAGE_STATS)
			return MODE_DEFAULT;

		if(op == OP_MOCK_LOCATION)
			return MODE_ERRORED;

		return MODE_ALLOWED;
	}

	private static void mergeOpEntryWrappers(List<OpEntryWrapper> dest, List<OpEntryWrapper> src)
	{
		final BitSet bs = new BitSet(_NUM_OP);
		for(OpEntryWrapper oew : dest)
			bs.set(oew.getOp());

		for(OpEntryWrapper oew : src)
		{
			if(!bs.get(oew.getOp()))
				dest.add(oew);
		}
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
		private int mProxyUid = -1;
		private String mProxyPackageName = null;

		public OpEntryWrapper(int op, int mode, long time, long rejectTime, int duration)
		{
			super(null);

			mOp = op;
			mMode = mode;
			mTime = time;
			mRejectTime = rejectTime;
			mDuration = duration;
		}

		public OpEntryWrapper(int op, int mode, long time, long rejectTime, int duration, int proxyId, String proxyPackageName)
		{
			this(op, mode, time, rejectTime, duration);

			mProxyUid = proxyId;
			mProxyPackageName = proxyPackageName;
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

		public int getProxyUid()
		{
			if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
				return -1;

			if(mObj != null)
				return (Integer) call("getProxyUid");

			return mProxyUid;
		}

		public String getProxyPackageName()
		{
			if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
				return null;

			if(mObj != null)
				return (String) call("getProxyPackageName");

			return mProxyPackageName;
		}
	}
}
