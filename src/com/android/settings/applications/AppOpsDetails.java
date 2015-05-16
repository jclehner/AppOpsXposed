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

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import at.jclehner.appopsxposed.BuildConfig;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper.OpEntryWrapper;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper.PackageOpsWrapper;

public class AppOpsDetails extends Fragment {
    static final String TAG = "AppOpsDetails";

    public static final String ARG_PACKAGE_NAME = "package";

    private AppOpsState mState;
    private PackageManager mPm;
    private AppOpsManagerWrapper mAppOps;
    private PackageInfo mPackageInfo;
    private LayoutInflater mInflater;
    private View mRootView;
    private TextView mAppVersion;
    private LinearLayout mOperationsSection;

    // Utility method to set application label and icon.
    private void setAppLabelAndIcon(PackageInfo pkgInfo) {
        final View appSnippet = mRootView.findViewById(R.id.app_snippet);
        appSnippet.setPaddingRelative(0, appSnippet.getPaddingTop(), 0, appSnippet.getPaddingBottom());

        ImageView icon = (ImageView) appSnippet.findViewById(R.id.app_icon);
        icon.setImageDrawable(mPm.getApplicationIcon(pkgInfo.applicationInfo));
        // Set application name.
        TextView label = (TextView) appSnippet.findViewById(R.id.app_name);
        label.setText(mPm.getApplicationLabel(pkgInfo.applicationInfo));
        // Version number of application
        mAppVersion = (TextView) appSnippet.findViewById(R.id.app_size);

        final StringBuilder sb = new StringBuilder(pkgInfo.packageName);

        if (pkgInfo.versionName != null) {
            sb.append("\n");
            sb.append(getActivity().getString(R.string.version_text, pkgInfo.versionName));
        }

        mAppVersion.setText(sb);
    }

    private String retrieveAppEntry() {
        final Bundle args = getArguments();
        String packageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        if (packageName == null) {
            Intent intent = (args == null) ?
                    getActivity().getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null) {
                packageName = intent.getData().getSchemeSpecificPart();
            }
        }
        try {
            mPackageInfo = mPm.getPackageInfo(packageName,
                    PackageManager.GET_DISABLED_COMPONENTS |
                    PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Exception when retrieving package:" + packageName, e);
            mPackageInfo = null;
        }

        return packageName;
    }

    private boolean refreshUi() {
        if (mPackageInfo == null) {
            return false;
        }

        setAppLabelAndIcon(mPackageInfo);

        Resources res = getActivity().getResources();

        mOperationsSection.removeAllViews();
        boolean hasBootupSwitch = false;
        String lastPermGroup = "";
        for (AppOpsState.OpsTemplate tpl : AppOpsState.ALL_TEMPLATES) {
            List<AppOpsState.AppOpEntry> entries = mState.buildState(tpl,
                    mPackageInfo.applicationInfo.uid, mPackageInfo.packageName);
            for (final AppOpsState.AppOpEntry entry : entries) {
                final OpEntryWrapper firstOp = entry.getOpEntry(0);
                final View view = mInflater.inflate(R.layout.app_ops_details_item,
                        mOperationsSection, false);
                String perm = AppOpsManagerWrapper.opToPermission(firstOp.getOp());
                if (perm != null) {
                    if (Manifest.permission.RECEIVE_BOOT_COMPLETED.equals(perm)) {
                        if (!hasBootupSwitch) {
                            hasBootupSwitch = true;
                        } else {
                            Log.i(TAG, "Skipping second bootup switch");
                            continue;
                        }
                    }
                    try {
                        PermissionInfo pi = mPm.getPermissionInfo(perm, 0);
                        if (pi.group != null && !lastPermGroup.equals(pi.group)) {
                            lastPermGroup = pi.group;
                            PermissionGroupInfo pgi = mPm.getPermissionGroupInfo(pi.group, 0);
                            if (pgi.icon != 0) {
                                ((ImageView)view.findViewById(R.id.op_icon)).setImageDrawable(
                                        pgi.loadIcon(mPm));
                            }
                        }
                    } catch (NameNotFoundException e) {
                    }
                }
                ((TextView)view.findViewById(R.id.op_name)).setText(
                        entry.getSwitchText(getActivity(), mState));
                ((TextView)view.findViewById(R.id.op_time)).setText(
                        entry.getTimeText(res, true));

                final int switchOp = AppOpsState.opToSwitch(firstOp.getOp());

                if(BuildConfig.DEBUG && true)
                {
                    final int currentMode = mAppOps.checkOpNoThrow(switchOp, entry.getPackageOps().getUid(),
                            entry.getPackageOps().getPackageName());

                    final int modes[] = {
                            AppOpsManagerWrapper.MODE_ALLOWED,
                            AppOpsManagerWrapper.MODE_ASK,
                            AppOpsManagerWrapper.MODE_HINT,
                            AppOpsManagerWrapper.MODE_IGNORED,
                            AppOpsManagerWrapper.MODE_ERRORED,
                            AppOpsManagerWrapper.MODE_DEFAULT
                    };

                    final int[] indexToMode = new int[modes.length];
                    final List<String> modeNames = new ArrayList<String>();
                    int currentIndex = 0;

                    for (int mode : modes) {
                        if (mode == -1) continue;
                        if (mode == currentMode) currentIndex = modeNames.size();
                        indexToMode[modeNames.size()] = mode;
                        modeNames.add(AppOpsManagerWrapper.modeToName(mode));
                    }

                    view.findViewById(R.id.switchWidget).setVisibility(View.GONE);

                    Spinner sp = (Spinner)view.findViewById(R.id.spinnerWidget);
                    sp.setVisibility(View.VISIBLE);
                    sp.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                            modeNames));
                    sp.setSelection(currentIndex);
                    sp.setOnItemSelectedListener(new OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent,
                                View view, int position, long id) {
                            setMode(indexToMode[position]);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                           setMode(currentMode);
                        }

                        private void setMode(int mode) {
                            mAppOps.setMode(switchOp, entry.getPackageOps().getUid(),
                                entry.getPackageOps().getPackageName(), mode);
                        }
					});

                    mOperationsSection.addView(view);
                    continue;
                }

                Switch sw = (Switch)view.findViewById(R.id.switchWidget);
                sw.setChecked(modeToChecked(switchOp, entry.getPackageOps()));
                sw.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mAppOps.setMode(switchOp, entry.getPackageOps().getUid(),
                                entry.getPackageOps().getPackageName(), isChecked
                                ? AppOpsManagerWrapper.MODE_ALLOWED : AppOpsManagerWrapper.MODE_IGNORED);
                    }
                });
                mOperationsSection.addView(view);
            }
        }

        return true;
    }

    private boolean modeToChecked(int switchOp, PackageOpsWrapper ops) {
        final int mode = mAppOps.checkOpNoThrow(switchOp, ops.getUid(), ops.getPackageName());
        if (mode == AppOpsManagerWrapper.MODE_ALLOWED)
            return true;
        if (mode == AppOpsManagerWrapper.MODE_DEFAULT)
            return true;
        if (mode == AppOpsManagerWrapper.MODE_ASK)
            return true;
        if (mode == AppOpsManagerWrapper.MODE_HINT)
            return true;

        return false;
    }

    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        Intent intent = new Intent();
        intent.putExtra("chg", appChanged);
        PreferenceActivity pa = (PreferenceActivity)getActivity();
        pa.finishPreferencePanel(this, Activity.RESULT_OK, intent);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mState = new AppOpsState(getActivity());
        mPm = getActivity().getPackageManager();
        mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAppOps = AppOpsManagerWrapper.from(getActivity());

        retrieveAppEntry();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.app_ops_details, container, false);
        //Utils.prepareCustomPreferencesList(container, view, view, false);

        mRootView = view;
        mOperationsSection = (LinearLayout)view.findViewById(R.id.operations_section);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!refreshUi()) {
            setIntentAndFinish(true, true);
        }
    }
}
