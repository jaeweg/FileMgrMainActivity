package com.aliyunos.filemanager.core;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.aliyunos.filemanager.FileMgrApplication;
import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.view.dialog.ProgressDialog;
import com.aliyunos.filemanager.util.PreferenceUtil;
import com.kanbox.filemgr.KbxCategoryFileInfo;
import com.kanbox.filemgr.KbxFileProgressListener;
import com.kanbox.filemgr.KbxGetFileListListener;
import com.kanbox.filemgr.KbxGetLocalThumbnailListener;
import com.kanbox.filemgr.KbxLocalFile;
import com.kanbox.filemgr.KbxLocalFileManager;
import com.kanbox.filemgr.KbxLocalThumbnailManager;
import com.kanbox.filemgr.KbxScanFileListener;
import com.kanbox.filemgr.KbxStorageCapacity;
import com.kanbox.sdk.transfer.KbxThumbnailDelegate;
import com.kanbox.sdk.transfer.KbxThumbnailManager;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.aliyunos.filemanager.ui.view.dialog.MessageDialog;
import com.aliyunos.filemanager.core.StorageInfo;

import android.annotation.SuppressLint;
import android.app.Activity;

public class FileMgrCore {
    private final static String TAG = "FileMgrCore";
    private final static int MSG_SCAN_COMPLETE = 1;
    private final static int MSG_GET_THUMBNAIL = 2;
    private final static int MSG_CATEGORY_CHANGE = 3;
    private final static int MSG_REFRESH_LOCAL_FILELIST = 4;
    private final static int MSG_PROGRESS_CHANGE = 5;

    private final static int THUMBNAIL_CACHE_SIZE = 4 * 1024 * 1024; //4MB

    private static KbxStorageCapacity mStorageCapacity;
    private static KbxCategoryFileInfo[] mCategoryFileInfoList =
            new KbxCategoryFileInfo[KbxLocalFileManager.CategoryType.values().length];

    private static boolean mIsScanning = false;
    private static boolean mIsScanned = false;
    private static boolean mIsFetchLocalFileList = false;
    private static UIHandler mUIHandler;

    public static boolean isScanning() { return mIsScanning; }
    public static boolean isScanned() { return mIsScanned; }

    private static WeakReference<ProgressDialog> mProgressDialog;
    private static WeakReference<Context> mContext;
    private static boolean mIgnoreFileWatch = false;
    private static HashMap<String,WeakReference<KbxGetLocalThumbnailListener>> hashMap_thumbnailListener
            = new HashMap<String, WeakReference<KbxGetLocalThumbnailListener>>();

    private static WeakReference<GetLocalFileListListener> mGetLocalFileListListener = null;
    private static String  mMediaFolderPath = "";

    private static MessageDialog sLoadDialog;

    private static void showLoadDialog(int resId) {
        if (mContext == null) {
            return;
        }
        Context context = mContext.get();
        if (context == null || (context instanceof Activity && (((Activity)context).isFinishing()))) {
            return;
        }
        if (sLoadDialog == null) {
            sLoadDialog = new MessageDialog(context);
        }
        sLoadDialog.setMessage(context.getString(resId));
        sLoadDialog.show();
    }

    private static void cancelLoadDialog() {
        if (sLoadDialog != null) {
            sLoadDialog.dismiss();
            sLoadDialog = null;
        }
    }


    public static boolean isIgnoreFileWatch() { return mIgnoreFileWatch; }
    public static void setIgnoreFileWatch(boolean igored) {
        mIgnoreFileWatch = igored;
        if(!igored) {
            BusProvider.getInstance().post(new FileMgrLocalEvent());
            FileMgrClipboard.getInstance().finishClipboard();
        }
    }

    public static boolean isProgressShowing() {
        if (mProgressDialog != null && mProgressDialog.get() != null) {
            return (mProgressDialog != null && mProgressDialog.get().isShowing());
        }
        return false;
    }

    public static void stopScan() {
        if (!mIsScanned && mIsScanning) {
            Log.d(TAG, "stopScan");
            KbxLocalFileManager.stopScan();
            mIsScanning = false;
        }
    }

    public static interface GetLocalFileListListener {
        void onGetFileList(KbxLocalFile[] fileList);
    }
    static class GetThumbnailMessage {
        String path;
        Bitmap bitmap;
        GetThumbnailMessage(String path, Bitmap bitmap) {
            this.path = path;
            this.bitmap = bitmap;
        }
    }

    static class GetProgressMessage {
        int error;
        boolean cancelled;
        int total_count;
        boolean current_finished;
        String path;
        double current_progress;

        GetProgressMessage(int error, boolean cancelled, int total_count,
                           boolean current_finished, String path, double current_progress) {
            this.error = error;
            this.cancelled = cancelled;
            this.total_count = total_count;
            this.current_finished = current_finished;
            this.path = path;
            this.current_progress = current_progress;
        }
    }

    static class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SCAN_COMPLETE:
                    mIsScanning = false;
                    mIsScanned = true;
                    resetDataAfterScan();
                    BusProvider.getInstance().post(new FileMgrScanEvent());
                    break;
                case MSG_REFRESH_LOCAL_FILELIST:
                    BusProvider.getInstance().post(new FileMgrLocalEvent());
                    break;
                case MSG_GET_THUMBNAIL:
                    GetThumbnailMessage thumbnailMessage = (GetThumbnailMessage)msg.obj;
                    WeakReference<KbxGetLocalThumbnailListener> listener = hashMap_thumbnailListener.get(thumbnailMessage.path);
                    if (listener != null && listener.get() != null) {
                        listener.get().onGetThumbnail(
                                thumbnailMessage.path, thumbnailMessage.bitmap);
                    }
                    hashMap_thumbnailListener.remove(thumbnailMessage.path);
                    break;
                case MSG_PROGRESS_CHANGE:
                    GetProgressMessage progressMessage = (GetProgressMessage)msg.obj;

                    if (progressMessage.cancelled) {
                    	Log.i("huangjiawei", "1111111111111111");
                        notifyFolderChange();
                        if (mProgressDialog != null && mProgressDialog.get() != null) {
                            mProgressDialog.get().dismiss();
                            mProgressDialog = null;
                        }
                        mIgnoreFileWatch = false;

                        BusProvider.getInstance().post(new FileMgrLocalEvent());
                        FileMgrClipboard.getInstance().finishClipboard();
                        mContext = null;
                    }
                    else if (progressMessage.error != 0) {
                    	Log.i("huangjiawei", "22222222222222");
                        notifyFolderChange();
                        if (mProgressDialog != null && mProgressDialog.get() != null) {
                            mProgressDialog.get().dismiss();
                            mProgressDialog = null;
                        }
                        mIgnoreFileWatch = false;
                        if (progressMessage.error == 28 || progressMessage.error == 20
                                || progressMessage.error == 122 || progressMessage.error == 2) {
                            toast(R.string.ali_NotEnoughSpace);
                        }
                        BusProvider.getInstance().post(new FileMgrLocalEvent());
                        FileMgrClipboard.getInstance().finishClipboard();
                        mContext = null;
                    }
                    else {
                        if (mProgressDialog != null && mProgressDialog.get() != null) {
                        	Log.i("huangjiawei", "33333333333333");
                            mProgressDialog.get().setTaskCount(progressMessage.total_count);
                            mProgressDialog.get().setProgress(progressMessage.current_progress);
                            mProgressDialog.get().setMessage(progressMessage.path);
                            if (progressMessage.current_finished) {
                                mProgressDialog.get().finishCurrentTask();
                                if (mProgressDialog.get().isFinished()) {
                                	Log.i("huangjiawei", "0000000000000");
                                    notifyFolderChange();
                                    mProgressDialog.get().dismiss();
                                    mProgressDialog = null;
                                    mIgnoreFileWatch = false;

                                    BusProvider.getInstance().post(new FileMgrLocalEvent());
                                    FileMgrClipboard.getInstance().finishClipboard();
                                    mContext = null;
                                }
                            }
                        } else {
                        	Log.i("huangjiawei", "4444444444444444");
                            if (progressMessage.current_finished) {
                            	Log.i("huangjiawei", "5555555555555");
                                mIgnoreFileWatch = false;
                                BusProvider.getInstance().post(new FileMgrLocalEvent());
                                FileMgrClipboard.getInstance().finishClipboard();
                                mContext = null;
                            }
                        }
                    }
                    break;
            }
        }
    }

    public static void init() {
        mUIHandler = new UIHandler();
        KbxLocalThumbnailManager.getInstance().initCache(THUMBNAIL_CACHE_SIZE);
        KbxThumbnailManager.getInstance().initCache(THUMBNAIL_CACHE_SIZE);
        FileMgrWatcher.getInstance().init();
        FileMgrAccount.getsInstance().init(FileMgrApplication.getInstance());
        FileMgrNetworkInfo.getsInstance().init(FileMgrApplication.getInstance());
        FileMgrTransfer.getsInstance();

    }

    public static void toast(final String str)
    {
        mUIHandler.post(new Runnable(){
            @Override
            public void run() {
                Toast.makeText(FileMgrApplication.getInstance().getApplicationContext(),str,Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void toast(final int str)
    {
        mUIHandler.post(new Runnable(){
            @Override
            public void run() {
                Toast.makeText(FileMgrApplication.getInstance().getApplicationContext(), str, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static void doFirstScan(boolean isCategoryFile) {
        if ((!mIsScanning && !mIsScanned) || isCategoryFile) {
            doScan();
        }
    }

    private static void doScan() {
        if (mIsScanning) return;

        mIsScanning = true;
        mIsScanned = false;
        KbxLocalFileManager.scanAllStorageVolume(new KbxScanFileListener() {
            @Override
            public void onCompleted() {
                mUIHandler.sendEmptyMessage(MSG_SCAN_COMPLETE);
            }
        });
    }
    
    /*
     begin add by hjw at 2016/9/9
     **/
    public static class MyMemoryInfo 
    {
        public long total;

        public long free;
    } 
  
    /** 
     * 获得指定路径内存总大小 和可用内存的MyMemoryInfo。
     *  
     * @return 
     */  
    public static MyMemoryInfo getInfoTotalSize(String path) {  
        try {
        StatFs stat = new StatFs(path);  
        long blockSize = stat.getBlockSize();  
        long totalBlocks = stat.getBlockCount();  
        long availableBlocks = stat.getAvailableBlocks();
        MyMemoryInfo info = new MyMemoryInfo();
        
        info.total = blockSize * totalBlocks;
        info.free = blockSize * availableBlocks;
        
        return info;
        } catch (IllegalArgumentException e) {
            Log.e("FileMgrCore", e.toString());
        }
        return null;
    }  
    
    /*
     *end add by hjw at 2016/9/9
     **/
    
    /*
     *add by huangjiawei at 2016/9/27 begin 
     *考虑除SD卡外OTG等其他外插内存设备
     */
    
    @SuppressLint("NewApi") 
    public static List<StorageInfo> listAllStorage(Context context) {  
 	    ArrayList<StorageInfo> storages = new ArrayList<StorageInfo>();  
 	    StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);  
 	    try {  
 	        Class<?>[] paramClasses = {};  
 	        Method getVolumeList = StorageManager.class.getMethod("getVolumeList", paramClasses);  
 	        Object[] params = {};  
 	        Object[] invokes = (Object[]) getVolumeList.invoke(storageManager, params);  
 	          
 	        if (invokes != null) {  
 	            StorageInfo info = null;  
 	            for (int i = 0; i < invokes.length; i++) {  
 	                Object obj = invokes[i];  
 	                Method getPath = obj.getClass().getMethod("getPath", new Class[0]);  
 	                String path = (String) getPath.invoke(obj, new Object[0]);  
 	                info = new StorageInfo(path);  
 	  
 	                Method getVolumeState = StorageManager.class.getMethod("getVolumeState", String.class);  
 	                String state = (String) getVolumeState.invoke(storageManager, info.path);  
 	                info.state = state;  
 	  
 	                Method isRemovable = obj.getClass().getMethod("isRemovable", new Class[0]);  
 	                info.isRemoveable = ((Boolean) isRemovable.invoke(obj, new Object[0])).booleanValue();  
 	                storages.add(info);  
 	            }  
 	        }  
 	    } catch (Exception e) {  
 	        e.printStackTrace();  
 	    }  
 	    storages.trimToSize();  
 	    return storages;  
 	}  
 	  
 	public static List<StorageInfo> getAvaliableStorage(List<StorageInfo> infos){  
 	    List<StorageInfo> storages = new ArrayList<StorageInfo>();  
 	    for(StorageInfo info : infos){  
 	        File file = new File(info.path);  
 	        if ((file.exists()) && (file.isDirectory()) && (file.canWrite())) {  
 	            if (info.isMounted()) {  
 	                storages.add(info);  
 	            }  
 	        }  
 	    }  
 	      Log.d(TAG, "getAvaliableStorage -- storages.lenght == "+storages.size());
 	    return storages;  
 	}
    
 	public static long[] getDoovTotalCapacity(Context context){
		long[] tempTotal = new long[3];
		List<StorageInfo> list = listAllStorage(context);  
        List<StorageInfo> infos = getAvaliableStorage(list);  
        long total = 0;
        long romTotal = 0;
        // 遍历存储设备
        for(StorageInfo info : infos){  
            if (!info.getIsRemoveable()) {
            	// 判断如果设备是不可移除的就是系统内存了，得到系统内存总量。
				romTotal = getInfoTotalSize(info.getPath()).total;
			}
            long everyInfo = getInfoTotalSize(info.getPath()).total;
            total += everyInfo;
            Log.d(TAG, "getDoovTotalCapacity --"+info.toString());  
            Log.d(TAG, "getDoovTotalCapacity -- everyInfo :"+everyInfo);
            Log.d(TAG, "getDoovTotalCapacity -- total == "+total);
        }
        
        if (infos.size() == 1) {
			tempTotal[2] = 1;
		}else {
			tempTotal[2] = 0;
		}
        tempTotal[0] = romTotal;
        tempTotal[1] = total;
        return tempTotal;
	}
    /*
     *add by huangjiawei at 2016/9/27 end 
     */

    public static long getTotalCapacity() {
        if (mStorageCapacity == null) {
            mStorageCapacity = KbxLocalFileManager.getStorageCapacityInfo();
        }
        return mStorageCapacity.getTotalCapacity();
    }

    public static long getAvailableCapacity() {
        if (mStorageCapacity == null) {
            mStorageCapacity = KbxLocalFileManager.getStorageCapacityInfo();
        }
        return mStorageCapacity.getAvailableCapacity();
    }

    public static long getOtherTypeCapacity() {
        long categoryCapacity = 0;
        for (KbxLocalFileManager.CategoryType type:KbxLocalFileManager.CategoryType.values()) {
            categoryCapacity += getCategoryTypeSize(type);
        }
        return getTotalCapacity() - getAvailableCapacity() - categoryCapacity;
    }

    public static KbxLocalFile[] getCategoryFileList(KbxLocalFileManager.CategoryType categoryType) {
        long curTime = System.currentTimeMillis();
        KbxLocalFile[]  list = KbxLocalFileManager.getCategoryFileList(categoryType);
        Log.d("FileMgrCore", "getCategoryFileList consumeTime " + (System.currentTimeMillis()- curTime));
        Log.d("FileMgrCore", "getCategoryFileList categoryType size: " + list.length);
        return list;
    }

    public static int getCategoryFileCount(KbxLocalFileManager.CategoryType categoryType) {
        long curTime = System.currentTimeMillis();
        if (mCategoryFileInfoList[categoryType.ordinal()] == null) {
            mCategoryFileInfoList[categoryType.ordinal()] =
                    KbxLocalFileManager.getCategoryFileInfo(categoryType);
        }
        int nCount = mCategoryFileInfoList[categoryType.ordinal()].getFileCount();
        int nHideCount = mCategoryFileInfoList[categoryType.ordinal()].getHideFileCount();
        Log.d("FileMgrCore", "getCategoryFileCount consumeTime " + (System.currentTimeMillis()- curTime));
        boolean showHidden = PreferenceUtil.getBoolean(PreferenceUtil.Setting.SETTINGS_SHOW_HIDDEN);
        return showHidden ? nCount : (nCount - nHideCount);
    }

    public static long getCategoryTypeSize(KbxLocalFileManager.CategoryType categoryType) {
        if (mCategoryFileInfoList[categoryType.ordinal()] == null) {
            mCategoryFileInfoList[categoryType.ordinal()] =
                    KbxLocalFileManager.getCategoryFileInfo(categoryType);
        }
        return mCategoryFileInfoList[categoryType.ordinal()].getCapacitySize();
    }

    public static KbxLocalFile[] getRootFolderList() {
        return KbxLocalFileManager.getRootFolderList();
    }

    public static void getFileList(final String path, GetLocalFileListListener listener) {
        mGetLocalFileListListener = new WeakReference<GetLocalFileListListener>(listener);
        if (listener == null) {
            return;
        }
        if (mIsFetchLocalFileList) {
            return;
        }
        mIsFetchLocalFileList = true;
        KbxLocalFileManager.getFileListAsync(path, new KbxGetFileListListener() {
            @Override
            public void onGetFileList(final KbxLocalFile[] fileList) {
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mGetLocalFileListListener != null && mGetLocalFileListListener.get() != null) {
                            mGetLocalFileListListener.get().onGetFileList(fileList);
                        }
                        mIsFetchLocalFileList = false;
                        mGetLocalFileListListener = null;
                    }
                });
            }
        });
    }

    public static void newFolder(String path, String folderName, Context context) {
        mContext = new WeakReference<Context>(context);
        int error = KbxLocalFileManager.newFolder(path, folderName);
        if (error == 17) {
            toast(R.string.ali_dirIsExist);
        }
        else if (error == 2) {
            toast(R.string.ali_dirNameEmtpy);
        }
        else if (error == 28) {
            toast(R.string.ali_NotEnoughSpace);
        }
        else if (error != 0){
            toast(R.string.new_folder_failed);
        } else {
            notifyFolderChange(path);
        }
    }

    public static void renameFile(String filePath, String oldName, String newName, Context context) {
        File file = new File(filePath);
        if(file !=null && file.isDirectory()) {
            mMediaFolderPath = file.getParentFile().getPath();
        }

        mContext = new WeakReference<Context>(context);
        int error = KbxLocalFileManager.renameFile(filePath, oldName, newName);
        if (error == 17) {
            toast(R.string.RenameFileExist);
        } else if (error == 28) {
            toast(R.string.ali_NotEnoughSpace);
        } else if (error != 0) {
            toast(R.string.rename_failed);
        } else {
            notifyMediaFileChange(filePath);
            notifyMediaFileChange(filePath + "/"  + newName);
            notifyFolderChange(filePath);
        }
    }

    static void resetDataAfterScan() {
        mStorageCapacity = null;
        mCategoryFileInfoList = new KbxCategoryFileInfo[KbxLocalFileManager.CategoryType.values().length];
    }

    static Bitmap getThumbnailCache(String filePath) {
        return KbxLocalThumbnailManager.getInstance().getThumbnailFromCache(filePath);
    }

    static void getThumbnail(String filePath, int width, int height, KbxGetLocalThumbnailListener listener) {
        hashMap_thumbnailListener.put(filePath, new WeakReference<KbxGetLocalThumbnailListener>(listener));
        KbxLocalThumbnailManager.getInstance().getThumbnailFromFile(
                filePath, width, height, new KbxGetLocalThumbnailListener() {
            @Override
            public void onGetThumbnail(String path, Bitmap bitmap) {
                GetThumbnailMessage thumbnailMessage = new GetThumbnailMessage(path, bitmap);
                mUIHandler.sendMessage(mUIHandler.obtainMessage(MSG_GET_THUMBNAIL, thumbnailMessage));
            }
        });
    }

    static Bitmap getCloudThumbnailCache(String filePath, String fileId, KbxThumbnailManager.ZoomType zoomType) {
        // 根据filePath获取文件类型
        KbxLocalFile.FileType fileType = KbxLocalFileManager.getFileType(filePath);
        boolean isVideo = false;
        if (!(fileType == KbxLocalFile.FileType.Image || fileType == KbxLocalFile.FileType.Video)) {
            return null;
        }
        if (fileType == KbxLocalFile.FileType.Video) {
            isVideo = true;
        }
        return KbxThumbnailManager.getInstance().getThumbnailFromCache(filePath, fileId, zoomType, isVideo);
    }

    static void getCloudThumbnail(final String filePath, String fileId, KbxThumbnailManager.ZoomType zoomType,
                                         KbxGetLocalThumbnailListener listener) {
        // 根据filePath获取文件类型
        hashMap_thumbnailListener.put(filePath, new WeakReference<KbxGetLocalThumbnailListener>(listener));
        KbxLocalFile.FileType fileType = KbxLocalFileManager.getFileType(filePath);
        boolean isVideo = false;
        if (!(fileType == KbxLocalFile.FileType.Image || fileType == KbxLocalFile.FileType.Video)) {
            return;
        }
        if (fileType == KbxLocalFile.FileType.Video) {
            isVideo = true;
        }
        KbxThumbnailManager.getInstance().getThumbnail(filePath, fileId, zoomType, isVideo,
                new KbxThumbnailDelegate() {
                    @Override
                    public void onComplete(String fileId, KbxThumbnailManager.ZoomType zoomType, boolean isVideo, Bitmap bitmap) {
                        GetThumbnailMessage thumbnailMessage = new GetThumbnailMessage(filePath, bitmap);
                        mUIHandler.sendMessage(mUIHandler.obtainMessage(MSG_GET_THUMBNAIL, thumbnailMessage));
                    }

                    @Override
                    public void onError(int errorCode) {
                        Log.d(TAG, "getCloudThumbnail Error: " + errorCode);
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "getCloudThumbnail OnCancel");
                    }
                });
    }

    public static KbxLocalFile.FileType getFileTypeByPath(String path) {
        return KbxLocalFileManager.getFileType(path);
    }

    private static void showProgressDialog(String message, int task_count) {
        if (mContext != null && mContext.get() != null) {
            mProgressDialog = new WeakReference<ProgressDialog>(new ProgressDialog(mContext.get(), message));
            mProgressDialog.get().setMessage("");
            mProgressDialog.get().setTaskCount(task_count);
            mProgressDialog.get().setCancelDelegate(new ProgressDialog.ProgressDialogDelegate() {
                @Override
                public void onCancel() {
                    cancel();
                    FileMgrClipboard.getInstance().finishClipboard();
                }
            });
            mProgressDialog.get().show();
            mProgressDialog.get().finishCurrentTask();
        }
    }

    public static void copyFiles(KbxLocalFile[] fileInfos, String filePath, Context context) {
        File file = new File(filePath);
        if(file !=null && file.isDirectory()) {
            mMediaFolderPath = filePath;
        }

        mContext = new WeakReference<Context>(context);
        mIgnoreFileWatch = true;
        showProgressDialog(context.getString(R.string.dialog_title_do_copy), fileInfos.length);
        if(mProgressDialog != null && mProgressDialog.get() != null) {
            mProgressDialog.get().setMessage(fileInfos[0].getFilePath());
        }
        KbxLocalFileManager.copyFiles(fileInfos, filePath, new KbxFileProgressListener() {
            @Override
            public void onProgress(int error, boolean cancelled, int total_count,
                                   boolean current_finished, String path, double current_progress) {
                GetProgressMessage progressMessage = new GetProgressMessage(
                        error, cancelled, total_count, current_finished, path, current_progress);

                mUIHandler.sendMessage(mUIHandler.obtainMessage(MSG_PROGRESS_CHANGE, progressMessage));
            }
        });
    }

    public static void deleteFiles(final KbxLocalFile[] fileInfos, Context context) {
        for(KbxLocalFile localFile :fileInfos) {
            if(localFile.getIsFolder()) {
                File file = new File(localFile.getFilePath());
                mMediaFolderPath = file.getParentFile().getPath();
                break;
            }
        }

        mContext = new WeakReference<Context>(context);
        mIgnoreFileWatch = true;
        if (fileInfos.length == 1) {
            showLoadDialog(R.string.dialog_title_do_delete);
        } else {
            showProgressDialog(context.getString(R.string.dialog_title_do_delete), fileInfos.length);
        }
        KbxLocalFileManager.deleteFiles(fileInfos, new KbxFileProgressListener() {
            @Override
            public void onProgress(int error, boolean cancelled, int total_count,
                                   boolean current_finished, String path, double current_progress) {
                GetProgressMessage progressMessage = new GetProgressMessage(
                        error, cancelled, total_count, current_finished, path, current_progress);

                Log.i("huangjiawei", "FileMgrCore.deleteFile.path == " + path);
                Log.i("huangjiawei", "FileMgrCore.deleteFile.current_finished == " + current_finished);
                Log.i("huangjiawei", "FileMgrCore.deleteFile.error == " + error);
                Log.i("huangjiawei", "FileMgrCore.deleteFile.fileInfos.length == " + fileInfos.length);
                if (fileInfos.length != 1) {
                    mUIHandler.sendMessage(mUIHandler.obtainMessage(MSG_PROGRESS_CHANGE, progressMessage));
                }
                if (current_finished) {
                    if (fileInfos.length == 1) {
                        cancelLoadDialog();
                        notifyFolderChange();
                        mIgnoreFileWatch = false;
                        mContext = null;
                    }
                    notifyMediaFileChange(path);
                }
            }
        });
    }

    public static void moveFiles(KbxLocalFile[] fileInfos, final String filePath, Context context) {
        File file = new File(filePath);
        if(file !=null && file.isDirectory()) {
            mMediaFolderPath = filePath;
        }

        mContext = new WeakReference<Context>(context);
        mIgnoreFileWatch = true;
        showProgressDialog(context.getString(R.string.dialog_title_do_cut), fileInfos.length);
        if(mProgressDialog != null && mProgressDialog.get() != null) {
            mProgressDialog.get().setMessage(fileInfos[0].getFilePath());
        }
        KbxLocalFileManager.moveFiles(fileInfos, filePath, new KbxFileProgressListener() {
            @Override
            public void onProgress(int error, boolean cancelled, int total_count,
                                   boolean current_finished, String path, double current_progress) {
                GetProgressMessage progressMessage = new GetProgressMessage(
                        error, cancelled, total_count, current_finished, path, current_progress);

                mUIHandler.sendMessage(mUIHandler.obtainMessage(MSG_PROGRESS_CHANGE, progressMessage));

                if (current_finished) {
                    notifyMediaFileChange(path);
                    File file = new File(path);
                    notifyMediaFileChange(filePath + file.getName());
                }
            }
        });
    }

    public static void crushFile(String filePath, Context context) {
        File file = new File(filePath);
        if(file !=null && file.isDirectory()) {
            mMediaFolderPath = file.getParentFile().getPath();
        }

        mContext = new WeakReference<Context>(context);
        mIgnoreFileWatch = true;
        //showProgressDialog(context.getString(R.string.dialog_title_do_crush_delete), 1);
        showLoadDialog(R.string.dialog_title_do_crush_delete);
        KbxLocalFileManager.crushFile(filePath, new KbxFileProgressListener() {
            @Override
            public void onProgress(int error, boolean cancelled, int total_count,
                                   boolean current_finished, String path, double current_progress) {
                GetProgressMessage progressMessage = new GetProgressMessage(
                        error, cancelled, total_count, current_finished, path, current_progress);

                //mUIHandler.sendMessage(mUIHandler.obtainMessage(MSG_PROGRESS_CHANGE, progressMessage));

                if (current_finished) {
                    cancelLoadDialog();
                    notifyFolderChange();
                    mIgnoreFileWatch = false;
                    mContext = null;

                    notifyMediaFileChange(path);
                }
            }
        });
    }

    public static  void cancel() {
        KbxLocalFileManager.cancel();
    }

    private final static String BroadcastActionScan = "com.aliyunos.filemanager.ActionScan";
    public final static String DeleteByFileManager = "DeleteByFileManager";

    public static void notifyMediaFileChange(String filePath) {
        KbxLocalFile.FileType fileType = KbxLocalFileManager.getFileType(filePath);
        if (fileType == KbxLocalFile.FileType.Image ||
                fileType == KbxLocalFile.FileType.Video ||
                fileType == KbxLocalFile.FileType.Audio ||
                fileType == KbxLocalFile.FileType.Apk   ||
                fileType == KbxLocalFile.FileType.Zip) {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(new File(filePath)));
            FileMgrApplication.getInstance().sendBroadcast(scanIntent);
        }
    }

    public static void notifyFolderChange() {
        if(!mMediaFolderPath.isEmpty()) {
            Uri localUri = Uri.fromFile(new File(mMediaFolderPath));
            Intent localIntent = new Intent(BroadcastActionScan, localUri );
            localIntent.putExtra(DeleteByFileManager, false);
            FileMgrApplication.getInstance().sendBroadcast(localIntent);

            MediaScannerConnection.scanFile(FileMgrApplication.getInstance(), new String[] { mMediaFolderPath }, null,null);
        }

        mMediaFolderPath = "";
    }

    public static  void notifyFolderChange(String path) {
        if(!path.isEmpty()) {
            Uri localUri = Uri.fromFile(new File(path));
            Intent localIntent = new Intent(BroadcastActionScan, localUri);
            localIntent.putExtra(DeleteByFileManager, false);
            FileMgrApplication.getInstance().sendBroadcast(localIntent);

            MediaScannerConnection.scanFile(FileMgrApplication.getInstance(), new String[]{path}, null, null);
        }
    }

}
