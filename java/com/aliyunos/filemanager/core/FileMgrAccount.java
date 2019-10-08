package com.aliyunos.filemanager.core;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aliyun.ams.tyid.TYIDConstants;
import com.aliyun.ams.tyid.TYIDException;
import com.aliyun.ams.tyid.TYIDManager;
import com.aliyun.ams.tyid.TYIDManagerFuture;
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.util.DeviceUtil;
import com.aliyunos.filemanager.util.PreferenceUtil;
import com.kanbox.filemgr.KbxCloudAccount;
import com.kanbox.filemgr.response.KbxLoginByYunOSResponse;
import com.kanbox.sdk.account.KbxAccountDelegate;
import com.kanbox.sdk.common.KbxRequest;
import com.kanbox.sdk.common.KbxResponse;
public class FileMgrAccount implements KbxAccountDelegate{
    private Context mContext;
    private String mYunOSKp;
    private String mYunOSToken;
    private String mAccountId;
    private String mUUID;
    private String mYunOSVersion;
    private String mClientVersion;
    private String mDeviceType;
    private String mDeviceModel;
    private static String TAG = "FileMgrAccount";
    private static final int MSG_LOGIN_SUCCESS = 0;
    private static final int MSG_LOGIN_FAILED = 1;
    private static final int MSG_LOGIN_RELOGIN = 2;
    private static FileMgrAccount sInstance;
    private boolean mIsKanboxLogined;
    private boolean mIsLogined = false;
    private String appKey_daily = "2cd1fdd3d3f57c090928bca5c077f89f";
    private String appKey = "10d4c8c7af1cd0bfdb6d9ea19aea723c";

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOGIN_SUCCESS:
                    BusProvider.getInstance().post(new FileMgrLoginEvent(MSG_LOGIN_SUCCESS));
                    break;
                case MSG_LOGIN_FAILED:
                    BusProvider.getInstance().post(new FileMgrLoginEvent(MSG_LOGIN_FAILED));
                    break;
                case MSG_LOGIN_RELOGIN:
                    BusProvider.getInstance().post(new FileMgrLoginEvent(MSG_LOGIN_RELOGIN));
                    break;
                default:
                    break;
            }
        }
    };
    static public  FileMgrAccount getsInstance() {
        if (sInstance == null) {
            sInstance = new FileMgrAccount();
        }
        return sInstance;
    }

    //KbxAccountDelegate
    @Override
    public void onSessionInvalid() {
    }

    @Override
    public void onRefreshTokenError(int i) {
        reLogin();
    }

    @Override
    public void onLoginAfterRefreshToken(int i) {
        reLogin();
    }

    @Override
    public void onLogout() {
        mIsKanboxLogined = false;
        mIsLogined = false;
    }

    //--KbxAccountDelegate

    public void init(Context context) {
        mContext = context;
        KbxCloudAccount.getInstance().setDelegate(this);
        mUUID = DeviceUtil.getUUID();
        mClientVersion = DeviceUtil.getAppVersion(mContext);
        mYunOSVersion = DeviceUtil.getYunosVersion();
        mDeviceModel = DeviceUtil.getDeviceModel();
        mDeviceType = DeviceUtil.getDeviceType();
    }

    public boolean isKanboxLogined() {
        return mIsKanboxLogined;
    }

    public boolean isSystemAccountLogined() {
        try {
            int loginStatus = TYIDManager.get(mContext).yunosGetLoginState();
            if(loginStatus == TYIDConstants.EYUNOS_SUCCESS) {
                return true;
            }
            else {
                return false;
            }
        } catch (TYIDException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getHavanaId() {
        String havanaId = "";
        try {
            havanaId = TYIDManager.get(mContext).yunosGetHavanaId();
        }catch (TYIDException e) {
            e.printStackTrace();
        }

        if(havanaId == null)
            havanaId= "";

        return havanaId;
    }

    public void login() {
        FileMgrTransfer.getsInstance();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mAccountId = TYIDManager.get(mContext).yunosGetHavanaId();
                    Bundle bundle = TYIDManager.get(mContext).yunosPeekToken(appKey);
                    int keyCode = bundle.getInt(TYIDConstants.KEY_CODE);
                    if (keyCode == TYIDConstants.EYUNOS_SUCCESS) {
                        mYunOSToken = bundle.getString(TYIDConstants.YUNOS_APP_TOKEN);
                        mYunOSKp = bundle.getString(TYIDConstants.YUNOS_KP);
                    } else if(keyCode == TYIDConstants.EYUNOS_PEEK_TOKEN_NONE) {
                        TYIDManagerFuture<Bundle> accountBundle = TYIDManager.get(mContext).yunosGetToken(appKey, null, null);
                        keyCode = accountBundle.getResult().getInt(TYIDConstants.KEY_CODE);
                        mYunOSToken = accountBundle.getResult().getString(TYIDConstants.YUNOS_APP_TOKEN);
                        mYunOSKp = accountBundle.getResult().getString(TYIDConstants.YUNOS_KP);
                    }

                    if (keyCode != TYIDConstants.EYUNOS_SUCCESS) {
                        if (keyCode == TYIDConstants.EYUNOS_ACCESS_TOKEN_ERROR) {
                            mHandler.sendEmptyMessage(MSG_LOGIN_RELOGIN);
                        } else {
                            mHandler.sendEmptyMessage(MSG_LOGIN_FAILED);
                        }
                        return;
                    }


                    KbxCloudAccount.getInstance().loginByYunOS(mAccountId, mYunOSToken, mYunOSKp, mUUID,
                            mYunOSVersion, mClientVersion, mDeviceType, mDeviceModel,
                            new KbxRequest<KbxLoginByYunOSResponse>(new KbxResponse<KbxLoginByYunOSResponse>() {
                                @Override
                                public void onResponse(KbxLoginByYunOSResponse response) {
                                    if (response.getErrorNo() == 0) {
                                        mIsKanboxLogined = true;
                                        if (!mIsLogined) {
                                            mIsLogined = true;
                                            // 登录后才能清空数据库里面的内容
                                            FileMgrTransfer.getsInstance().init();
                                        }
                                        Log.d(TAG, "login success.");
                                        PreferenceUtil.setString(PreferenceUtil.Setting.SETTTING_LOGIN_ID, mAccountId);
                                        mHandler.sendEmptyMessage(MSG_LOGIN_SUCCESS);
                                    } else {
                                        mIsKanboxLogined = false;
                                        Log.d(TAG, "login failed: " + response.getErrorNo());
                                        mHandler.sendEmptyMessage(MSG_LOGIN_FAILED);
                                    }
                                }
                            }));

                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void reLogin() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mAccountId = TYIDManager.get(mContext).yunosGetHavanaId();
                    TYIDManagerFuture<Bundle> accountBundle = TYIDManager.get(mContext).yunosGetToken(appKey, null, null);
                    int keyCode = accountBundle.getResult().getInt(TYIDConstants.KEY_CODE);
                    mYunOSToken = accountBundle.getResult().getString(TYIDConstants.YUNOS_APP_TOKEN);
                    mYunOSKp = accountBundle.getResult().getString(TYIDConstants.YUNOS_KP);
                    if (keyCode != TYIDConstants.EYUNOS_SUCCESS) {
                        if (keyCode == TYIDConstants.EYUNOS_ACCESS_TOKEN_ERROR) {
                            mHandler.sendEmptyMessage(MSG_LOGIN_RELOGIN);
                        } else {
                            mHandler.sendEmptyMessage(MSG_LOGIN_FAILED);
                        }
                        return;
                    }

                    KbxCloudAccount.getInstance().loginByYunOS(mAccountId, mYunOSToken, mYunOSKp, mUUID,
                            mYunOSVersion, mClientVersion, mDeviceType, mDeviceModel,
                            new KbxRequest<KbxLoginByYunOSResponse>(new KbxResponse<KbxLoginByYunOSResponse>() {
                                @Override
                                public void onResponse(KbxLoginByYunOSResponse response) {
                                    if (response.getErrorNo() == 0) {
                                        mIsKanboxLogined = true;
                                        Log.d(TAG, "login success.");
                                        PreferenceUtil.setString(PreferenceUtil.Setting.SETTTING_LOGIN_ID, mAccountId);
                                    } else {
                                        mIsKanboxLogined = false;
                                        Log.d(TAG, "login failed: " + response.getErrorNo());
                                        mHandler.sendEmptyMessage(MSG_LOGIN_FAILED);
                                    }
                                }
                            }));

                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void cancelTransferProgress() {
        if (FileMgrTransfer.getsInstance().isTransfering()) {
            FileMgrClipboard.getInstance().finishClipboard();
        }

        FileMgrTransfer.getsInstance().cancelDownloadTask();
        FileMgrTransfer.getsInstance().cancelUploadTask();
    }

    public void logout() {
        try {
            KbxCloudAccount.getInstance().logout();
            cancelTransferProgress();
            mIsKanboxLogined = false;
            PreferenceUtil.setString(PreferenceUtil.Setting.SETTTING_LOGIN_ID, "");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
