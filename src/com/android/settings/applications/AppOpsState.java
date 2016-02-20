/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings.applications;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.util.SparseArray;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper.OpEntryWrapper;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper.PackageOpsWrapper;
import at.jclehner.appopsxposed.util.OpsLabelHelper;

public class AppOpsState {
    static final String TAG = "AppOpsState";
    static final boolean DEBUG = false;

    final Context mContext;
    final AppOpsManagerWrapper mAppOps;
    final PackageManager mPm;
    final CharSequence[] mOpSummaries;
    final CharSequence[] mOpLabels;

    List<AppOpEntry> mApps;

    @TargetApi(19)
    public AppOpsState(Context context) {
        mContext = context;
        mAppOps = AppOpsManagerWrapper.from(context);
        mPm = context.getPackageManager();
        //mOpSummaries = context.getResources().getTextArray(R.array.app_ops_summaries);
        mOpSummaries = OpsLabelHelper.getOpSummaries(context);
        //mOpLabels = context.getResources().getTextArray(R.array.app_ops_labels);
        mOpLabels = OpsLabelHelper.getOpLabels(context);

        /*if (AppOpsManagerWrapper.hasFakeBootCompletedOp()) {
            mOpSummaries[AppOpsManagerWrapper.OP_VIBRATE] = mOpSummaries[AppOpsManagerWrapper.OP_VIBRATE]
                    + "/" + mOpSummaries[AppOpsManagerWrapper.OP_POST_NOTIFICATION];

            mOpLabels[AppOpsManagerWrapper.OP_VIBRATE] = mOpLabels[AppOpsManagerWrapper.OP_VIBRATE]
                    + "/" + mOpLabels[AppOpsManagerWrapper.OP_POST_NOTIFICATION];

            final CharSequence summary = OpsLabelHelper.getPermissionLabel(context,
                    android.Manifest.permission.RECEIVE_BOOT_COMPLETED);

            mOpSummaries[AppOpsManagerWrapper.OP_POST_NOTIFICATION] = summary;
            mOpLabels[AppOpsManagerWrapper.OP_POST_NOTIFICATION] = Util.capitalizeFirst(summary);
        }*/
    }

    public static class OpsTemplate implements Parcelable {
        public final int[] ops;
        public final boolean[] showPerms;

        public OpsTemplate(int[] _ops, boolean[] _showPerms) {
            ops = _ops;
            showPerms = _showPerms;
        }

        OpsTemplate(Parcel src) {
            ops = src.createIntArray();
            showPerms = src.createBooleanArray();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeIntArray(ops);
            dest.writeBooleanArray(showPerms);
        }

        public static final Creator<OpsTemplate> CREATOR = new Creator<OpsTemplate>() {
            @Override public OpsTemplate createFromParcel(Parcel source) {
                return new OpsTemplate(source);
            }

            @Override public OpsTemplate[] newArray(int size) {
                return new OpsTemplate[size];
            }
        };
    }

    public static final OpsTemplate LOCATION_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManagerWrapper.OP_COARSE_LOCATION,
                    AppOpsManagerWrapper.OP_FINE_LOCATION,
                    AppOpsManagerWrapper.OP_GPS,
                    AppOpsManagerWrapper.OP_WIFI_SCAN,
                    AppOpsManagerWrapper.OP_NEIGHBORING_CELLS,
                    AppOpsManagerWrapper.OP_MONITOR_LOCATION,
                    AppOpsManagerWrapper.OP_MONITOR_HIGH_POWER_LOCATION,
                    AppOpsManagerWrapper.OP_MOCK_LOCATION, },
            new boolean[] { true,
                    true,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true}
            );

    public static final OpsTemplate PERSONAL_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManagerWrapper.OP_READ_CONTACTS,
                    AppOpsManagerWrapper.OP_WRITE_CONTACTS,
                    AppOpsManagerWrapper.OP_READ_CALL_LOG,
                    AppOpsManagerWrapper.OP_WRITE_CALL_LOG,
                    AppOpsManagerWrapper.OP_READ_CALENDAR,
                    AppOpsManagerWrapper.OP_WRITE_CALENDAR,
                    AppOpsManagerWrapper.OP_DELETE_CALL_LOG,
                    AppOpsManagerWrapper.OP_DELETE_CONTACTS,
                    AppOpsManagerWrapper.OP_ACCESS_XIAOMI_ACCOUNT,
                    AppOpsManagerWrapper.OP_READ_PHONE_STATE,
                    AppOpsManagerWrapper.OP_PROCESS_OUTGOING_CALLS,
                    AppOpsManagerWrapper.OP_USE_FINGERPRINT,
                    AppOpsManagerWrapper.OP_BODY_SENSORS,
                    AppOpsManagerWrapper.OP_READ_EXTERNAL_STORAGE,
                    AppOpsManagerWrapper.OP_WRITE_EXTERNAL_STORAGE,
                    AppOpsManagerWrapper.OP_GET_ACCOUNTS,
                    AppOpsManagerWrapper.OP_READ_CLIPBOARD,
                    AppOpsManagerWrapper.OP_WRITE_CLIPBOARD },
            new boolean[] { true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    false,
                    false }
            );

    public static final OpsTemplate MESSAGING_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManagerWrapper.OP_READ_SMS,
                    AppOpsManagerWrapper.OP_RECEIVE_SMS,
                    AppOpsManagerWrapper.OP_RECEIVE_EMERGECY_SMS,
                    AppOpsManagerWrapper.OP_RECEIVE_MMS,
                    AppOpsManagerWrapper.OP_RECEIVE_WAP_PUSH,
                    AppOpsManagerWrapper.OP_WRITE_SMS,
                    AppOpsManagerWrapper.OP_SEND_SMS,
                    AppOpsManagerWrapper.OP_READ_ICC_SMS,
                    AppOpsManagerWrapper.OP_WRITE_ICC_SMS,
                    AppOpsManagerWrapper.OP_SEND_MMS,
                    AppOpsManagerWrapper.OP_READ_MMS,
                    AppOpsManagerWrapper.OP_DELETE_SMS,
                    AppOpsManagerWrapper.OP_DELETE_MMS,
                    AppOpsManagerWrapper.OP_WRITE_MMS },
            new boolean[] { true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true }
            );

    public static final OpsTemplate MEDIA_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManagerWrapper.OP_VIBRATE,
                    AppOpsManagerWrapper.OP_CAMERA,
                    AppOpsManagerWrapper.OP_RECORD_AUDIO,
                    AppOpsManagerWrapper.OP_PLAY_AUDIO,
                    AppOpsManagerWrapper.OP_TAKE_MEDIA_BUTTONS,
                    AppOpsManagerWrapper.OP_TAKE_AUDIO_FOCUS,
                    AppOpsManagerWrapper.OP_AUDIO_MASTER_VOLUME,
                    AppOpsManagerWrapper.OP_AUDIO_VOICE_VOLUME,
                    AppOpsManagerWrapper.OP_AUDIO_RING_VOLUME,
                    AppOpsManagerWrapper.OP_AUDIO_MEDIA_VOLUME,
                    AppOpsManagerWrapper.OP_AUDIO_ALARM_VOLUME,
                    AppOpsManagerWrapper.OP_AUDIO_NOTIFICATION_VOLUME,
                    AppOpsManagerWrapper.OP_AUDIO_BLUETOOTH_VOLUME,
                    AppOpsManagerWrapper.OP_AUDIO_FM_VOLUME,
                    AppOpsManagerWrapper.OP_AUDIO_MATV_VOLUME,
                    AppOpsManagerWrapper.OP_WRITE_WALLPAPER,
                    AppOpsManagerWrapper.OP_ASSIST_SCREENSHOT,
                    AppOpsManagerWrapper.OP_ASSIST_STRUCTURE,
                    AppOpsManagerWrapper.OP_MUTE_MICROPHONE, },
            new boolean[] { false,
                    true,
                    true,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true, }
            );

    private static final int OP_POST_NOTIFICATION;

    static {
        if (AppOpsManagerWrapper.OP_POST_NOTIFICATION == AppOpsManagerWrapper.OP_BOOT_COMPLETED) {
            OP_POST_NOTIFICATION = -1;
        } else {
            OP_POST_NOTIFICATION = AppOpsManagerWrapper.OP_POST_NOTIFICATION;
        }
    }

    public static final OpsTemplate DEVICE_TEMPLATE = new OpsTemplate(
            new int[] { OP_POST_NOTIFICATION,
                    AppOpsManagerWrapper.OP_ACCESS_NOTIFICATIONS,
                    AppOpsManagerWrapper.OP_CALL_PHONE,
                    AppOpsManagerWrapper.OP_ADD_VOICEMAIL,
                    AppOpsManagerWrapper.OP_USE_SIP,
                    AppOpsManagerWrapper.OP_READ_CELL_BROADCASTS,
                    AppOpsManagerWrapper.OP_TURN_SCREEN_ON,
                    AppOpsManagerWrapper.OP_WRITE_SETTINGS,
                    AppOpsManagerWrapper.OP_SYSTEM_ALERT_WINDOW,
                    AppOpsManagerWrapper.OP_WAKE_LOCK,
                    AppOpsManagerWrapper.OP_ALARM_WAKEUP,
                    AppOpsManagerWrapper.OP_WIFI_CHANGE,
                    AppOpsManagerWrapper.OP_BLUETOOTH_CHANGE,
                    AppOpsManagerWrapper.OP_DATA_CONNECT_CHANGE,
                    AppOpsManagerWrapper.OP_NFC_CHANGE,
                    AppOpsManagerWrapper.OP_PROJECT_MEDIA,
                    AppOpsManagerWrapper.OP_ACTIVATE_VPN,
                    AppOpsManagerWrapper.OP_GET_USAGE_STATS,
                    AppOpsManagerWrapper.OP_EXACT_ALARM,
                    AppOpsManagerWrapper.OP_WAKEUP_ALARM,
                    AppOpsManagerWrapper.OP_TOAST_WINDOW, },
            new boolean[] { false,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,  }
            );

    public static final OpsTemplate BOOTUP_TEMPLATE = new OpsTemplate(
            new int[] { AppOpsManagerWrapper.OP_BOOT_COMPLETED,
                    AppOpsManagerWrapper.OP_AUTO_START },
            new boolean[] { true,
                    true }
            );

    public static final OpsTemplate[] ALL_TEMPLATES = new OpsTemplate[] {
            LOCATION_TEMPLATE, PERSONAL_TEMPLATE, MESSAGING_TEMPLATE,
            MEDIA_TEMPLATE, DEVICE_TEMPLATE, BOOTUP_TEMPLATE
    };

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppEntry {
        private final AppOpsState mState;
        private final ApplicationInfo mInfo;
        private final File mApkFile;
        private final SparseArray<OpEntryWrapper> mOps
                = new SparseArray<OpEntryWrapper>();
        private final SparseArray<AppOpEntry> mOpSwitches
                = new SparseArray<AppOpEntry>();
        private String mLabel;
        private Drawable mIcon;
        private boolean mMounted;
        private boolean mHasDisallowedOps = false;

        public AppEntry(AppOpsState state, ApplicationInfo info) {
            mState = state;
            mInfo = info;
            mApkFile = new File(info.sourceDir);
        }

        public void addOp(AppOpEntry entry, OpEntryWrapper op) {
            mOps.put(op.getOp(), op);
            mHasDisallowedOps |= op.getMode() != AppOpsManagerWrapper.MODE_ALLOWED;
            mOpSwitches.put(opToSwitch(op.getOp()), entry);
        }

        public boolean hasOp(int op) {
            return mOps.indexOfKey(op) >= 0;
        }

        public AppOpEntry getOpSwitch(int op) {
            return mOpSwitches.get(opToSwitch(op));
        }

        public ApplicationInfo getApplicationInfo() {
            return mInfo;
        }

        public String getLabel() {
            return mLabel;
        }

        public Drawable getIcon() {
            if (mIcon == null) {
                if (mApkFile.exists()) {
                    mIcon = mInfo.loadIcon(mState.mPm);
                    return mIcon;
                } else {
                    mMounted = false;
                }
            } else if (!mMounted) {
                // If the app wasn't mounted but is now mounted, reload
                // its icon.
                if (mApkFile.exists()) {
                    mMounted = true;
                    mIcon = mInfo.loadIcon(mState.mPm);
                    return mIcon;
                }
            } else {
                return mIcon;
            }

            return mState.mContext.getResources().getDrawable(
                    android.R.drawable.sym_def_app_icon);
        }

        @Override public String toString() {
            return mLabel;
        }

        void loadLabel(Context context) {
            if (mLabel == null || !mMounted) {
                if (!mApkFile.exists()) {
                    mMounted = false;
                    mLabel = mInfo.packageName;
                } else {
                    mMounted = true;
                    CharSequence label = mInfo.loadLabel(context.getPackageManager());
                    mLabel = label != null ? label.toString() : mInfo.packageName;
                }
            }
        }
    }

    /**
     * This class holds the per-item data in our Loader.
     */
    public static class AppOpEntry {
        private final AppOpsManagerWrapper.PackageOpsWrapper mPkgOps;
        private final ArrayList<OpEntryWrapper> mOps
                = new ArrayList<OpEntryWrapper>();
        private final ArrayList<OpEntryWrapper> mSwitchOps
                = new ArrayList<OpEntryWrapper>();
        private final AppEntry mApp;
        private final int mSwitchOrder;

        public AppOpEntry(PackageOpsWrapper pkg, OpEntryWrapper op, AppEntry app,
                int switchOrder) {
            mPkgOps = pkg;
            mApp = app;
            mSwitchOrder = switchOrder;
            mApp.addOp(this, op);
            mOps.add(op);
            mSwitchOps.add(op);
        }

        private static void addOp(ArrayList<OpEntryWrapper> list, OpEntryWrapper op) {
            for (int i=0; i<list.size(); i++) {
                OpEntryWrapper pos = list.get(i);
                if (pos.isRunning() != op.isRunning()) {
                    if (op.isRunning()) {
                        list.add(i, op);
                        return;
                    }
                    continue;
                }
                if (pos.getTime() < op.getTime()) {
                    list.add(i, op);
                    return;
                }
            }
            list.add(op);
        }

        public void addOp(OpEntryWrapper op) {
            mApp.addOp(this, op);
            addOp(mOps, op);
            if (mApp.getOpSwitch(opToSwitch(op.getOp())) == null) {
                addOp(mSwitchOps, op);
            }
        }

        public AppEntry getAppEntry() {
            return mApp;
        }

        public int getSwitchOrder() {
            return mSwitchOrder;
        }

        public PackageOpsWrapper getPackageOps() {
            return mPkgOps;
        }

        public int getNumOpEntry() {
            return mOps.size();
        }

        public OpEntryWrapper getOpEntry(int pos) {
            return mOps.get(pos);
        }

        public boolean hasDisallowedOps() {
            return mApp.mHasDisallowedOps;
        }

        private static boolean isSwitchChecked(List<OpEntryWrapper> ops, int op) {
            int opSwitch = AppOpsManagerWrapper.opToSwitch(op);
            for (OpEntryWrapper wrapper : ops) {
                if (wrapper.getOp() == opSwitch) {
                    return AppOpsDetails.modeToChecked(wrapper.getMode());
                }
            }
            return true;
        }

        private CharSequence getCombinedText(Context context, ArrayList<OpEntryWrapper> ops,
                CharSequence[] items, boolean isSummary) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            Set<String> strings = new HashSet<>();
            for (int i=0; i<ops.size(); i++) {
                int op = ops.get(i).getOp();
                SpannableString ss;
                if (op < items.length && !TextUtils.isEmpty(items[op])) {
                    ss = new SpannableString(items[op]);
                } else if (isSummary) {
                    ss = new SpannableString(OpsLabelHelper.getOpSummary(context, op));
                } else {
                    ss = new SpannableString(OpsLabelHelper.getOpLabel(context, op));
                }

                if (!strings.add(ss.toString())) {
                    continue;
                }

                if (isSummary && !isSwitchChecked(ops, op)) {
                    ss.setSpan(new StrikethroughSpan(), 0, ss.length(), 0);
                }

                if (i > 0) {
                    builder.append(", ");
                }

                builder.append(ss);
            }

            return builder;
        }

        public CharSequence getSummaryText(Context context, AppOpsState state) {
            return getCombinedText(context, mOps, state.mOpSummaries, true);
        }

        public CharSequence getSwitchText(Context context, AppOpsState state) {
            if (mSwitchOps.size() > 0) {
                return getCombinedText(context, mSwitchOps, state.mOpLabels, false);
            } else {
                return getCombinedText(context, mOps, state.mOpLabels, false);
            }
        }

        public CharSequence getTimeText(Resources res, boolean showEmptyText) {
            if (isRunning()) {
                return res.getText(R.string.app_ops_running);
            }
            if (getTime() > 0) {
                return DateUtils.getRelativeTimeSpanString(getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);
            }
            return showEmptyText ? res.getText(R.string.app_ops_never_used) : "";
        }

        public boolean isRunning() {
            return mOps.get(0).isRunning();
        }

        public long getTime() {
            return mOps.get(0).getTime();
        }

        @Override public String toString() {
            return mApp.getLabel();
        }
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppOpEntry> APP_OP_COMPARATOR = new Comparator<AppOpEntry>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppOpEntry object1, AppOpEntry object2) {
            if (object1.getSwitchOrder() != object2.getSwitchOrder()) {
                return object1.getSwitchOrder() < object2.getSwitchOrder() ? -1 : 1;
            }
            if (object1.isRunning() != object2.isRunning()) {
                // Currently running ops go first.
                return object1.isRunning() ? -1 : 1;
            }
            if (object1.getTime() != object2.getTime()) {
                // More recent times go first.
                return object1.getTime() > object2.getTime() ? -1 : 1;
            }
            if (object1.hasDisallowedOps() != object2.hasDisallowedOps()) {
                // Disallowed ops go first.
                return object1.hasDisallowedOps() ? -1 : 1;
            }
            return sCollator.compare(object1.getAppEntry().getLabel(),
                    object2.getAppEntry().getLabel());
        }
    };

    private void addOp(List<AppOpEntry> entries, PackageOpsWrapper pkgOps,
            AppEntry appEntry, OpEntryWrapper opEntry, boolean allowMerge, int switchOrder) {
        if (allowMerge && entries.size() > 0) {
            AppOpEntry last = entries.get(entries.size()-1);
            if (last.getAppEntry() == appEntry) {
                boolean lastExe = last.getTime() != 0;
                boolean entryExe = opEntry.getTime() != 0;
                if (lastExe == entryExe) {
                    if (DEBUG) Log.d(TAG, "Add op " + opEntry.getOp() + " to package "
                            + pkgOps.getPackageName() + ": append to " + last);
                    last.addOp(opEntry);
                    return;
                }
            }
        }
        AppOpEntry entry = appEntry.getOpSwitch(opEntry.getOp());
        if (entry != null) {
            entry.addOp(opEntry);
            return;
        }
        entry = new AppOpEntry(pkgOps, opEntry, appEntry, switchOrder);
        if (DEBUG) Log.d(TAG, "Add op " + opEntry.getOp() + " to package "
                + pkgOps.getPackageName() + ": making new " + entry);
        entries.add(entry);
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl) {
        return buildState(tpl, 0, null);
    }

    private AppEntry getAppEntry(final Context context, final HashMap<String, AppEntry> appEntries,
            final String packageName, ApplicationInfo appInfo) {
        AppEntry appEntry = appEntries.get(packageName);
        if (appEntry == null) {
            if (appInfo == null) {
                try {
                    appInfo = mPm.getApplicationInfo(packageName,
                            PackageManager.GET_DISABLED_COMPONENTS
                            | PackageManager.GET_UNINSTALLED_PACKAGES);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "Unable to find info for package " + packageName);
                    return null;
                }
            }
            appEntry = new AppEntry(this, appInfo);
            appEntry.loadLabel(context);
            appEntries.put(packageName, appEntry);
        }
        return appEntry;
    }

    public List<AppOpEntry> buildStateWithChangedOpsOnly() {
        final List<PackageOpsWrapper> pkgs = mAppOps.getPackagesForOps(null);
        final HashMap<String, AppEntry> appEntries = new HashMap<String, AppEntry>();
        final List<AppOpEntry> entries = new ArrayList<AppOpEntry>();
        for (PackageOpsWrapper pkg : pkgs) {
            for (OpEntryWrapper op : pkg.getOps()) {
                if (op.getMode() != AppOpsManagerWrapper.MODE_ALLOWED) {
                    final ApplicationInfo appInfo;
                    try {
                        appInfo= mContext.getPackageManager().getApplicationInfo(
                            pkg.getPackageName(), PackageManager.GET_DISABLED_COMPONENTS
                            | PackageManager.GET_UNINSTALLED_PACKAGES);
                    } catch (PackageManager.NameNotFoundException e) {
                        continue;
                    }
                    final AppEntry app = getAppEntry(mContext, appEntries, appInfo.packageName, appInfo);
                    if (app == null) {
                        continue;
                    }
                    addOp(entries, pkg, app, op, true, 0);
                    //entries.add(new AppOpEntry(pkg, op, new AppEntry(this, appInfo), 0));
                }
            }
        }
        return entries;
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl, int uid, String packageName) {
        final Context context = mContext;

        final HashMap<String, AppEntry> appEntries = new HashMap<String, AppEntry>();
        final List<AppOpEntry> entries = new ArrayList<AppOpEntry>();

        final ArrayList<String> perms = new ArrayList<String>();
        final ArrayList<Integer> permOps = new ArrayList<Integer>();
        final int[] opToOrder = new int[AppOpsManagerWrapper._NUM_OP];
        for (int i=0; i<tpl.ops.length; i++) {
            if (isValidOp(tpl.ops[i]) && tpl.showPerms[i]) {
                String perm = AppOpsManagerWrapper.opToPermission(tpl.ops[i]);
                if (perm != null && !perms.contains(perm)) {
                    perms.add(perm);
                    permOps.add(tpl.ops[i]);
                    opToOrder[tpl.ops[i]] = i;
                }
            }
        }

        List<PackageOpsWrapper> pkgs;
        if (packageName != null) {
            pkgs = mAppOps.getOpsForPackage(uid, packageName, tpl.ops);
        } else {
            pkgs = mAppOps.getPackagesForOpsMerged(tpl.ops);
        }

        if (pkgs != null) {
            for (int i=0; i<pkgs.size(); i++) {
                PackageOpsWrapper pkgOps = pkgs.get(i);
                AppEntry appEntry = getAppEntry(context, appEntries, pkgOps.getPackageName(), null);
                if (appEntry == null) {
                    continue;
                }
                for (int j=0; j<pkgOps.getOps().size(); j++) {
                    OpEntryWrapper opEntry = pkgOps.getOps().get(j);
                    addOp(entries, pkgOps, appEntry, opEntry, packageName == null,
                            packageName == null ? 0 : opToOrder[opEntry.getOp()]);
                }
            }
        }

        List<PackageInfo> apps;
        if (packageName != null) {
            apps = new ArrayList<PackageInfo>();
            try {
                PackageInfo pi = mPm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                apps.add(pi);
            } catch (NameNotFoundException e) {
            }
        } else {
            String[] permsArray = new String[perms.size()];
            perms.toArray(permsArray);
            apps = mPm.getPackagesHoldingPermissions(permsArray, 0);
        }

        for (int i=0; i<apps.size(); i++) {
            PackageInfo appInfo = apps.get(i);
            AppEntry appEntry = getAppEntry(context, appEntries, appInfo.packageName,
                    appInfo.applicationInfo);
            if (appEntry == null) {
                continue;
            }
            List<OpEntryWrapper> dummyOps = null;
            PackageOpsWrapper pkgOps = null;
            if (appInfo.requestedPermissions != null) {
                for (int j=0; j<appInfo.requestedPermissions.length; j++) {
                    if (appInfo.requestedPermissionsFlags != null) {
                        if ((appInfo.requestedPermissionsFlags[j]
                                & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                            if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + " perm "
                                    + appInfo.requestedPermissions[j] + " not granted; skipping");
                            continue;
                        }
                    }
                    if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + ": requested perm "
                            + appInfo.requestedPermissions[j]);
                    for (int k=0; k<perms.size(); k++) {
                        if (!perms.get(k).equals(appInfo.requestedPermissions[j])) {
                            continue;
                        }
                        if (DEBUG) Log.d(TAG, "Pkg " + appInfo.packageName + " perm " + perms.get(k)
                                + " has op " + permOps.get(k) + ": " + appEntry.hasOp(permOps.get(k)));
                        if (appEntry.hasOp(permOps.get(k))) {
                            continue;
                        }
                        if (dummyOps == null) {
                            dummyOps = new ArrayList<OpEntryWrapper>();
                            pkgOps = new PackageOpsWrapper(
                                    appInfo.packageName, appInfo.applicationInfo.uid, dummyOps);

                        }
                        int mode = mAppOps.checkOpNoThrow(permOps.get(k), appInfo.applicationInfo.uid, appInfo.packageName);
                        OpEntryWrapper opEntry = new OpEntryWrapper(
                                permOps.get(k), mode, 0, 0, 0);
                        dummyOps.add(opEntry);
                        addOp(entries, pkgOps, appEntry, opEntry, packageName == null,
                                packageName == null ? 0 : opToOrder[opEntry.getOp()]);
                    }
                }
            }
        }

        // Sort the list.
        Collections.sort(entries, APP_OP_COMPARATOR);

        // Done!
        return entries;
    }

    private boolean isValidOp(int op)
    {
        if (op >= 0 && op < AppOpsManagerWrapper._NUM_OP) {
            try {
                //mAppOps.checkOp(op, Process.SYSTEM_UID, "android");
                return true;
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Skipping #" + op + " even though it is within valid range");
            }
        }

        return false;
    }

    private static int opToSwitch(int op)
    {
        // TODO Implement something like expert mode?

        if (true) {
            return AppOpsManagerWrapper.opToSwitch(op);
        }

        return op;
    }
}
