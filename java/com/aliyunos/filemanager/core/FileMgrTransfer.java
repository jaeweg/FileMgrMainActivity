package com.aliyunos.filemanager.core;


import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.cloud.CloudFileInfo;
import com.aliyunos.filemanager.ui.local.LocalFileInfo;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.kanbox.filemgr.KbxLocalFile;
import com.kanbox.filemgr.KbxLocalFileManager;
import com.kanbox.sdk.common.KbxDefaultResponse;
import com.kanbox.sdk.common.KbxRequest;
import com.kanbox.sdk.common.KbxResponse;
import com.kanbox.sdk.filelist.KbxFileManager;
import com.kanbox.sdk.filelist.model.KbxFile;
import com.kanbox.sdk.filelist.response.KbxUpdateFileListResponse;
import com.kanbox.sdk.transfer.KbxDownloadTask;
import com.kanbox.sdk.transfer.KbxTransferDelegate;
import com.kanbox.sdk.transfer.KbxTransferTask;
import com.kanbox.sdk.transfer.KbxUploadFileTask;
import com.kanbox.sdk.transfer.KbxUploadTask;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FileMgrTransfer {

    private final int MSG_ON_PROGRESS = 0;
    private final int MSG_ON_COMPLETE = 1;
    private final int MSG_ON_CANCEL = 2;
    private final int MSG_ON_ERROR = 3;
    private final int MSG_ON_CHANGE_TASK = 4;
    private final int MSG_ON_TASK_COUNT_CHANGE = 5;

    private final int MSG_UPLOAD_COMPLETE = 6;
    private final int MSG_DOWNLOAD_COMPLETE = 7;

    class TransferThread extends Thread {
        {
            start(); // 类加载完成后直接启动
        }
        private Handler handler;

        @Override
        public void run() {
            while (true) {

                Looper.prepare(); // 创建该线程的Looper对象
                handler = new Handler(Looper.myLooper()) {
                    public void handleMessage(android.os.Message msg) {
                        Log.i("handleMessage", "" + msg.what);
                    };
                };

                Looper.loop(); // 这里是一个死循环
                // 此后的代码无法执行
            }
        }
    }

    private TransferThread mTransferThread = new TransferThread();

    public void postToNoneUIThread(Runnable r) {
        // 执行到这里的时候，子线程可能尚未启动，等待子线程启动，等待的时间会很短，
        while (mTransferThread.handler == null) {
        }
        mTransferThread.handler.post(r);
        mTransferThread.handler.sendEmptyMessage(100);
    }

    class DelegateParam {
        DelegateParam(FileMgrTransferDelegate delegate) {
            this.delegate = delegate;
        }

        DelegateParam(FileMgrTransferDelegate delegate, Object param) {
            this.delegate = delegate;
            this.param = param;
        }

        FileMgrTransferDelegate delegate;
        Object param;
    }

    private Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_ON_PROGRESS: {
                    DelegateParam obj = (DelegateParam) msg.obj;
                    FileMgrTransferDelegate delegate = obj.delegate;
                    if (delegate == null) {
                        return;
                    }
                    Double progress = (Double) obj.param;
                    if (progress != null) {
                        delegate.onProgress(progress.doubleValue());
                    }
                }
                    break;
                case MSG_ON_COMPLETE: {
                    DelegateParam obj = (DelegateParam) msg.obj;
                    FileMgrTransferDelegate delegate = obj.delegate;
                    if (delegate == null) {
                        return;
                    }
                    delegate.onComplete();
                }
                    break;
                case MSG_ON_CANCEL: {
                    DelegateParam obj = (DelegateParam) msg.obj;
                    FileMgrTransferDelegate delegate = obj.delegate;
                    if (delegate == null) {
                        return;
                    }
                    delegate.onCancel();
                }
                    break;
                case MSG_ON_ERROR: {
                    DelegateParam obj = (DelegateParam) msg.obj;
                    FileMgrTransferDelegate delegate = obj.delegate;
                    if (delegate == null) {
                        return;
                    }
                    int errorCode = ((Integer) obj.param).intValue();
                    delegate.onError(errorCode);
                }
                    break;
                case MSG_ON_CHANGE_TASK: {
                    DelegateParam obj = (DelegateParam) msg.obj;
                    FileMgrTransferDelegate delegate = obj.delegate;
                    if (delegate == null) {
                        return;
                    }
                    String path = (String) obj.param;
                    delegate.onChangeTask(path);
                }
                    break;
                case MSG_ON_TASK_COUNT_CHANGE: {
                    DelegateParam obj = (DelegateParam) msg.obj;
                    FileMgrTransferDelegate delegate = obj.delegate;
                    if (delegate == null) {
                        return;
                    }
                    int taskCount = ((Integer) obj.param).intValue();
                    delegate.onTaskCountChange(taskCount);
                }
                    break;
                case MSG_DOWNLOAD_COMPLETE:
                    String cloudRootPath = (String)msg.obj;
                    BusProvider.getInstance().post(
                            new FileMgrCloudEvent(FileMgrCloudEvent.CLOUD_EVENT_DOWNLOAD_COMPLETE, cloudRootPath));
                    break;
                case MSG_UPLOAD_COMPLETE:
                    String serverPath = (String)msg.obj;
                    BusProvider.getInstance().post(
                            new FileMgrCloudEvent(FileMgrCloudEvent.CLOUD_EVENT_UPLOAD_COMPLETE, serverPath));
                    break;
            }
        }
    };

    public interface FileMgrTransferDelegate {
        void onComplete();
        void onCancel();
        void onError(int errorCode);
        void onProgress(double progress);
        void onChangeTask(String filePath);
        void onTaskCountChange(int taskCount);
    }

    class TransferTask {
        protected WeakReference<FileMgrTransferDelegate>  mDelegate;
        public TransferTask() {
        }
        public void start() {
        }

        void onTaskProgress(double progress) {
            if (mDelegate == null || mDelegate.get() == null) {
                return;
            }
            Message msg = new Message();
            msg.what = MSG_ON_PROGRESS;
            msg.obj = new DelegateParam(mDelegate.get(), new Double(progress));
            mUIHandler.sendMessage(msg);
        }

        void onTaskComplete() {
            mTaskList.clear();
            if (mDelegate == null || mDelegate.get() == null) {
                return;
            }
            mIsTaskCompleted = true;
            Message msg = new Message();
            msg.what = MSG_ON_COMPLETE;
            msg.obj = new DelegateParam(mDelegate.get());
            mUIHandler.sendMessage(msg);
        }

        void onTaskCancel() {
            mTaskList.clear();
            if (mDelegate == null || mDelegate.get() == null) {
                return;
            }
            mIsTaskCompleted = true;
            Message msg = new Message();
            msg.what = MSG_ON_CANCEL;
            msg.obj = new DelegateParam(mDelegate.get());
            mUIHandler.sendMessage(msg);
        }

        void onTaskError(int errorCode) {
            mTaskList.clear();
            if (mDelegate == null || mDelegate.get() == null) {
                return;
            }
            mIsTaskCompleted = true;
            Message msg = new Message();
            msg.what = MSG_ON_ERROR;
            msg.obj = new DelegateParam(mDelegate.get(), Integer.valueOf(errorCode));
            mUIHandler.sendMessage(msg);
        }

        void onTaskChangeTask(String filePath) {
            if (mDelegate == null || mDelegate.get() == null) {
                return;
            }
            Message msg = new Message();
            msg.what = MSG_ON_CHANGE_TASK;
            msg.obj = new DelegateParam(mDelegate.get(), filePath);
            mUIHandler.sendMessage(msg);
        }

        protected void onTaskChangeTaskCount(int taskCount) {
            if (mDelegate == null || mDelegate.get() == null) {
                return;
            }
            Message msg = new Message();
            msg.what = MSG_ON_TASK_COUNT_CHANGE;
            msg.obj = new DelegateParam(mDelegate.get(), Integer.valueOf(taskCount));
            mUIHandler.sendMessage(msg);
        }
    }

    class UploadTask extends TransferTask {
        private String mUploadPath;
        private String mLocalPath;
        private boolean mIsTaskComplete;

        public UploadTask(String uploadPath, String localPath, WeakReference<FileMgrTransferDelegate> delegate) {
            mUploadPath = uploadPath;
            mLocalPath = localPath;
            mDelegate = delegate;
            mTaskCount++;
            mIsTaskComplete = false;
            onTaskChangeTaskCount(mTaskCount);
        }

        private void notifyUploadComplete() {
            Message msg = new Message();
            msg.what = MSG_UPLOAD_COMPLETE;
            msg.obj = mCloudRootPath;
            mUIHandler.sendMessage(msg);
        }

        @Override
        public void start() {
            final KbxTransferTask task = new KbxUploadFileTask(mUploadPath, mLocalPath);
            task.setDelegate(new KbxTransferDelegate() {
                @Override
                public void onStart() {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onStart");
                            Log.d(TAG, "task change: " + mTaskCount + "\t" + mCompleteTaskCount);
                            onTaskChangeTask(((KbxUploadTask) task).getLocalPath());
                        }
                    });
                }

                @Override
                public void onCancel() {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onCancel");
                            mIsTransfering = false;
                            task.destroy();
                            if (!mIsTaskComplete) {
                                onTaskCancel();
                                cancelUploadTask();
                                notifyUploadComplete();
                            }
                        }
                    });
                }

                @Override
                public void onPause() {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onPause");
                            mIsTransfering = false;
                            onTaskError(-1);
                            task.destroy();
                            cancelUploadTask();
                        }
                    });
                }

                @Override
                public void onComplete() {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onComplete");
                            mCompleteTaskCount++;
                            mIsTaskComplete = true;
                            mIsTransfering = false;
                            if  (mCompleteTaskCount == mTaskCount && mTaskList.isEmpty()) {
                                Log.d(TAG, "all task completed!");
                                onTaskComplete();
                                task.destroy();
                                notifyUploadComplete();
                            } else {
                                task.destroy();
                                startNextTask();
                            }
                        }
                    });
                }

                @Override
                public void onError(final int errorCode) {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onError: " + errorCode);
                            mIsTransfering = false;
                            onTaskError(errorCode);
                            task.destroy();
                            cancelUploadTask();
                            notifyUploadComplete();
                        }
                    });
                }

                @Override
                public void ReportSnapInfo(final int speed, final double progress) {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "progress: " + progress);
                            onTaskProgress(progress);
                        }
                    });
                }
            });
            task.start();
        }
    }

    class DownloadTask extends TransferTask {
        private String mServerPath;
        private String mDownloadPath;
        private String mFileId;
        private long mFileSize;
        private boolean mIsDownloaded = false;
        public DownloadTask(String serverPath, String downloadPath, String fileId, long fileSize, WeakReference<FileMgrTransferDelegate> delegate) {
            mServerPath = serverPath;
            mDownloadPath = downloadPath;
            mFileId = fileId;
            mFileSize = fileSize;
            mDelegate = delegate;
            mTaskCount++;
            onTaskChangeTaskCount(mTaskCount);
        }

        private void deleteTmpFiles() {
            File file = new File(mDownloadPath);
            file.delete();
        }

        private void notifyDownloadComplete() {
            Message msg = new Message();
            msg.what = MSG_DOWNLOAD_COMPLETE;
            msg.obj = mServerPath;
            mUIHandler.sendMessage(msg);
        }

        @Override
        public void start() {
            checkDownloadTargetDirectory(mDownloadPath);
            if (mFileSize == 0) {
                Log.d(TAG, "download empty file");
                try {
                    File file = new File(mDownloadPath);
                    file.createNewFile();
                    mCompleteTaskCount++;
                    mIsDownloaded = true;
                    mIsTransfering = false;
                    if  (mCompleteTaskCount == mTaskCount && mTaskList.isEmpty()) {
                        Log.d(TAG, "all task completed!");
                        cancelDownloadTask();
                        onTaskComplete();
                        notifyDownloadComplete();
                    } else {
                        startNextTask();
                    }
                } catch (IOException e) {
                    Log.d(TAG, "create empty file error!");
                }
                return;
            }
            final KbxTransferTask task = new KbxDownloadTask(mServerPath, mDownloadPath, mFileId);
            task.setDelegate(new KbxTransferDelegate() {
                @Override
                public void onStart() {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onStart");
                            Log.d(TAG, "task change: " + mTaskCount + "\t" + mCompleteTaskCount);
//                            String serverPath = ((KbxDownloadTask)task).getServerPath();
                            onTaskChangeTask(mServerPath);
                        }
                    });
                }

                @Override
                public void onCancel() {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onCancel");
                            task.destroy();
                            mIsTransfering = false;
                            if (!mIsDownloaded) {
                                deleteTmpFiles();
                                onTaskCancel();
                                cancelDownloadTask();
                                notifyDownloadComplete();
                            }
                        }
                    });
                }

                @Override
                public void onPause() {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onPause");
                            mIsTransfering = false;
                            onTaskError(-1);
                            task.destroy();
                            cancelDownloadTask();
                            deleteTmpFiles();
                        }
                    });
                }

                @Override
                public void onComplete() {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onComplete");
                            KbxLocalFileManager.insertDownloadFile(mServerPath, mDownloadPath);
                            mCompleteTaskCount++;
                            mIsDownloaded = true;
                            mIsTransfering = false;
                            task.destroy();
                            FileMgrCore.notifyMediaFileChange(mDownloadPath);
                            if  (mCompleteTaskCount == mTaskCount && mTaskList.isEmpty()) {
                                Log.d(TAG, "all task completed!");
                                cancelDownloadTask();
                                onTaskComplete();
                                notifyDownloadComplete();
                            } else {
                                startNextTask();
                            }
                        }
                    });
                }

                @Override
                public void onError(final int errorCode) {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "onError: " + errorCode);
                            onTaskError(errorCode);
                            task.destroy();
                            mIsTransfering = false;
                            cancelDownloadTask();
                            deleteTmpFiles();
                            notifyDownloadComplete();
                        }
                    });
                }

                @Override
                public void ReportSnapInfo(final int speed, final double progress) {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "progress: " + progress);
                            onTaskProgress(progress);
                        }
                    });
                }
            });
            task.start();
        }
    }

    class FetchCloudFileListTask extends TransferTask {
        private String mCloudPath;
        private String mRootPath;
        private String mLocalPath;
        FetchCloudFileListTask(String cloudPath, String rootPath, String localPath, WeakReference<FileMgrTransferDelegate> delegate) {
            mCloudPath = cloudPath;
            mRootPath = rootPath;
            mLocalPath = localPath;
            mDelegate = delegate;
            mTaskCount++;
            onTaskChangeTaskCount(mTaskCount);
        }

        private void notifyFetchComplete() {
            Log.d(TAG, "notifyFetchComplete");
            Message msg = new Message();
            msg.what = MSG_DOWNLOAD_COMPLETE;
            msg.obj = mCloudPath;
            mUIHandler.sendMessage(msg);
        }

        @Override
        public void start() {
            onTaskChangeTask(mCloudPath);
            checkDownloadFolder(mCloudPath, mRootPath, mLocalPath);
            // 从服务端拉取文件列表
            KbxFileManager.updateFileList(mCloudPath,
                    new KbxRequest<KbxUpdateFileListResponse>(new KbxResponse<KbxUpdateFileListResponse>() {
                @Override
                public void onResponse(final KbxUpdateFileListResponse response) {
                    if (response.getErrorNo() == 0) {
                        postToNoneUIThread(new Runnable() {
                            @Override
                            public void run() {
                                // 3. 拉取成功，将文件加入到下载队列
                                mCompleteTaskCount++;
                                KbxFile[] files = KbxFileManager.getFileList(mCloudPath, KbxFileManager.FILE_SORT_TYPE_NAME);
                                if (files.length != 0) {
                                    List<KbxFile> fileList = new ArrayList<KbxFile>();
                                    for (KbxFile file : files) {
                                        fileList.add(file);
                                    }
                                    if (fileList.size() != 0) {
                                        addDownloadTask(fileList, mRootPath, mLocalPath, mDelegate);
                                    }
                                } else {
                                    Log.d(TAG, "download");
                                    if (mCompleteTaskCount == mTaskCount && mTaskList.isEmpty()) {
                                        onTaskComplete();
                                        notifyFetchComplete();
                                    }
                                }
                                mIsTransfering = false;
                                startNextTask();
                            }
                        });
                    } else {
                        postToNoneUIThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "fetch list failed: " + mCloudPath);
                                onTaskError(response.getErrorNo());
                                mIsTransfering = false;
                                cancelDownloadTask();
                                notifyFetchComplete();
                            }
                        });
                    }
                }
            }));
        }
    }

    class UploadFolderTask extends TransferTask {
        private String mFolderName;
        private String mCloudPath;
        UploadFolderTask(String folderName, String cloudPath, WeakReference<FileMgrTransferDelegate> delegate) {
            mFolderName = folderName;
            mCloudPath = cloudPath;
            mDelegate = delegate;
            mTaskCount++;
            onTaskChangeTaskCount(mTaskCount);
        }

        private void notifyUploadFolderComplete() {
            Message msg = new Message();
            msg.what = MSG_UPLOAD_COMPLETE;
            msg.obj = mCloudRootPath;
            mUIHandler.sendMessage(msg);
        }

        @Override
        public void start() {
            Log.d(TAG, "Start New Folder Task");

            String path = mCloudPath + "/" + mFolderName;
            onTaskChangeTask(path);
            KbxFileManager.addFolder(mFolderName, mCloudPath,
                    new KbxRequest<KbxDefaultResponse>(new KbxResponse<KbxDefaultResponse>() {
                @Override
                public void onResponse(final KbxDefaultResponse response) {
                    postToNoneUIThread(new Runnable() {
                        @Override
                        public void run() {
                            mIsTransfering = false;
                            if (response.getErrorNo() == 0) {
                                mCompleteTaskCount++;
                                if (mCompleteTaskCount == mTaskCount && mTaskList.isEmpty()) {
                                    onTaskComplete();
                                    notifyUploadFolderComplete();
                                }
                                startNextTask();
                            } else {
                                onTaskError(response.getErrorNo());
                                cancelUploadTask();
                                notifyUploadFolderComplete();
                            }
                        }
                    });
                }
            }));
        }
    }

    private List<TransferTask> mTaskList = new ArrayList<TransferTask>();
    private int mTaskCount = 0;
    private int mCompleteTaskCount = 0;
    private boolean mIsTransfering = false;
    private boolean mIsTaskCompleted = false;
    private String mCloudRootPath;
    private static FileMgrTransfer sInstance;
    private static String TAG = "FileMgrTransfer";
    private static String mDownloadPath = "/filemanager/cache";
    private WeakReference<FileMgrTransferDelegate> mTransferDelegate = null;
    public static FileMgrTransfer getsInstance() {
        if (sInstance == null) {
            sInstance = new FileMgrTransfer();
        }
        return sInstance;
    }

    public boolean isTransfering() {
        if (mIsTransfering) {
            return true;
        }
        if (!mTaskList.isEmpty()) {
            return true;
        }
        return false;
    }

    public String getSingleDownloadPath(String cloudPath) {
        return mDownloadPath + cloudPath;
    }

    public void init() {
        // 获取数据库中的上传下载任务，并且删除
        resetUploadTask();
        resetDownloadTask();
        mDownloadPath = Environment.getExternalStorageDirectory().getAbsolutePath() + mDownloadPath;
    }

    private void resetUploadTask() {
        KbxUploadTask.getUploadTasks(new KbxRequest<KbxUploadTask[]>(new KbxResponse<KbxUploadTask[]>() {
            @Override
            public void onResponse(final KbxUploadTask[] tasks) {
                postToNoneUIThread(new Runnable() {
                    @Override
                    public void run() {
                        // destroy
                        for (KbxTransferTask task : tasks) {
                            task.setDelegate(new KbxTransferDelegate() {
                                @Override
                                public void onStart() {}
                                @Override
                                public void onCancel() {}
                                @Override
                                public void onPause() {}
                                @Override
                                public void onComplete() {}
                                @Override
                                public void onError(int i) {}
                                @Override
                                public void ReportSnapInfo(int i, double v) {}
                            });
                            task.destroy();
                        }
                    }
                });
            }
        }));
        clean();
    }

    private void resetDownloadTask() {
        KbxDownloadTask.getDownloadTasks(new KbxRequest<KbxDownloadTask[]>(new KbxResponse<KbxDownloadTask[]>() {
            @Override
            public void onResponse(final KbxDownloadTask[] tasks) {
                // destroy
                postToNoneUIThread(new Runnable() {
                    @Override
                    public void run() {
                        for (KbxTransferTask task : tasks) {
                            task.setDelegate(new KbxTransferDelegate() {
                                @Override
                                public void onStart() {}
                                @Override
                                public void onCancel() {}
                                @Override
                                public void onPause() {}
                                @Override
                                public void onComplete() {}
                                @Override
                                public void onError(int i) {}
                                @Override
                                public void ReportSnapInfo(int i, double v) {}
                            });
                            task.destroy();
                        }
                    }
                });
            }
        }));
        clean();
    }

    public void cancelDownloadTask() {
        KbxDownloadTask.cancelAll();
    }

    public void cancelUploadTask() {
        KbxUploadTask.cancelAll();
    }

    private void clean() {
        postToNoneUIThread(new Runnable() {
            @Override
            public void run() {
                mTaskCount = 0;
                mCompleteTaskCount = 0;
                mIsTransfering = false;
                mTaskList.clear();
            }
        });
    }

    private void startNextTask() {
        postToNoneUIThread(new Runnable() {
            @Override
            public void run() {
                if (mIsTransfering) {
                    return;
                }
                mIsTransfering = true;
                if (mTaskList != null && mTaskList.size() > 0) {
                    mIsTaskCompleted = false;
                    TransferTask firstTask = mTaskList.get(0);
                    mTaskList.remove(firstTask);
                    Log.d(TAG, "start New Task");
                    firstTask.start();
                } else {
                    mIsTransfering = false;
                }
            }
        });
    }

    private void checkDownloadTargetDirectory(String path) {
        File file = new File(path);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                Log.d(TAG, "checkDownloadTargetDirectory failed!");
            }
        }
    }

    private void checkDownloadFolder(String cloudPath, String rootPath, String localPath) {
        String folderPath = null;
        if (localPath == null || localPath.length() == 0) {
            folderPath = mDownloadPath;
            if (folderPath.endsWith("/")) {
                folderPath = folderPath.substring(0, folderPath.length() - 1);
            }
            folderPath += cloudPath;
        } else if (rootPath != null && rootPath.length() != 0) {
            if (rootPath.equals("/")) {
                folderPath = localPath + cloudPath;
            } else {
                String subPath = cloudPath.substring(rootPath.length(), cloudPath.length());
                folderPath = localPath + subPath;
            }
        }

        File file = new File(folderPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.d(TAG, "checkDownloadFolder failed!");
            }
        } else {
            Log.d(TAG, "checkDownloadFolder exist");
        }
    }

    private void addUploadEmptyFolderTask(String localPath, String rootPath, String cloudPath, WeakReference<FileMgrTransferDelegate> delegate) {
        String uploadPath;
        if (cloudPath.endsWith("/")) {
            uploadPath = cloudPath.substring(0, cloudPath.length() - 1);
        } else {
            uploadPath = cloudPath;
        }

        if (rootPath != null && rootPath.length() != 0) {
            String subFilePath = localPath.substring(rootPath.length(), localPath.length());
            uploadPath = uploadPath + subFilePath;
        }
        File cloudFolder = new File(uploadPath);
        TransferTask task = new UploadFolderTask(cloudFolder.getName(), cloudFolder.getParent(), delegate);
        mTaskList.add(task);
        startNextTask();
    }

    private void addUploadTask(String localPath, String rootPath, String cloudPath, WeakReference<FileMgrTransferDelegate>  delegate) {
        String uploadPath;
        if (cloudPath.endsWith("/")) {
            uploadPath = cloudPath.substring(0, cloudPath.length() - 1);
        } else {
            uploadPath = cloudPath;
        }

        if (rootPath != null && rootPath.length() != 0) {
            String subFilePath = localPath.substring(rootPath.length(), localPath.length());
            uploadPath = uploadPath + subFilePath;
        } else {
            File file = new File(localPath);
            uploadPath += "/" + file.getName();
        }

        TransferTask task = new UploadTask(uploadPath, localPath, delegate);
        mTaskList.add(task);
        startNextTask();
    }

    private void addFolderToUploadTask(final String localPath, final String rootPath, final String cloudPath, WeakReference<FileMgrTransferDelegate>  delegate) {
        File dir = new File(localPath);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            addUploadEmptyFolderTask(localPath, rootPath, cloudPath, delegate);
            return;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                addFolderToUploadTask(files[i].getAbsolutePath(), rootPath, cloudPath, delegate);
            } else {
                addUploadTask(files[i].getAbsolutePath(), rootPath, cloudPath, delegate);
            }
        }
    }

    private void addUploadTask(List<KbxLocalFile> localFiles, String rootPath, String cloudPath,
                                 WeakReference<FileMgrTransferDelegate>  delegate) {
        for (KbxLocalFile fileInfo : localFiles) {
            if (fileInfo.getIsFolder()) {
                Log.d(TAG, "add Folder Task: " + fileInfo.getFilePath());
                addFolderToUploadTask(fileInfo.getFilePath(), rootPath, cloudPath, delegate);
            } else {
                Log.d(TAG, "add File Task: " + fileInfo.getFilePath());
                addUploadTask(fileInfo.getFilePath(), rootPath, cloudPath, delegate);
            }
        }
    }

    // 当前版本暂且简单处理
    public void uploadLocalFiles(final List<FileInfo> localFiles, final String rootPath, final String cloudPath, FileMgrTransferDelegate delegate) {
        mTransferDelegate = new WeakReference<FileMgrTransferDelegate>(delegate);
        clean();
        postToNoneUIThread(new Runnable() {
            @Override
            public void run() {
                mIsTaskCompleted = false;
                mCloudRootPath = cloudPath;
                if (localFiles.size() == 0) {
                    // 如果任务为空，则返回完成
                    if (mTaskList.size() == 0) {
                        Message msg = new Message();
                        msg.what = MSG_ON_COMPLETE;
                        msg.obj = new DelegateParam(mTransferDelegate.get());
                        mUIHandler.sendMessage(msg);
                    }
                } else {
                    List<KbxLocalFile> fileList = new ArrayList<KbxLocalFile>();
                    for (FileInfo fileInfo : localFiles) {
                        KbxLocalFile file = ((LocalFileInfo) fileInfo).getLocalFile();
                        fileList.add(file);
                    }
                    if (fileList.size() != 0) {
                        addUploadTask(fileList, rootPath, cloudPath, mTransferDelegate);
                    }
                }
            }
        });
    }

    private void addDownloadTask(List<KbxFile> cloudFiles, String rootPath, String localPath,
                                WeakReference<FileMgrTransferDelegate>  delegate) {
        for (KbxFile fileInfo : cloudFiles) {
            if (fileInfo.getIsDir()) {
                Log.d(TAG, "add Folder Task: " + fileInfo.getFullPath());
                addFolderToDownloadTask(fileInfo.getFullPath(), rootPath, localPath, delegate);
            } else {
                Log.d(TAG, "add File Task: " + fileInfo.getFullPath());
                String fileId = fileInfo.getFileId();
                String serverPath = fileInfo.getFullPath();
                String downloadPath = null;
                if (localPath == null || localPath.length() == 0) {
                    downloadPath = mDownloadPath;
                    if (downloadPath.endsWith("/")) {
                        downloadPath = downloadPath.substring(0, downloadPath.length() - 1);
                    }
                    downloadPath += fileInfo.getFullPath();
                } else if (rootPath != null && rootPath.length() != 0) {
                    if (rootPath.equals("/")) {
                        downloadPath = localPath + serverPath;
                    } else {
                        String subPath = serverPath.substring(rootPath.length(), serverPath.length());
                        downloadPath = localPath + subPath;
                    }
                }

                addDownloadTask(serverPath, downloadPath, fileId, fileInfo.getFileSize(), delegate);
            }
        }
    }

    private void addDownloadTask(String serverPath, String downloadPath, String fileId, long fileSize,
                                 WeakReference<FileMgrTransferDelegate>  delegate) {
        TransferTask task = new DownloadTask(serverPath, downloadPath, fileId, fileSize, delegate);
        mTaskList.add(task);
        startNextTask();
    }

    private void addFolderToDownloadTask(final String cloudPath, final String rootPath, final String localPath, WeakReference<FileMgrTransferDelegate>  delegate) {
        TransferTask task = new FetchCloudFileListTask(cloudPath, rootPath, localPath, delegate);
        mTaskList.add(task);
        startNextTask();
    }


    public void downloadCloudFiles(final List<FileInfo> cloudFiles,
                                   final String rootPath, final String localPath,
                                   FileMgrTransferDelegate delegate) {
        mTransferDelegate = new WeakReference<FileMgrTransferDelegate>(delegate);
        clean();
        postToNoneUIThread(new Runnable() {
            @Override
            public void run() {
                mIsTaskCompleted = false;
                List<KbxFile> fileList = new ArrayList<KbxFile>();
                for (FileInfo cloudFile : cloudFiles) {
                    fileList.add(((CloudFileInfo) cloudFile).getCloudFile());
                }
                addDownloadTask(fileList, rootPath, localPath, mTransferDelegate);
            }
        });
    }

    public void downloadCloudFiles(final List<FileInfo> cloudFiles,
                                   final String rootPath, FileMgrTransferDelegate delegate) {
        downloadCloudFiles(cloudFiles, rootPath, "", delegate);
    }
}
