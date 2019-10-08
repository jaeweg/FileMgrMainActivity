package com.aliyunos.filemanager.ui.view;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aliyunos.filemanager.FileMgrMainActivity;

import yunos.support.v4.app.Fragment;

public abstract class TabFragment extends Fragment {

    protected View mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        int idx = args.getInt(TabsAdapter.INDEX_KEY);
        FileMgrMainActivity.registerTabFragment(idx, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootView != null) {
            ViewGroup parent = (ViewGroup) mRootView.getParent();
            if (parent != null) {
                parent.removeView(mRootView);
            }
            return mRootView;
        }
        return null;
    }

    public void onPageScrolled() {}

    public void onActivityResume() {}

    public abstract boolean onKeyUp(int keyCode, KeyEvent event);
}
