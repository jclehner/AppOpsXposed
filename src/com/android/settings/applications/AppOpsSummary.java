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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import at.jclehner.appopsxposed.AppListFragment;
import at.jclehner.appopsxposed.R;
import at.jclehner.appopsxposed.SettingsActivity;
import at.jclehner.appopsxposed.util.AppOpsManagerWrapper;
import at.jclehner.appopsxposed.util.ObjectWrapper;
import at.jclehner.appopsxposed.util.Util;

public class AppOpsSummary extends Fragment {
    // layout inflater object used to inflate views
    private LayoutInflater mInflater;

    private ViewGroup mContentContainer;
    private View mRootView;
    private ViewPager mViewPager;

    CharSequence[] mPageNames;
    static AppOpsState.OpsTemplate[] sPageTemplates = new AppOpsState.OpsTemplate[] {
        AppOpsState.LOCATION_TEMPLATE,
        AppOpsState.PERSONAL_TEMPLATE,
        AppOpsState.MESSAGING_TEMPLATE,
        AppOpsState.MEDIA_TEMPLATE,
        AppOpsState.DEVICE_TEMPLATE,
        AppOpsState.BOOTUP_TEMPLATE
    };

    int mCurPos;

    class MyPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new AppOpsCategory(sPageTemplates[position]);
        }

        @Override
        public int getCount() {
            int count = sPageTemplates.length;

            if (AppOpsManagerWrapper.hasTrueBootCompletedOp() || Util.isBootCompletedHackWorking()) {
                int bootCompletedOp = AppOpsManagerWrapper.getBootCompletedOp();
                if (bootCompletedOp != -1) {
                    AppOpsState.BOOTUP_TEMPLATE.ops[0] = bootCompletedOp;
                    return count;
                }

                Util.log("bootCompletedOp is -1");
            }

            return count - 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPageNames[position];
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mCurPos = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                //updateCurrentTab(mCurPos);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // initialize the inflater
        mInflater = inflater;

        View rootView = mInflater.inflate(R.layout.app_ops_summary,
                container, false);
        mContentContainer = container;
        mRootView = rootView;

        mPageNames = getResources().getTextArray(R.array.app_ops_categories);

        mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        MyPagerAdapter adapter = new MyPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(adapter);
        PagerTabStrip tabs = (PagerTabStrip) rootView.findViewById(R.id.tabs);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            tabs.setTabIndicatorColorResource(android.R.color.holo_blue_light);
        } else {
            final TypedValue val = new TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.colorAccent, val, true);
            tabs.setTabIndicatorColor(val.data);
        }

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container != null && "android.preference.PreferenceFrameLayout".equals(container.getClass().getName())) {
            new ObjectWrapper(rootView.getLayoutParams()).set("removeBorders", true);
        }

        return rootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(R.string.show_changed_only_title).setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ((PreferenceActivity) getActivity()).startPreferenceFragment(AppListFragment.newInstance(true), true);
                return true;
            }
        });
        menu.add(R.string.settings).setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                getActivity().startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
        });
    }

}
