package com.aliyunos.filemanager.ui.view;

import android.content.Context;
import android.os.Bundle;
import yunos.support.v4.app.Fragment;
import yunos.support.v4.app.FragmentActivity;
import yunos.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class TabsAdapter extends FragmentPagerAdapter {
    public static final String INDEX_KEY = "INDEX_KEY";

    private final Context mContext;
    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

    final class TabInfo {
        private final String title;
        private final Class<?> cls;
        private final Bundle args;
        private Fragment instance;

        TabInfo(String t, Class<?> c, Bundle a) {
            title = t;
            cls = c;
            args = a;
            instance = null;
        }
    }

    public TabsAdapter(FragmentActivity activity) {
        super(activity.getSupportFragmentManager());
        mContext = activity;
    }

    public void addTab(String title, Class<?> cls, int idx) {
        Bundle args = new Bundle();
        args.putInt(INDEX_KEY, idx);

        TabInfo info = new TabInfo(title, cls, args);
        mTabs.add(info);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabs.get(position).title;
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public Fragment getItem(int position) {
        TabInfo info = mTabs.get(position);
        if (info.instance != null) {
            return info.instance;
        }
        info.instance = Fragment.instantiate(mContext, info.cls.getName(), info.args);
        return info.instance;
    }

    public void registerItem(Fragment fragment,int position) {
        TabInfo info = mTabs.get(position);
        info.instance = fragment;
    }
}
