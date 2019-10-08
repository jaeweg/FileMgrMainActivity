package com.aliyunos.filemanager.ui.cloud;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.content.pm.PackageInfo;

import com.aliyunos.filemanager.FileMgrMainActivity;
import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrAccount;
import com.aliyunos.filemanager.core.FileMgrLoginEvent;
import com.aliyunos.filemanager.core.FileMgrNetworkInfo;
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.view.TabFragment;
import com.aliyunos.filemanager.util.PreferenceUtil;
import com.aliyunos.filemanager.core.CloudFileMgr;
import com.kanbox.sdk.KbxInitializer;
import com.kanbox.sdk.filelist.KbxFileManager;
import com.squareup.otto.Subscribe;
import com.kanbox.sdk.common.KbxErrorCode;
import com.kanbox.sdk.common.KbxRequest;
import com.kanbox.sdk.common.KbxResponse;
import com.kanbox.sdk.common.KbxAcInfoResponse;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

public class CloudFragment extends TabFragment implements CloudDelegate {
    private static final String TAG = "CloudFragment";
    private CloudFileListView mFileListView;
    private CloudFileListAdapter mFileListAdapter;
    private static final int REQUEST_CODE_LOGIN = 100;
    private static final int REQUEST_CONNECT_NETWORK = 101;
    private boolean isFirstComing = false;
    private boolean isScriptInit = false;
    private Activity mActivity;
    private ViewGroup mMainLayout;
    private static final String ACTION_ACCOUNT_ADDED  = "com.aliyun.xiaoyunmi.action.AYUN_LOGIN_BROADCAST";
    private static final String ACTION_ACCOUNT_DELETE = "com.aliyun.xiaoyunmi.action.DELETE_ACCOUNT";
    private static final String ACTION_LOCALE_CHANGE = "android.intent.action.LOCALE_CHANGED";
    private static final String ACTION_DYN_COLOR_CHANGE = "com.aliyun.action.COLOR_CHANGED";
    private Button mLoginButton;
    private Button mOpenCloudButton;
    private static final int CLOUD_MANAGER_VERSION = 201600927;
    private static final String CLOUD_MANAGER_PACKAGE = "com.yunos.sync.manager";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        if (rootView != null) {
            return rootView;
        }

        mRootView = inflater.inflate(R.layout.cloud_main, container, false);
        ViewStub viewStub = (ViewStub)mRootView.findViewById(R.id.cloud_view);
        mMainLayout = (ViewGroup)viewStub.inflate();
        mFileListAdapter = new CloudFileListAdapter(this.getActivity());
        mFileListView = new CloudFileListView(this.getActivity());
        mFileListView.init(mMainLayout, mFileListAdapter, this);
        mActivity = this.getActivity();

        mLoginButton = (Button) mRootView.findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!FileMgrNetworkInfo.getsInstance().hasNetwork()) {
                    showAlertDialog(mActivity);
                } else {
                    Intent intent = new Intent();
                    intent.setAction("com.yunos.account.action.LOGIN");
                    intent.putExtra("From" , "AliFileBrowser");
                    startActivityForResult(intent, REQUEST_CODE_LOGIN);
                }
            }
        });

        mOpenCloudButton = (Button) mRootView.findViewById(R.id.open_cloud_button);
        mOpenCloudButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!FileMgrNetworkInfo.getsInstance().hasNetwork()) {
                    showAlertDialog(mActivity);
                    return;
                }
                PreferenceUtil.setBoolean(PreferenceUtil.Setting.SETTTING_CLOSE_CLOUD, false);
                if(FileMgrAccount.getsInstance().isKanboxLogined()) {
                    checkCloudSpace();
                    showCloudFileListView(true);
                } else {
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected  Boolean doInBackground(Void... params) {
                            return FileMgrAccount.getsInstance().isSystemAccountLogined();
                        }

                        @Override
                        protected void onPostExecute(final Boolean isLogined) {
                            if (!isLogined) {
                                Intent intent = new Intent();
                                intent.setAction("com.yunos.account.action.LOGIN");
                                intent.putExtra("From" , "AliFileBrowser");
                                startActivityForResult(intent, REQUEST_CODE_LOGIN);
                            } else {
                                mFileListView.showMessageDialog(mActivity.getString(R.string.refreshing));
                                showCloudFileListView(true);
                                FileMgrAccount.getsInstance().login();
                            }
                        }
                    }.execute();
                }
            }
        });

        Button noNetworkView = (Button) mRootView.findViewById(R.id.online_subtypes_no_network_btn);
        noNetworkView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!FileMgrNetworkInfo.getsInstance().hasNetwork()) {
                    showAlertDialog(mActivity);
                } else if (FileMgrAccount.getsInstance().isKanboxLogined()) {
                    mFileListView.refreshCurrentFileListFromServer();
                } else {
                    if (FileMgrNetworkInfo.getsInstance().showNetworkIsAvailable(mActivity)) {
                        login();
                    }
                }
            }
        });

        if (FileMgrAccount.getsInstance().isKanboxLogined()) {
            mFileListView.setupRootInfo();
        }
        isFirstComing = true;

        init();
        if(needCloseCloud()){
            showCloseCloudView();
        }
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
        if (mFileListView != null) {
            mFileListView.onResume();
        }

        if(FileMgrNetworkInfo.getsInstance().hasNetwork() && FileMgrAccount.getsInstance().isKanboxLogined()) {
            View networkView = mRootView.findViewById(R.id.no_network_view);
            if (networkView.getVisibility()== View.INVISIBLE) {
                showCloudFileListView(true);
                mFileListView.refreshCurrentFileList();
            }
        }

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected  Boolean doInBackground(Void... params) {
                return FileMgrAccount.getsInstance().isSystemAccountLogined();
            }

            @Override
            protected void onPostExecute(final Boolean isLogined) {
                super.onPostExecute(isLogined);
                Log.d(TAG," onResume , SystemLogined = "+isLogined);

                if (!isLogined) {
                    showLoginView(true);
                } else if (!FileMgrNetworkInfo.getsInstance().hasNetwork()) {
                    showNetworkErrorView(true);
                }

                if(((FileMgrMainActivity)mActivity).getCurrentTab() == FileMgrMainActivity.FragmentTab.CloudTab) {
                    refreshCloudView();
                }
            }
        }.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);

        if (mFileListView !=null) {
            mFileListView.onPause();
        }
    }

    private boolean needCloseCloud(){
        return PreferenceUtil.getBoolean(PreferenceUtil.Setting.SETTTING_CLOSE_CLOUD);
    }

    public void refreshCloudView() {
        if (mRootView == null) {
            return;
        }

        if (FileMgrNetworkInfo.getsInstance().hasNetwork() && FileMgrAccount.getsInstance().isKanboxLogined()) {
            View networkView = mRootView.findViewById(R.id.no_network_view);
            if (networkView.getVisibility()== View.VISIBLE) {
                return;
            }
        }

        new AsyncTask<Void, Void, Boolean>() {
            String curHavanaId = "";
            @Override
            protected  Boolean doInBackground(Void... params) {
                curHavanaId = FileMgrAccount.getsInstance().getHavanaId();
                return FileMgrAccount.getsInstance().isSystemAccountLogined();
            }

            @Override
            protected void onPostExecute(final Boolean isLogined) {
                super.onPostExecute(isLogined);

                boolean isAccountChanged = false;
                String oldHavanaId =  PreferenceUtil.getString(PreferenceUtil.Setting.SETTTING_LOGIN_ID);
                /* YUNOS BEGIN PB */
                //##module:AliFileBrowser ##author:wutao.wt@alibaba-inc.com
                //##BugID:(8016322) ##date:2016-03-28
                //##description:Judge whether the object is null
                if (curHavanaId != null && !curHavanaId.isEmpty() && oldHavanaId != null && !oldHavanaId.isEmpty() && !curHavanaId.equals(oldHavanaId)) {
                /* YUNOS END PB */
                    isAccountChanged = true;
                }else {
                    isAccountChanged = false;
                }

                if (!isLogined) {
                    showLoginView(true);
                } else if (!FileMgrNetworkInfo.getsInstance().hasNetwork()) {
                    showNetworkErrorView(true);
                } else if (!FileMgrAccount.getsInstance().isKanboxLogined()) {
                    login();
                } else if (isAccountChanged) {
                    FileMgrAccount.getsInstance().login();
                    showCloudFileListView(true);
                    login();
                } else if(isFirstComing) {
                    isFirstComing =false;
                    mFileListView.refreshCurrentFileListFromServer();
                } else{
                    showCloudFileListView(true);
                }
            }
        }.execute();
    }

    @Override
    public void onDestroy() {
        if (mActivity != null && mBroadcastReceiver != null) {
            mActivity.unregisterReceiver(mBroadcastReceiver);
        }
        if (mRootView != null) {
            mRootView.findViewById(R.id.login_button).setOnClickListener(null);
            mRootView.findViewById(R.id.online_subtypes_no_network_btn).setOnClickListener(null);
        }
        super.onDestroy();

        if (mFileListView != null) {
            mFileListView.onDestroy();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(TAG, "finalize");
    }

    private void init() {
        BusProvider.getInstance().register(this);
        IntentFilter intentFilter = new IntentFilter(ACTION_ACCOUNT_DELETE);
        mActivity.registerReceiver(mBroadcastReceiver, intentFilter, "com.aliyun.account.permission.SEND_MANAGE_DATA", null);

        IntentFilter intentFilter1 = new IntentFilter(ACTION_ACCOUNT_ADDED);
        mActivity.registerReceiver(mBroadcastReceiver, intentFilter1, "com.aliyun.account.permission.SEND_MANAGE_DATA", null);

        IntentFilter intentFilter2 = new IntentFilter(ACTION_LOCALE_CHANGE);
        mActivity.registerReceiver(mBroadcastReceiver, intentFilter2);

        IntentFilter intentFilter3 = new IntentFilter(ACTION_DYN_COLOR_CHANGE);
        mActivity.registerReceiver(mBroadcastReceiver, intentFilter3);
    }

    private void showCloseCloudView() {
        if(mRootView == null) return;
        View loginView = mRootView.findViewById(R.id.login_view);
        showNetworkErrorView(false);
        showCloudFileListView(false);
        loginView.setVisibility(View.VISIBLE);
        changeToLoginButton(false);
    }

    private void changeToLoginButton(boolean isLogin){
        mLoginButton.setVisibility(isLogin ? View.VISIBLE : View.GONE);
        mOpenCloudButton.setVisibility(isLogin ? View.GONE : View.VISIBLE);
    }

    private void showLoginView(boolean isShow) {
        if(mRootView == null || needCloseCloud()) return;
        View loginView = mRootView.findViewById(R.id.login_view);
        if (isShow) {
            showNetworkErrorView(false);
            showCloudFileListView(false);
            loginView.setVisibility(View.VISIBLE);
            changeToLoginButton(true);
        } else {
            loginView.setVisibility(View.INVISIBLE);
        }
    }

    private void showNetworkErrorView(boolean isShow) {
        if(mRootView == null) return;
        View networkView = mRootView.findViewById(R.id.no_network_view);
        if (isShow) {
            if(needCloseCloud()){
                return;
            }
            showCloudFileListView(false);
            showLoginView(false);
            networkView.setVisibility(View.VISIBLE);
        } else {
            networkView.setVisibility(View.INVISIBLE);
        }
    }

    private void showCloudFileListView(boolean isShow) {
        mFileListView.show(isShow);
        if(isShow) {
            if(needCloseCloud()){
                return;
            }
            showLoginView(false);
            showNetworkErrorView(false);
            mMainLayout.setVisibility(View.VISIBLE);
        } else {
            mMainLayout.setVisibility(View.INVISIBLE);
        }
    }

    private void login() {
        showCloudFileListView(false);

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected  Boolean doInBackground(Void... params) {
                return FileMgrAccount.getsInstance().isSystemAccountLogined();
            }

            @Override
            protected void onPostExecute(final Boolean isLogined) {
                super.onPostExecute(isLogined);

                // 如果系统没有登录，则取消登录
                if (!isLogined) {
                    showLoginView(true);
                    return;
                }

                // 如果酷盘已经登录，返回
                if (FileMgrAccount.getsInstance().isKanboxLogined()) {
                    showCloudFileListView(true);
                    return;
                }

                if(!FileMgrNetworkInfo.getsInstance().hasNetwork()) {
                    showNetworkErrorView(true);
                }else {
                    mFileListView.showMessageDialog(mActivity.getString(R.string.refreshing));
                    FileMgrAccount.getsInstance().login();
                }
            }
        }.execute();
    }

    @Override
    public void onPageScrolled() {
        super.onPageScrolled();
        if(!isScriptInit) {
            KbxInitializer.startNetWorkActivity();
            isScriptInit = true;
        }
        refreshCloudView();
    }

    @Override
    public void onFetchListError(int errorCode) {
        if (errorCode == 0) {
            this.showCloudFileListView(true);
        } else {
            this.showNetworkErrorView(true);
        }
    }

    @Override
    public void onShowSpaceWarning(){
        showSpaceWarning(mActivity, true);
    }

    @Override
    public void onCloseCloud() {
        Log.d(TAG," onCloseCloud ");
        PreferenceUtil.setBoolean(PreferenceUtil.Setting.SETTTING_CLOSE_CLOUD, true);
        showCloseCloudView();
    }

    @Subscribe
    public void onLoginCompleted(FileMgrLoginEvent event) {
        Log.d(TAG," onLoginCompleted , event : "+event.getEvent());
        if (event.getEvent() == 0) {
            showCloudFileListView(true);
            mFileListView.gotoRootDir();
            boolean isShowTips = PreferenceUtil.getBoolean(PreferenceUtil.Setting.SETTINGS_SHOW_TIPS);
            if(isShowTips) {
                PreferenceUtil.setBoolean(PreferenceUtil.Setting.SETTINGS_SHOW_TIPS,false);
                showTipsDialog(mActivity);
            }
            checkCloudSpace();
        } else if(event.getEvent() == 1){
            mFileListView.hideMessageDialog();
            showNetworkErrorView(true);
        } else if(event.getEvent() == 2) {
            mFileListView.hideMessageDialog();
            Intent intent = new Intent();
            intent.setAction("com.yunos.account.action.LOGIN");
            intent.putExtra("From" , "AliFileBrowser");
            startActivityForResult(intent, REQUEST_CODE_LOGIN);
        }
    }

    private void checkCloudSpace(){
        CloudFileMgr.getsInstance().getAcInfo(new KbxRequest<KbxAcInfoResponse>(
            new KbxResponse<KbxAcInfoResponse>() {
                @Override
                public void onResponse(KbxAcInfoResponse response) {
                    boolean needQuotaWarning = response.getNeedQuotaWarning();
                    int errorNo = response.getErrorNo();
                    Log.d(TAG,"getAcInfo , response: needQuotaWarning = "+needQuotaWarning+", errorNo = "+errorNo);
                    if(needQuotaWarning){
                        showSpaceWarning(mActivity, false);
                    }
                }
            }
        ));
    }

    private void showSpaceWarning(Context ctx, boolean upload){
        if (((FileMgrMainActivity)mActivity).getCurrentTab() != FileMgrMainActivity.FragmentTab.CloudTab
                || needCloseCloud()) {
            return;
        }

        if(!upload){
            long lastTimeMillis = PreferenceUtil.getLong(PreferenceUtil.Setting.SETTTING_SPACE_WARNING);
            long currentTimeMillis = System.currentTimeMillis();
            if(lastTimeMillis != 0){
                if(currentTimeMillis - lastTimeMillis < 24*3600 *1000) {
                    return;
                }
            }
            PreferenceUtil.setLong(PreferenceUtil.Setting.SETTTING_SPACE_WARNING, currentTimeMillis);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setPositiveButton(R.string.manage_storage, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String action = getCloudManagerAction(mActivity);
                Intent intent = new Intent();
                intent.setAction(action);
                try{
                    startActivity(intent);
                }catch (Exception e){
                    Log.d(TAG, " start cloud manager fail: "+e);
                    startActivity(new Intent("com.yunos.action.SYNC_AND_BACKUP"));
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        builder.setMessage(upload ? R.string.cloud_space_no_enough_msg : R.string.cloud_space_warning_msg);
        AlertDialog dd = builder.create();
        dd.show();
    }

    private String getCloudManagerAction(Context context){
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(CLOUD_MANAGER_PACKAGE, 0);
            String versionName = info.versionName;
            int versionCode = info.versionCode;
            Log.d(TAG,"checkCloudManagerVersion , versionName = "+versionName+", versionCode = "+versionCode);
            if(versionCode > CLOUD_MANAGER_VERSION){
                return "com.yunos.sync.action.SPACE_MGR";
            } else {
                return "com.yunos.sync.manager.CLOUDDATAMANAGER";
            }
        } catch (Exception e) {
            Log.e(TAG,"getCloudManagerVersion cannot get right version");
            e.printStackTrace();
            return "com.yunos.action.SYNC_AND_BACKUP";
        }
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LOGIN) {
            if(resultCode == Activity.RESULT_OK){
                showCloudFileListView(true);
            }else {
                showLoginView(true);
            }
        } else if (requestCode == REQUEST_CONNECT_NETWORK) {
            login();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showAlertDialog(Context ctx) {
        if (((FileMgrMainActivity)mActivity).getCurrentTab() != FileMgrMainActivity.FragmentTab.CloudTab) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setPositiveButton(R.string.setting, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_WIRELESS_SETTINGS);
                startActivityForResult(intent, REQUEST_CONNECT_NETWORK);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        builder.setMessage(R.string.nonetwork_tips);
        AlertDialog dd = builder.create();
        dd.show();
    }

    private void showTipsDialog(Context ctx) {
        long nDrivespaceByByte = KbxFileManager.getTotalSpaceSize();
        double nsapce= nDrivespaceByByte/Math.pow(1000,4);
        int nDrivespaceByTB = (int)nsapce;
        if(nDrivespaceByTB == 0)
            return;

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setNegativeButton(mActivity.getString(R.string.ok), null);
        String sTitleFormat=getResources().getString(R.string.drive_get_tips);
        String sTitle = String.format(sTitleFormat,nDrivespaceByTB);
        builder.setTitle(sTitle);
        AlertDialog dd = builder.create();
        dd.show();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1 != null && ACTION_ACCOUNT_DELETE.equals(arg1.getAction())) {
                FileMgrAccount.getsInstance().logout();
                mFileListView.dismissTranserView();
                showLoginView(true);
            }

            if (arg1 != null && ACTION_ACCOUNT_ADDED.equals(arg1.getAction())) {
                showCloudFileListView(true);
            }

            if (arg1 != null && ACTION_LOCALE_CHANGE.equals(arg1.getAction())) {
                FileMgrAccount.getsInstance().cancelTransferProgress();
            }

            if (arg1 != null && ACTION_DYN_COLOR_CHANGE.equals(arg1.getAction())) {
                FileMgrAccount.getsInstance().cancelTransferProgress();
            }
        }
    };

}
