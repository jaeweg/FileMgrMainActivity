package com.aliyunos.filemanager.core;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aliyunos.filemanager.provider.BusProvider;
import com.kanbox.filemgr.KbxFileChangeEvent;
import com.kanbox.filemgr.KbxFileDelegate;
import com.kanbox.filemgr.KbxLocalFileManager;

public class FileMgrWatcher implements KbxFileDelegate {
    private static FileMgrWatcher sInstance;
    private static final String TAG = "FileMgrWatcher";
    private static final int MSG_FILE_CHANGE = 0;
    private static final int MSG_CATEGORY_CHANGE = 1;
    private static final int MSG_ROOTDIR_CHANGE = 2;
    private static final int MSG_DOWNLOAD_FILE_DELETE = 3;
    private boolean  isNeedRefreshCategory = true;
    private boolean  isNeedRefreshLocal = false;
    private FileMgrFileEvent localFileEvent;


    private Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_FILE_CHANGE:
                    isNeedRefreshLocal = true;
                    FileMgrFileEvent fileEvent = (FileMgrFileEvent)msg.obj;
                    BusProvider.getInstance().post(fileEvent);
                    localFileEvent = fileEvent;
                    break;
                case MSG_CATEGORY_CHANGE:
                    isNeedRefreshCategory = true;
                    FileMgrCore.resetDataAfterScan();
                    FileMgrCategoryEvent categoryEvent = (FileMgrCategoryEvent)msg.obj;
                    BusProvider.getInstance().post(categoryEvent);
                    break;
                case MSG_ROOTDIR_CHANGE:
                    BusProvider.getInstance().post(new FileMgrFileEvent());
                    break;
                case MSG_DOWNLOAD_FILE_DELETE:
                    FileMgrCloudEvent cloudEvent = (FileMgrCloudEvent)msg.obj;
                    BusProvider.getInstance().post(cloudEvent);
                default:
                    break;
            }
        }
    };

    public static FileMgrWatcher getInstance() {
        if (sInstance == null) {
            sInstance = new FileMgrWatcher();
        }
        return sInstance;
    }

    void init() {
        KbxLocalFileManager.setDelegate(this);
    }

    public void startWatcher() {
        KbxLocalFileManager.startMonitor();
    }

    public void stopWatcher() {
        KbxLocalFileManager.stopMonitor();
    }


    @Override
    public void onRootDirChange() {
        mHandle.sendEmptyMessage(MSG_ROOTDIR_CHANGE);
    }

    @Override
    public void onFileChange(KbxFileChangeEvent[] eventList) {
        Log.d(TAG, "onFileChange");
        FileMgrFileEvent event = new FileMgrFileEvent(eventList);
        Message msg = new Message();
        msg.what = MSG_FILE_CHANGE;
        msg.obj = event;
        mHandle.sendMessage(msg);
    }

    @Override
    public void onCategoryFileChange() {
        Log.d(TAG, "onCategoryFileChange");
        FileMgrCategoryEvent categoryEvent = new FileMgrCategoryEvent();
        Message msg = new Message();
        msg.what = MSG_CATEGORY_CHANGE;
        msg.obj = categoryEvent;
        mHandle.sendMessage(msg);
    }

    @Override
    public void onDownloadFileDeleted(String[] files) {
        Log.d(TAG, "onDownloadFileDeleted");
        FileMgrCloudEvent cloudEvent = new FileMgrCloudEvent(FileMgrCloudEvent.CLOUD_EVENT_DOWNLOAD_FILE_DELETE, files);
        Message msg = new Message();
        msg.what = MSG_DOWNLOAD_FILE_DELETE;
        msg.obj = cloudEvent;
        mHandle.sendMessage(msg);
    }

    public  boolean isNeedRefreshCategory () {
        return isNeedRefreshCategory;
    }

    public  void setNeedRefreshCattegory(boolean flag) {
        isNeedRefreshCategory = flag;
    }

    public  boolean isNeedRefreshLocal () {
        return isNeedRefreshLocal;
    }

    public  void setNeedRefreshLocal(boolean flag) {
        isNeedRefreshLocal = flag;
    }

    public FileMgrFileEvent getLocalFileEvent () {
        return localFileEvent;
    }
}
