package com.aliyunos.filemanager.ui.local;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.view.FileListView;
import com.aliyunos.filemanager.ui.view.TabFragment;


public class LocalFragment extends TabFragment {
    private static final String TAG = "LocalFragment";

    private LocalFileListView mFileListView;
    private LocalFileListAdapter mFileListAdapter;
    private static final String ACTION_ACCOUNT_DELETE = "com.aliyun.xiaoyunmi.action.DELETE_ACCOUNT";
    private static final String ACTION_LOCALE_CHANGE = "android.intent.action.LOCALE_CHANGED";
    private static final String ACTION_DYN_COLOR_CHANGE = "com.aliyun.action.COLOR_CHANGED";



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        if (rootView != null) {
            return rootView;
        }

        mRootView = inflater.inflate(R.layout.filelist_view, container, false);
        ViewGroup mainLayout = (ViewGroup)mRootView.findViewById(R.id.filelist_view_container);

        mFileListAdapter = new LocalFileListAdapter(this.getActivity());
        mFileListView = new LocalFileListView(this.getActivity());

        mFileListView.init(mainLayout, mFileListAdapter, FileListView.Mode.Normal);
        mFileListView.gotoRootPath();

        BusProvider.getInstance().register(this);
        IntentFilter intentFilter = new IntentFilter(ACTION_ACCOUNT_DELETE);
        this.getActivity().registerReceiver(mBroadcastReceiver, intentFilter, "com.aliyun.account.permission.SEND_MANAGE_DATA", null);
        IntentFilter intentFilter1 = new IntentFilter(ACTION_LOCALE_CHANGE);
        this.getActivity().registerReceiver(mBroadcastReceiver, intentFilter1);
        IntentFilter intentFilter2 = new IntentFilter(ACTION_DYN_COLOR_CHANGE);
        this.getActivity().registerReceiver(mBroadcastReceiver, intentFilter2);

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mFileListView != null) {
            mFileListView.onResume();
        }
        BusProvider.getInstance().register(this);
    }

    @Override
    public void onActivityResume() {
        super.onActivityResume();
        if (mFileListView != null) {
            mFileListView.checkRootDir();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mFileListView != null) {
            mFileListView.onPause();
        }
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onDestroy() {
        if (this.getActivity() != null && mBroadcastReceiver != null) {
            this.getActivity().unregisterReceiver(mBroadcastReceiver);
        }
        super.onDestroy();

        if (mFileListView != null) {
            mFileListView.onDestroy();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (backToUpLevel()) {
                return true;
            }
        }
        return false;
    }

    private boolean backToUpLevel() {
        if (mFileListView.backToUpLevel()) {
            return true;
        }
        return false;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1 != null && ACTION_ACCOUNT_DELETE.equals(arg1.getAction())) {
                mFileListView.dismissTranserView();
            } else if (arg1 != null && ACTION_LOCALE_CHANGE.equals(arg1.getAction())) {
                FileMgrCore.cancel();
            } else if (arg1 != null && ACTION_DYN_COLOR_CHANGE.equals(arg1.getAction())) {
                FileMgrCore.cancel();
            }
        }
    };

    public boolean checkHasUnFinishTask() {
        if(mFileListView == null)
            return false;

        return mFileListView.hasUnfinishedLongTimeTask();
    }

}
