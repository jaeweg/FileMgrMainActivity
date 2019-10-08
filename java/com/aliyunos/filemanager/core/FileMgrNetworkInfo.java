package com.aliyunos.filemanager.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import com.aliyunos.filemanager.R;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

public class FileMgrNetworkInfo {
    public static interface NetworkInfoListener {
        void onOK();
        void onForbid();
    }
    private static FileMgrNetworkInfo sInstance;
    private boolean mEnableMobileNetwork;
    public static final int NETWORK_TYPE_WIFI = 0x01;
    public static final int NETWORK_TYPE_MOBILE = 0x02;
    public static final int NETWORK_OTHER = 0x03;
    private static final int REQUEST_CONNECT_NETWORK = 101;
    public static final int ERROR_SRC_DEST_SAME_PARENT = 10717;

    private ConnectivityManager mManager;
    public static FileMgrNetworkInfo getsInstance() {
        if (sInstance == null) {
            sInstance = new FileMgrNetworkInfo();
        }

        return sInstance;
    }

    public void init(Context context) {
        mManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean hasNetwork(){
        int networkType = FileMgrNetworkInfo.getsInstance().getNetworkType();
        if (networkType == FileMgrNetworkInfo.NETWORK_OTHER) {
            return false;
        }
        return true;
    }

    public int getNetworkType(){
        NetworkInfo info = mManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()){
            switch (info.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return NETWORK_TYPE_WIFI;
                case ConnectivityManager.TYPE_MOBILE:
                case ConnectivityManager.TYPE_MOBILE_DUN:
                case ConnectivityManager.TYPE_MOBILE_HIPRI:
                case ConnectivityManager.TYPE_MOBILE_MMS:
                case ConnectivityManager.TYPE_MOBILE_SUPL:
                    return NETWORK_TYPE_MOBILE;
                default:
                    return NETWORK_OTHER;
            }
        }else{
            return NETWORK_OTHER;
        }
    }

    public boolean showNetworkIsAvailable(final Context context) {
        int networkType = FileMgrNetworkInfo.getsInstance().getNetworkType();
        if (networkType == FileMgrNetworkInfo.NETWORK_OTHER) {
            if (context != null && !((Activity)context).isFinishing()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setPositiveButton(R.string.setting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {

                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_WIRELESS_SETTINGS);

                        ((Activity) context).startActivityForResult(intent, REQUEST_CONNECT_NETWORK);
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
            return false;
        }
        return true;
    }

    public void showNetworkIsAvailable(final Context context, boolean isUpload, final NetworkInfoListener listener) {
        int networkType = FileMgrNetworkInfo.getsInstance().getNetworkType();
        if (networkType == FileMgrNetworkInfo.NETWORK_OTHER) {
            if (context != null && !((Activity)context).isFinishing()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setPositiveButton(R.string.setting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {

                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_WIRELESS_SETTINGS);

                        ((Activity) context).startActivityForResult(intent, REQUEST_CONNECT_NETWORK);
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
            listener.onForbid();
            return;
        } else if (networkType == FileMgrNetworkInfo.NETWORK_TYPE_MOBILE) {
            mEnableMobileNetwork = false;
            if (context != null && !((Activity)context).isFinishing()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        listener.onOK();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        listener.onForbid();
                    }
                });
                if (isUpload) {
                    builder.setMessage(R.string.upload_tips);
                } else {
                    builder.setMessage(R.string.download_tips);
                }
                AlertDialog dd = builder.create();
                dd.show();
            }
            return;
        }
        listener.onOK();
    }
}
