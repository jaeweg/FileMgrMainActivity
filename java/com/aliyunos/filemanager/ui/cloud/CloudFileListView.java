package com.aliyunos.filemanager.ui.cloud;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyunos.filemanager.FileMgrMainActivity;
import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.CloudFileMgr;
import com.aliyunos.filemanager.core.FileMgrClipboard;
import com.aliyunos.filemanager.core.FileMgrCloudEvent;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.core.FileMgrFileEvent;
import com.aliyunos.filemanager.core.FileMgrNetworkInfo;
import com.aliyunos.filemanager.core.FileMgrTransfer;
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.local.LocalFileInfo;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListView;
import com.aliyunos.filemanager.ui.view.FileListViewAdapter;
import com.aliyunos.filemanager.ui.view.dialog.DeleteDialog;
import com.aliyunos.filemanager.ui.view.dialog.MessageDialog;
import com.aliyunos.filemanager.ui.view.dialog.NewFolderDialog;
import com.aliyunos.filemanager.ui.view.dialog.ProgressDialog;
import com.aliyunos.filemanager.ui.view.dialog.RenameDialog;
import com.aliyunos.filemanager.util.FileUtil;
import com.aliyunos.filemanager.util.FooterBarUtil;
import com.aliyunos.filemanager.util.MediaUtil;
import com.aliyunos.filemanager.util.ShareUtil;
import com.aliyunos.filemanager.util.PreferenceUtil;
import com.kanbox.filemgr.KbxLocalFile;
import com.kanbox.filemgr.KbxLocalFileManager;
import com.kanbox.sdk.common.KbxDefaultResponse;
import com.kanbox.sdk.common.KbxErrorCode;
import com.kanbox.sdk.common.KbxRequest;
import com.kanbox.sdk.common.KbxResponse;
import com.kanbox.sdk.filelist.KbxFileManager;
import com.kanbox.sdk.filelist.model.KbxFile;
import com.kanbox.sdk.filelist.request.KbxSetFileListRequest;
import com.kanbox.sdk.filelist.response.KbxUpdateFileListResponse;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

public class CloudFileListView extends FileListView {
    private CloudDelegate mCloudDelegate;
    private MessageDialog mMessageDialog;
    private ProgressDialog mProgressDialog;
    private boolean mCurrentUploadIsCut = false; // 当前上传是否是本地文件剪切
    private boolean mCurrentTaskIsUpload = false;
    private boolean mIsSingleDownload = false;
    private boolean mIsDownloading = false; // 修复bug：#7831026 【100%】快速点击两下在线文件进行下载，下载的提示框显示”准备下载“字样
    private boolean mHasUnfinishedLongTimeTask = false;
    private static String TAG = "CloudFileListView";
    private int[] mMainFooterBarIds = new int[]{
            FileUtil.FILE_OPERATOR_REFRESH,
            FileUtil.FILE_OPERATOR_COPY,
            FileUtil.FILE_OPERATOR_DELETE,
            FileUtil.FILE_OPERATOR_CUT,
            FileUtil.FILE_OPERATOR_NEWFOLDER,
            FileUtil.FILE_OPERATOR_CLOSE_CLOUD
    };
    private List<FileInfo> mFileInfoList;
    private final int CLOUD_FOLDER_IS_EXIST = 10724;
    private final int FILE_WRITE_FAILED = -6;

    private FileMgrTransfer.FileMgrTransferDelegate mTransferDelegate = new FileMgrTransfer.FileMgrTransferDelegate() {
        @Override
        public void onComplete() {
            FileMgrClipboard.getInstance().finishClipboard();
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
            if (mCurrentTaskIsUpload && mCurrentUploadIsCut) {
                KbxLocalFile localFileInfos[] = new KbxLocalFile[mFileInfoList.size()];
                for (int i = 0; i < mFileInfoList.size(); ++i) {
                    LocalFileInfo localFileInfo = (LocalFileInfo) mFileInfoList.get(i);
                    localFileInfos[i] = localFileInfo.localFile;
                }
                FileMgrCore.deleteFiles(localFileInfos, mActivity);
            }
            if (!mCurrentTaskIsUpload && mIsSingleDownload) {
                if (mFileInfoList.size() == 1) {
                    mIsDownloading = false;
                    FileInfo fileInfo = mFileInfoList.get(0);
                    String downloadPath = FileMgrTransfer.getsInstance().getSingleDownloadPath(fileInfo.getPath());
                    BusProvider.getInstance().post(new FileMgrFileEvent(downloadPath, FileMgrFileEvent.CREATE));
                    openDownloadFile(fileInfo);
                }
            }
//            refreshCurrentFileList();
        }

        @Override
        public void onCancel() {
            Log.d(TAG, "onCancel");
            mIsDownloading = false;
            if(mCurrentTaskIsUpload) {
                FileMgrClipboard.getInstance().finishClipboard();
            }

            if(mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
//            refreshCurrentFileList();
        }

        @Override
        public void onError(int errorCode) {
            Log.d(TAG, "onError: " + errorCode);
            mIsDownloading = false;
            FileMgrClipboard.getInstance().finishClipboard();
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }
//            refreshCurrentFileList();
            if (errorCode == KbxErrorCode.KB_ERROR_FILE_NOT_EXIST) {
                Toast.makeText(mActivity, R.string.transfer_file_not_exist, Toast.LENGTH_SHORT).show();
            } else if (errorCode == CLOUD_FOLDER_IS_EXIST) {
                Toast.makeText(mActivity, R.string.ali_dirIsExist, Toast.LENGTH_SHORT).show();
            } else if (errorCode == FILE_WRITE_FAILED) {
                Toast.makeText(mActivity, R.string.file_write_failed, Toast.LENGTH_SHORT).show();
            } else if (errorCode == KbxErrorCode.KB_ERROR_UPLOAD_CAPA_NOT_ENOUGH || errorCode == KbxErrorCode.KB_ERROR_CAPABITY_NOT_ENOUGH){
                if(mCloudDelegate != null){
                    mCloudDelegate.onShowSpaceWarning();
                }
            } else {
                Toast.makeText(mActivity, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onProgress(double progress) {
            if (mProgressDialog != null) {
                mProgressDialog.setProgress(progress);
            }
        }

        @Override
        public void onChangeTask(String path) {
            if (mProgressDialog != null) {
                mProgressDialog.increateCurrentTask();
                mProgressDialog.setMessage(path);
            }
        }

        @Override
        public void onTaskCountChange(int taskCount) {
            if (mProgressDialog != null) {
                mProgressDialog.setTaskCount(taskCount);
            }
        }
    };

    public CloudFileListView(Activity activity) {
        super(activity);
        mItemLongClickIds = new int[]{
                FileUtil.FILE_OPERATOR_RENAME,
                FileUtil.FILE_OPERATOR_COPY,
                FileUtil.FILE_OPERATOR_DELETE,
                FileUtil.FILE_OPERATOR_CUT
        };
        mFolderLongClickIds = mItemLongClickIds;
    }

    public void init(ViewGroup container, CloudFileListAdapter adapter, CloudDelegate delegate) {
        super.init(container, adapter);
        resetFootbarItemsByClipboard();
        this.mCloudDelegate = delegate;
        mListAdapter.setSortMode(FileListViewAdapter.SortMode.SortByName);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCurrentMode != Mode.Selecting) {
            resetFootbarItemsByClipboard();
        }

        if (mHasUnfinishedLongTimeTask && !hasUnfinishedLongTimeTask()) {
            refreshCurrentFileList();
            onClipboardChange(null);
        }
        mHasUnfinishedLongTimeTask =false;
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        if(hasUnfinishedLongTimeTask()) {
            mHasUnfinishedLongTimeTask = true;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(TAG, "finalize");
    }

    private boolean hasUnfinishedLongTimeTask() {
        if(FileMgrClipboard.getInstance().getMode()!= FileMgrClipboard.ClipboardMode.Empty ||
                (mProgressDialog!=null && mProgressDialog.isShowing())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean backToUpLevel() {
        if (super.backToUpLevel()) {
            return true;
        }

        if (mCurrentPath.size() <= 1) {
            return false;
        }

        mCurrentPath.remove(mCurrentPath.size() - 1);
        gotoPath(mCurrentPath);

        return true;
    }

    @Override
    protected void onActionMenuClick(FileInfo fileInfo, int action) {
        super.onActionMenuClick(fileInfo, action);
        switch (action) {
            case FileUtil.FILE_OPERATOR_RENAME:
                RenameDialog.showDialog(mActivity, fileInfo);
                break;
            case FileUtil.FILE_OPERATOR_COPY:
                FileMgrClipboard.getInstance().copyString(getFullPathFromInfo(mCurrentPath));
                FileMgrClipboard.getInstance().copyCloudFile(fileInfo);
                break;
            case FileUtil.FILE_OPERATOR_DELETE:
                List<FileInfo> fileInfos = new ArrayList<FileInfo>();
                fileInfos.add(fileInfo);
                deleteCloudFiles(fileInfos);
                break;
            case FileUtil.FILE_OPERATOR_CUT:
                FileMgrClipboard.getInstance().copyString(getFullPathFromInfo(mCurrentPath));
                FileMgrClipboard.getInstance().cutCloudFile(fileInfo);
                break;
        }
    }

    @Override
    public void onFooterItemClick(View view, int id) {
        if (mCurrentMode == Mode.Selecting) {
            ArrayList<FileInfo> fileInfos = mListAdapter.getSelectedItems();
            switch (id) {
                case FileUtil.FILE_OPERATOR_COPY:
                    FileMgrClipboard.getInstance().copyString(getFullPathFromInfo(mCurrentPath));
                    FileMgrClipboard.getInstance().copyCloudFiles(fileInfos);
                    break;
                case FileUtil.FILE_OPERATOR_CUT:
                    FileMgrClipboard.getInstance().copyString(getFullPathFromInfo(mCurrentPath));
                    FileMgrClipboard.getInstance().cutCloudFiles(fileInfos);
                    break;
                case FileUtil.FILE_OPERATOR_DELETE:
                    deleteCloudFiles(fileInfos);
                    break;
            }
        } else {
            super.onFooterItemClick(view, id);
            switch (id) {
                case FileUtil.FILE_OPERATOR_CANCEL:
                    FileMgrClipboard.getInstance().finishClipboard();
                    break;
                case FileUtil.FILE_OPERATOR_REFRESH:
                    refreshCurrentFileListFromServer();
                    break;
                case FileUtil.FILE_OPERATOR_PASTE:
                    if (FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.CloudFiles_Cut) {
                        List<FileInfo> srcFiles = FileMgrClipboard.getInstance().getClipboardFileList();
                        String dstPath = getCurrentDirectory();
                        cutCloudFiles(srcFiles, dstPath);
                    } else if (FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.CloudFiles_Copy) {
                        List<FileInfo> srcFiles = FileMgrClipboard.getInstance().getClipboardFileList();
                        String dstPath = getCurrentDirectory();
                        preCopyCloudFiles(srcFiles, dstPath);
                    }
                    FileMgrClipboard.getInstance().finishClipboard();
                    break;
                case FileUtil.FILE_OPERATOR_UPLOAD:
                    ArrayList<FileInfo> uploadFiles = FileMgrClipboard.getInstance().getClipboardFileList();
                    String rootPath = FileMgrClipboard.getInstance().getStringContent();
                    if (FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.LocalFiles_Cut) {
                        mCurrentUploadIsCut = true;
                        cutLocalFiles(uploadFiles, rootPath, getFullPathFromInfo(mCurrentPath));
                        mFileInfoList = uploadFiles;
                    } else if (FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.LocalFiles_Copy) {
                        mCurrentUploadIsCut = false;
                        uploadFiles(uploadFiles, rootPath, getFullPathFromInfo(mCurrentPath));
                    }
                    break;
                case FileUtil.FILE_OPERATOR_NEWFOLDER:
                    NewFolderDialog.showCloudDialog(mActivity, getFullPathFromInfo(mCurrentPath));
                    break;
                case FileUtil.FILE_OPERATOR_CLOSE_CLOUD:
                    closeCloud();
                    break;
            }
        }
    }

    @Override
    protected boolean onListItemClick(View view, int id) {
        if (super.onListItemClick(view, id)) {
            return true;
        }

        FileInfo[] fileInfoList = mListAdapter.getFileInfoList();
        if (id > fileInfoList.length)
            throw new IllegalArgumentException("id is out of index");

        FileInfo fileInfo = fileInfoList[id];

        if (fileInfo.isFolder()) {
            setMode(Mode.Normal);
            gotoSubPath(fileInfo.getName(), fileInfo.getPath());
        } else {
            if (KbxLocalFileManager.isDownloaded(fileInfo.getPath())) {
                openDownloadFile(fileInfo);
            } else {
                if (mIsDownloading) {
                    return true;
                }
                mIsSingleDownload = true;
                mIsDownloading = true;
                List<FileInfo> fileInfos = new ArrayList<FileInfo>();
                fileInfos.add(fileInfo);
                mFileInfoList = fileInfos;
                downloadFiles(fileInfos);
            }
        }

        return true;
    }

    private void closeCloud(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setMessage(R.string.close_cloud_dailog_msg);
        builder.setTitle(R.string.close_cloud_dailog_title);
        builder.setNegativeButton(R.string.cancel,null);
        builder.setPositiveButton(R.string.bt_close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if(mCloudDelegate != null){
                        mCloudDelegate.onCloseCloud();
                    }
                }
                });
        AlertDialog dd = builder.create();
        dd.show();
    }

    private void openDownloadFile(FileInfo fileInfo) {
        KbxLocalFile localFile = KbxLocalFileManager.getDownloadedFile(fileInfo.getPath());
        File file = new File(localFile.getFilePath());
        if(file != null && file.getParentFile() != null)
            FileMgrCore.notifyFolderChange(file.getParentFile().getPath());

        // 已经下载的文件
        String mime = ShareUtil.getMimeType(mActivity, localFile.getFilePath());
        if ((mime != null) && (mime.equalsIgnoreCase("application/zip"))) {
            ((FileMgrMainActivity) mActivity).scrollToTab(FileMgrMainActivity.FragmentTab.LocalTab);
            FileMgrClipboard.getInstance().unzipLocalFile(fileInfo);
        } else {
            MediaUtil.openMediaByMime(mActivity, fileInfo, mime);
        }
    }

    @Subscribe
    public void onClipboardChange(FileMgrClipboard.ClipboardChangeEvent event) {
        setMode(Mode.Normal);
        resetFootbarItemsByClipboard();
    }

    @Subscribe
    public void onGetCloudEvent(FileMgrCloudEvent event) {
        if (event.getEventType() == FileMgrCloudEvent.CLOUD_EVENT_REFRESH_CURRENT_LIST) {
            refreshCurrentFileListFromServer();
        } else if (event.getEventType() == FileMgrCloudEvent.CLOUD_EVENT_DOWNLOAD_COMPLETE) {
            // 收到下载完成通知，根据下载的是否是当前目录文件来给云盘文件打勾
            String path = event.getEventContent();
            String currentPath = getFullPathFromInfo(mCurrentPath);
            File file = new File(path);
            if (currentPath.equalsIgnoreCase(file.getParent())) {
                refreshCurrentFileListFromDB();
            }
        } else if (event.getEventType() == FileMgrCloudEvent.CLOUD_EVENT_CUT_COMPLETE) {
            refreshCurrentFileListFromServer();
        } else if (event.getEventType() == FileMgrCloudEvent.CLOUD_EVENT_DOWNLOAD_FILE_DELETE) {
            // 已经下载的文件被删除
            refreshCurrentFileListFromDB();
        } else {
            String path = event.getEventContent();
            String currentPath = getFullPathFromInfo(mCurrentPath);
            if (path.equalsIgnoreCase(currentPath)) {
                refreshCurrentFileListFromServer();
            }
        }
    }

    private void resetFootbarItemsByClipboard() {
        switch (FileMgrClipboard.getInstance().getMode()) {
            case Empty:
            case LocalFiles_UnZip:
            case LocalFiles_Zip:
                setMainFooterBarItems(FooterBarUtil.getItemsByIds(mMainFooterBarIds));
                break;
            case LocalFiles_Copy:
            case LocalFiles_Cut:
                setMainFooterBarItems(FooterBarUtil.getItemsByIds(new int[]{
                        FileUtil.FILE_OPERATOR_CANCEL,
                        FileUtil.FILE_OPERATOR_UPLOAD
                }));
                break;
            case CloudFiles_Copy:
            case CloudFiles_Cut:
                setMainFooterBarItems(FooterBarUtil.getItemsByIds(new int[]{
                        FileUtil.FILE_OPERATOR_CANCEL,
                        FileUtil.FILE_OPERATOR_PASTE
                }));
                break;
        }
    }

    public void gotoSubPath(String showName, String path) {
        mListAdapter.showSearchButton(false);
        gotoPathFromServer(showName, path);
    }

    public void gotoRootDir() {
        showMessageDialog(mActivity.getString(R.string.refreshing));
        mListAdapter.showSearchButton(false);
        mCurrentPath.clear();
        gotoPathFromServer(mActivity.getString(R.string.OnlineFiles), "/");
    }

    public boolean isOnRoot() {
        return mCurrentPath.size() <= 1;
    }

    @Override
    protected void gotoPath(List<PathInfo> pathInfos) {
        mListAdapter.showSearchButton(false);
        String path = pathInfos.get(pathInfos.size() - 1).getPath();
        if (path.isEmpty()) {
            gotoPathFromDB("/");
        } else {
            gotoPathFromDB(path);
        }
        setCrumbsPath(getShowNameFromInfo(pathInfos));
    }

    private void gotoPathFromDB(String path) {
        if (mMessageDialog != null && mMessageDialog.isShowing()) {
            mMessageDialog.dismiss();
        }
        KbxFile[] fileList = KbxFileManager.getFileList(path, KbxFileManager.FILE_SORT_TYPE_NAME);
        mListAdapter.setFileInfoList(
                ((CloudFileListAdapter) mListAdapter).convertToFileInfo(fileList));
        setCrumbsPath(getShowNameFromInfo(mCurrentPath));
    }

    private void onFetFileListError(int errorCode) {
        if (mCloudDelegate != null) {
            if (mMessageDialog != null) {
                mMessageDialog.dismiss();
            }
            mCloudDelegate.onFetchListError(errorCode);
        }
    }

    public void gotoPathFromServer(final String showName, final String path) {
        if (FileMgrNetworkInfo.getsInstance().showNetworkIsAvailable(mActivity) == false) {
            onFetFileListError(-1);
            return;
        }
        CloudFileMgr.getsInstance().updateFileList(path, new KbxRequest<KbxUpdateFileListResponse>(
                new KbxResponse<KbxUpdateFileListResponse>() {
                    @Override
                    public void onResponse(KbxUpdateFileListResponse response) {
                        onFetFileListError(response.getErrorNo());
                        if (response.getErrorNo() == 0) {
                            PathInfo pathInfo = new PathInfo(showName, path);
                            gotoSubFile(pathInfo);
                        }
                    }
                }));
    }

    public void show(boolean isShow) {
        if (isShow) {
            mListView.setVisibility(View.VISIBLE);
        } else {
            mNoItemTextView.setVisibility(View.INVISIBLE);
            mNoItemImageView.setVisibility(View.INVISIBLE);
            mListView.setVisibility(View.INVISIBLE);
        }
    }

    public String getCurrentDirectory() {
        if (mCurrentPath.size() > 0) {
            return mCurrentPath.get(mCurrentPath.size() - 1).getPath();
        }
        return "/";
    }


    public void showMessageDialog(String message) {
        if (mActivity == null || mActivity.isFinishing()) {
            Log.d(TAG, "ShowMessDialog isFinishing");
            return;
        }
        // 只在当前tab才显示
        if (((FileMgrMainActivity) mActivity).getCurrentTab() == FileMgrMainActivity.FragmentTab.CloudTab
                && !PreferenceUtil.getBoolean(PreferenceUtil.Setting.SETTTING_CLOSE_CLOUD)) {
            if (mMessageDialog == null) {
                mMessageDialog = new MessageDialog(mActivity);
            }
            if (!mMessageDialog.isShowing()) {
                mMessageDialog.setMessage(message);
                mMessageDialog.show();
            }
        }
    }

    public void hideMessageDialog() {
        if (mMessageDialog != null && mMessageDialog.isShowing()) {
            mMessageDialog.dismiss();
        }
    }

    public boolean isCurrentPathEmpty() {
        if (mCurrentPath.isEmpty()) {
            return true;
        }

        return false;
    }

    private void showProgressDialog(String message, int taskCount) {
        mProgressDialog = new ProgressDialog(mActivity, message);
        mProgressDialog.setTaskCount(taskCount);
        mProgressDialog.setMessage(mActivity.getResources().getString(R.string.read_to_download));
        mProgressDialog.setCancelDelegate(new ProgressDialog.ProgressDialogDelegate() {
            @Override
            public void onCancel() {
                FileMgrTransfer.getsInstance().cancelUploadTask();
                FileMgrTransfer.getsInstance().cancelDownloadTask();
            }
        });
        mProgressDialog.show();
    }

    private void showToast(String text) {
        Toast.makeText(mActivity, text, Toast.LENGTH_SHORT).show();
    }

    public void refreshCurrentFileListFromServer() {
//        if (FileMgrNetworkInfo.getsInstance().showNetworkIsAvailable(mActivity) == false) {
//            onFetFileListError(-1);
//            return;
//        }
        showMessageDialog(mActivity.getResources().getString(R.string.refreshing));
        final String path = getCurrentDirectory();
        CloudFileMgr.getsInstance().updateFileList(path, new KbxRequest<KbxUpdateFileListResponse>(
                new KbxResponse<KbxUpdateFileListResponse>() {
                    @Override
                    public void onResponse(KbxUpdateFileListResponse response) {
                        onFetFileListError(response.getErrorNo());
                        if (response.getErrorNo() == 0) {
                            refreshCurrentFileListFromDB();
                        }
                    }
                }));
    }

    public void refreshCurrentFileListFromDB() {
        String path = getCurrentDirectory();
        gotoPathFromDB(path);
    }

    public void setupRootInfo() {
        if (mCurrentPath.isEmpty()) {
            PathInfo rootPathInfo = new PathInfo(
                    mActivity.getString(R.string.OnlineFiles), "/");
            mCurrentPath.add(rootPathInfo);
        }
    }

    public void refreshCurrentFileList() {
        String path = getCurrentDirectory();
        Log.d(TAG, "path: " + path);
        KbxFile[] fileList = KbxFileManager.getFileList(path, KbxFileManager.FILE_SORT_TYPE_NAME);

        if (mCurrentMode == Mode.Selecting) {
            boolean isAllSelected = mListAdapter.setFileInfoListWithKeepSelectMode(((CloudFileListAdapter) mListAdapter)
                    .convertToFileInfo(fileList));
            if (mRightSelectCheckBox.isChecked() && isAllSelected) {
                mRightSelectCheckBox.setChecked(true);
            } else {
                mRightSelectCheckBox.setChecked(false);
            }
        } else {
            mListAdapter.setFileInfoList(((CloudFileListAdapter) mListAdapter).convertToFileInfo(fileList));
        }

        setCrumbsPath(getShowNameFromInfo(mCurrentPath));
    }

    void gotoSubFile(PathInfo pathInfo) {
        if (mMessageDialog != null && mMessageDialog.isShowing()) {
            mMessageDialog.dismiss();
        }
        mCurrentPath.add(pathInfo);
        KbxFile[] fileList = KbxFileManager.getFileList(pathInfo.getPath(), KbxFileManager.FILE_SORT_TYPE_NAME);
        mListAdapter.setFileInfoList(
                ((CloudFileListAdapter) mListAdapter).convertToFileInfo(fileList));
        setCrumbsPath(getShowNameFromInfo(mCurrentPath));
    }

    private KbxSetFileListRequest.FileData[] localFileDataToCloudFileData(List<FileInfo> files) {
        int fileCount = files.size();
        KbxSetFileListRequest.FileData[] fileList = new KbxSetFileListRequest.FileData[fileCount];
        for (int i = 0; i < fileCount; i++) {
            FileInfo fileInfo = files.get(i);
            fileList[i] = new KbxSetFileListRequest.FileData();
            fileList[i].fileName = fileInfo.getName();
            fileList[i].filePath = ((CloudFileInfo) fileInfo).getParentPath();
            fileList[i].isDir = fileInfo.isFolder();
        }
        return fileList;
    }

    private void preCopyCloudFiles(final List<FileInfo> srcFiles, final String dstPath) {
        CloudFileMgr.getsInstance().checkSpace(getFilesSize(srcFiles),
                new KbxRequest<KbxDefaultResponse>(new KbxResponse<KbxDefaultResponse>() {
                    @Override
                    public void onResponse(KbxDefaultResponse response) {
                        Log.d(TAG,"preCopyCloudFiles ,checkSpace, Response = "+response.getErrorNo());
                        if (response.getErrorNo() == 0) {
                            copyCloudFiles(srcFiles, dstPath);
                        } else if (response.getErrorNo() == KbxErrorCode.KB_ERROR_UPLOAD_CAPA_NOT_ENOUGH) {
                            if(mCloudDelegate != null){
                                mCloudDelegate.onShowSpaceWarning();
                            }
                        } else {
                            Log.d(TAG, "checkSpace Failed : " + response.getErrorNo());
                            showToast(mActivity.getString(R.string.network_error));
                        }
                    }
                    }));
    }

    private void copyCloudFiles(List<FileInfo> srcFiles, String dstPath) {
        showMessageDialog(mActivity.getResources().getString(R.string.copying));
        KbxSetFileListRequest.FileData[] fileList = localFileDataToCloudFileData(srcFiles);
        CloudFileMgr.getsInstance().copyFiles(fileList, dstPath,
                new KbxRequest<KbxDefaultResponse>(new KbxResponse<KbxDefaultResponse>() {
                    @Override
                    public void onResponse(KbxDefaultResponse response) {
                        mMessageDialog.dismiss();
                        if (response.getErrorNo() == 0) {
                            Log.d(TAG, "copy Success!");
                            refreshCurrentFileListFromServer();
                        } else if(response.getErrorNo() == KbxErrorCode.KB_ERROR_UPLOAD_CAPA_NOT_ENOUGH
                                || response.getErrorNo() == KbxErrorCode.KB_ERROR_CAPABITY_NOT_ENOUGH) {
                            mCloudDelegate.onShowSpaceWarning();
                        } else {
                            Log.d(TAG, "copy Failed: " + response.getErrorNo());
                            int networkType = FileMgrNetworkInfo.getsInstance().getNetworkType();
                            if (networkType == FileMgrNetworkInfo.NETWORK_OTHER) {
                                showToast(mActivity.getString(R.string.network_error));
                            } else {
                                showToast(mActivity.getString(R.string.copy_failed));
                            }
                        }
                    }
                }));
    }

    private void deleteCloudFiles(List<FileInfo> deleteFiles) {
        final KbxSetFileListRequest.FileData[] fileList = localFileDataToCloudFileData(deleteFiles);
        DeleteDialog.showDialog(mActivity, new DeleteDialog.DeleteDialogListener() {
            @Override
            public void onConfirmClicked() {
                CloudFileMgr.getsInstance().deleteFiles(fileList, new KbxRequest<KbxDefaultResponse>(
                        new KbxResponse<KbxDefaultResponse>() {
                            @Override
                            public void onResponse(KbxDefaultResponse response) {
                                mMessageDialog.dismiss();
                                if (response.getErrorNo() == 0) {
                                    Log.d(TAG, "delete Success!");
                                    refreshCurrentFileListFromDB();
                                } else {
                                    Log.d(TAG, "delete Failed: " + response.getErrorNo());
                                    int networkType = FileMgrNetworkInfo.getsInstance().getNetworkType();
                                    if (networkType == FileMgrNetworkInfo.NETWORK_OTHER) {
                                        showToast(mActivity.getString(R.string.network_error));
                                    } else {
                                        showToast(mActivity.getString(R.string.delete_failed));
                                    }
                                }
                                FileMgrClipboard.getInstance().finishClipboard();
                            }
                        }));
                setMode(Mode.Normal);
                showMessageDialog(mActivity.getResources().getString(R.string.deleting));
            }

            @Override
            public void onCancelClicked() {

            }
        });
    }

    private void cutLocalFiles(List<FileInfo> cutFiles, String rootPath, String dstPath) {
        uploadFiles(cutFiles, rootPath, dstPath);
    }

    private void cutCloudFiles(List<FileInfo> cutFiles, String dstPath) {
        showMessageDialog(mActivity.getResources().getString(R.string.cutting));
        KbxSetFileListRequest.FileData[] fileList = localFileDataToCloudFileData(cutFiles);
        CloudFileMgr.getsInstance().moveFiles(fileList, dstPath, new KbxRequest<KbxDefaultResponse>(
                new KbxResponse<KbxDefaultResponse>() {
                    @Override
                    public void onResponse(KbxDefaultResponse response) {
                        mMessageDialog.dismiss();
                        if (response.getErrorNo() == 0) {
                            Log.d(TAG, "cut Success!");
                            refreshCurrentFileListFromServer();
                        } else if (response.getErrorNo() == FileMgrNetworkInfo.ERROR_SRC_DEST_SAME_PARENT) {
                            showToast(mActivity.getString(R.string.err_same_name_file_exist));
                        } else {
                            Log.d(TAG, "cut Failed: " + response.getErrorNo());
                            int networkType = FileMgrNetworkInfo.getsInstance().getNetworkType();
                            if (networkType == FileMgrNetworkInfo.NETWORK_OTHER) {
                                showToast(mActivity.getString(R.string.network_error));
                            } else {
                                showToast(mActivity.getString(R.string.cut_failed));
                            }
                        }
                    }
                }));
    }

    private void downloadFiles(final List<FileInfo> cloudFiles) {
        FileMgrNetworkInfo.getsInstance().showNetworkIsAvailable(mActivity, false,
                new FileMgrNetworkInfo.NetworkInfoListener() {
                    @Override
                    public void onOK() {
                        showProgressDialog(mActivity.getString(R.string.downloading), 0);
                        if (mProgressDialog != null) {
                            mProgressDialog.setMessage(mActivity.getString(R.string.read_to_download));
                            mProgressDialog.setTaskCount(cloudFiles.size());
                        }
                        mCurrentUploadIsCut = false;
                        mCurrentTaskIsUpload = false;
                        FileMgrTransfer.getsInstance().downloadCloudFiles(cloudFiles, getFullPathFromInfo(mCurrentPath), mTransferDelegate);
                    }

                    @Override
                    public void onForbid() {

                    }
                });
    }

    private void uploadFiles(final List<FileInfo> localFiles, final String rootPath, final String cloudPath) {
        FileMgrNetworkInfo.getsInstance().showNetworkIsAvailable(mActivity, true,
                new FileMgrNetworkInfo.NetworkInfoListener() {
                    @Override
                    public void onOK() {
                        CloudFileMgr.getsInstance().checkSpace(getFilesSize(localFiles),
                            new KbxRequest<KbxDefaultResponse>(new KbxResponse<KbxDefaultResponse>() {
                                @Override
                                public void onResponse(KbxDefaultResponse response) {
                                    Log.d(TAG," uploadFiles,checkSpace, Response = "+response.getErrorNo());
                                    if (response.getErrorNo() == 0) {
                                        showProgressDialog(mActivity.getString(R.string.uploading), 0);
                                        if (mProgressDialog != null) {
                                            mProgressDialog.setMessage(mActivity.getString(R.string.read_to_upload));
                                            mProgressDialog.setTaskCount(localFiles.size());
                                        }
                                        mIsSingleDownload = false;
                                        mCurrentTaskIsUpload = true;
                                        FileMgrTransfer.getsInstance().uploadLocalFiles(localFiles, rootPath, cloudPath, mTransferDelegate);
                                    } else if(response.getErrorNo() == KbxErrorCode.KB_ERROR_UPLOAD_CAPA_NOT_ENOUGH){
                                        if(mCloudDelegate != null){
                                            mCloudDelegate.onShowSpaceWarning();
                                        }
                                    } else {
                                        Log.d(TAG, "checkSpace Failed: " + response.getErrorNo());
                                        showToast(mActivity.getString(R.string.network_error));
                                    }
                                }
                                }));
                    }

                    @Override
                    public void onForbid() {

                    }
                });
    }

    private long getFilesSize(List<FileInfo> files){
        long totalSize = 0;
        for (FileInfo fileInfo : files) {
            if(fileInfo.isFolder()){
                totalSize += getFileSize(new File(fileInfo.getPath()));
            }else{
                totalSize += fileInfo.getSize();
            }
        }
        Log.d(TAG," getFilesSize , total size = "+totalSize);
        return totalSize;
    }

    private long getFileSize(File f) {
        long size = 0;
        File flist[] = f.listFiles();
        if(flist == null){
            return size;
        }
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory()){
                size += getFileSize(flist[i]);
            } else {
                size += flist[i].length();
            }
        }
        return size;
    }

    @Override
    protected void resetFooterBarMenu() {
        super.resetFooterBarMenu();

        if (mCurrentMode == Mode.Normal &&
                FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.Empty) {
            if (FileMgrCore.isScanned()) {
                boolean enable = mListAdapter.getCount() != 0;
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_COPY, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_DELETE, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_CUT, enable);
            }
        }
    }

    @Override
    protected void resetFooterBarMenuInNormal() {
        super.resetFooterBarMenuInNormal();
        TextView indexingTextView = (TextView) mContainer.findViewById(R.id.indexing_text);
        indexingTextView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void resetFooterBarMenuInGetContextMode() {
        super.resetFooterBarMenuInGetContextMode();
        TextView indexingTextView = (TextView) mContainer.findViewById(R.id.indexing_text);
        indexingTextView.setVisibility(View.INVISIBLE);
    }

    public  void dismissTranserView() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}
