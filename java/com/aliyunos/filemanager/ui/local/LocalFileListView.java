package com.aliyunos.filemanager.ui.local;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.aliyunos.filemanager.FileMgrApplication;
import com.aliyunos.filemanager.FileMgrMainActivity;
import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.CloudFileMgr;
import com.aliyunos.filemanager.core.FileMgrClipboard;
import com.aliyunos.filemanager.core.FileMgrCloudEvent;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.core.FileMgrFileEvent;
import com.aliyunos.filemanager.core.FileMgrLocalEvent;
import com.aliyunos.filemanager.core.FileMgrNetworkInfo;
import com.aliyunos.filemanager.core.FileMgrScanEvent;
import com.aliyunos.filemanager.core.FileMgrTransfer;
import com.aliyunos.filemanager.core.FileMgrWatcher;
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.cloud.CloudFileInfo;
import com.aliyunos.filemanager.ui.view.FileActionSheet;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListView;
import com.aliyunos.filemanager.ui.view.FileListViewAdapter;
import com.aliyunos.filemanager.ui.view.dialog.CrushDialog;
import com.aliyunos.filemanager.ui.view.dialog.DeleteDialog;
import com.aliyunos.filemanager.ui.view.dialog.FileDetailDialog;
import com.aliyunos.filemanager.ui.view.dialog.MessageDialog;
import com.aliyunos.filemanager.ui.view.dialog.NewFolderDialog;
import com.aliyunos.filemanager.ui.view.dialog.ProgressDialog;
import com.aliyunos.filemanager.ui.view.dialog.RenameDialog;
import com.aliyunos.filemanager.ui.view.dialog.UnZipDialog;
import com.aliyunos.filemanager.ui.view.dialog.ZipDialog;
import com.aliyunos.filemanager.util.FileUtil;
import com.aliyunos.filemanager.util.FooterBarUtil;
import com.aliyunos.filemanager.util.MediaUtil;
import com.aliyunos.filemanager.util.PreferenceUtil;
import com.aliyunos.filemanager.util.ShareUtil;
import com.kanbox.filemgr.KbxFileChangeEvent;
import com.kanbox.filemgr.KbxLocalFile;
import com.kanbox.filemgr.KbxLocalFileManager;
import com.kanbox.sdk.common.KbxDefaultResponse;
import com.kanbox.sdk.common.KbxRequest;
import com.kanbox.sdk.common.KbxResponse;
import com.kanbox.sdk.filelist.request.KbxSetFileListRequest;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import hwdroid.widget.ActionSheet;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.content.Context;

public class LocalFileListView extends FileListView {
    private static String TAG = "LocalFileListView";
    private static final int MSG_CLOSE_PROGRESS_DIALOG = 0;
    private static final int MSG_SHOW_MESSAGE_DIALOG = 1;
    private static final int MSG_CLOSE_MESSAGE_DIALOG = 2;
    private static final int MSG_SHOW_NETWORK_ERROR = 3;
    private int[] mMainFooterBarIds = new int[] {
            FileUtil.FILE_OPERATOR_SORT,
            FileUtil.FILE_OPERATOR_COPY,
            FileUtil.FILE_OPERATOR_DELETE,
            FileUtil.FILE_OPERATOR_CUT,
            FileUtil.FILE_OPERATOR_SHARE,
            FileUtil.FILE_OPERATOR_ZIP,
            FileUtil.FILE_OPERATOR_NEWFOLDER,
            FileUtil.FILE_OPERATOR_SHOW_HIDE_FILE
    };

    private int[] mZipFileLongClickIds = new int[] {
            FileUtil.FILE_OPERATOR_SHOW_DETAIL,
            FileUtil.FILE_OPERATOR_RENAME,
            FileUtil.FILE_OPERATOR_COPY,
            FileUtil.FILE_OPERATOR_DELETE,
            FileUtil.FILE_OPERATOR_CUT,
            FileUtil.FILE_OPERATOR_SHARE,
            FileUtil.FILE_OPERATOR_CRUSH
    };

    private PathInfo mGetPathInfo;
    private List<PathInfo> mGetPathInfoList;
    private boolean mGetPathFromList;
    private boolean mISGetFileList;
    private boolean mHasUnfinishedLongTimeTask = false;

    private FileMgrCore.GetLocalFileListListener mGetLocalFileListListener = new FileMgrCore.GetLocalFileListListener() {
        @Override
        public void onGetFileList(KbxLocalFile[] fileList) {
            String path;
            if (mGetPathFromList) {
                path = getShowNameFromInfo(mGetPathInfoList);
            } else {
                mCurrentPath.add(mGetPathInfo);
                path = getShowNameFromInfo(mCurrentPath);
            }
            cancelLoadDialog();
            setCrumbsPath(path);
            mListAdapter.showSearchButton(false);
            if (mCurrentMode == Mode.Selecting) {
                boolean isAllSelected = mListAdapter.setFileInfoListWithKeepSelectMode(
                        ((LocalFileListAdapter) mListAdapter).convertToFileInfo(fileList));
                if (isAllSelected) {
                    mRightSelectCheckBox.setChecked(true);
                } else {
                    mRightSelectCheckBox.setChecked(false);
                }

            } else {
                mListAdapter.setFileInfoList(
                        ((LocalFileListAdapter) mListAdapter).convertToFileInfo(fileList));
                if (mGetPathFromList) {
                    int lastIndex = mGetPathInfoList.get(mGetPathInfoList.size() - 1).getLastIndex();
                    mListView.setSelection(lastIndex);
                }
            }

            mISGetFileList = false;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }
    };

    private FileMgrTransfer.FileMgrTransferDelegate mDelegate;
    private ProgressDialog mProgressDialog;
    private MessageDialog mMessageDialog;
    private MessageDialog mLoadDialog;

    private Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CLOSE_PROGRESS_DIALOG:
                    if (mProgressDialog != null) {
                        mProgressDialog.dismiss();
                        mProgressDialog = null;
                    }
                    break;
                case MSG_SHOW_MESSAGE_DIALOG:
                    mMessageDialog = new MessageDialog(mActivity);
                    String title = (String)msg.obj;
                    mMessageDialog.setMessage(title);
                    break;
                case MSG_CLOSE_MESSAGE_DIALOG:
                    if (mMessageDialog != null) {
                        mMessageDialog.dismiss();
                        mMessageDialog = null;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public LocalFileListView(Activity activity) {
        super(activity);
        mItemLongClickIds = new int[] {
                FileUtil.FILE_OPERATOR_SHOW_DETAIL,
                FileUtil.FILE_OPERATOR_RENAME,
                FileUtil.FILE_OPERATOR_COPY,
                FileUtil.FILE_OPERATOR_DELETE,
                FileUtil.FILE_OPERATOR_CUT,
                FileUtil.FILE_OPERATOR_ZIP,
                FileUtil.FILE_OPERATOR_SHARE,
                FileUtil.FILE_OPERATOR_CRUSH
        };
        mFolderLongClickIds = new int[] {
                FileUtil.FILE_OPERATOR_SHOW_DETAIL,
                FileUtil.FILE_OPERATOR_RENAME,
                FileUtil.FILE_OPERATOR_COPY,
                FileUtil.FILE_OPERATOR_DELETE,
                FileUtil.FILE_OPERATOR_CUT,
                FileUtil.FILE_OPERATOR_ZIP
        };
    }

    public void init(ViewGroup container, FileListViewAdapter adapter, Mode mode) {
        super.init(container, adapter);

        setMode(mode);
        resetFooterBarShowHiddenItem(false);
        resetFootbarItemsByClipboard();
        mListAdapter.setSortMode(
                FileListViewAdapter.SortMode.values()[
                        PreferenceUtil.getInteger(PreferenceUtil.Setting.SETTINGS_KEY_ORDER)]);
    }

    public void getFileList(String path) {
        if (mISGetFileList) {
            return;
        }
        mISGetFileList = true;
        showLoadDialog();
        FileMgrCore.getFileList(path, mGetLocalFileListListener);
    }

    public void gotoRootPath() {
        mCurrentPath.clear();

        PathInfo rootPathInfo = new PathInfo(
                mActivity.getString(R.string.ali_localstorage),
                "");

        mCurrentPath.add(rootPathInfo);
        setCrumbsPath(getShowNameFromInfo(mCurrentPath));
        mListAdapter.showSearchButton(false);
        mListAdapter.setFileInfoList(
                ((LocalFileListAdapter) mListAdapter).convertToFileInfo(FileMgrCore.getRootFolderList()));
    }

    public void gotoSubPath(String showName, String path) {
        if (!mISGetFileList) {
            mGetPathInfo = new PathInfo(showName, path);
            mGetPathFromList = false;
            getFileList(path);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mHasUnfinishedLongTimeTask && !hasUnfinishedLongTimeTask()) {
            onRefreshCurrentList(null);
            onClipboardChange(null);
        }
        mHasUnfinishedLongTimeTask =false;
        if(FileMgrWatcher.getInstance().isNeedRefreshLocal()){
            FileMgrFileEvent localFileEvent = FileMgrWatcher.getInstance().getLocalFileEvent();
            onFileChange(localFileEvent);
            FileMgrWatcher.getInstance().setNeedRefreshLocal(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(hasUnfinishedLongTimeTask()) {
            mHasUnfinishedLongTimeTask = true;
        }
        FileMgrWatcher.getInstance().setNeedRefreshLocal(false);
    }

    public boolean hasUnfinishedLongTimeTask() {
        if (FileMgrClipboard.getInstance().getMode()!= FileMgrClipboard.ClipboardMode.Empty ||
                FileMgrCore.isProgressShowing() ||
                (mProgressDialog !=null && mProgressDialog.isShowing())) {
            return true;
        }
        return false;
    }

    @Override
    protected void gotoPath(List<PathInfo> pathInfos) {
        String path = pathInfos.get(pathInfos.size() - 1).getPath();
        if (path.isEmpty()) {
            setCrumbsPath(getShowNameFromInfo(pathInfos));
            mListAdapter.showSearchButton(false);
            mListAdapter.setFileInfoList(
                    ((LocalFileListAdapter)mListAdapter).convertToFileInfo(FileMgrCore.getRootFolderList()));
        } else {
            Log.d(TAG, "begin gotoPath");
            if (!mISGetFileList) {
                mGetPathInfoList = pathInfos;
                mGetPathFromList = true;
                getFileList(path);
            }
            Log.d(TAG, "end gotoPath");
        }
    }

    public void gotoPath(String path) {
        //root
        PathInfo rootPathInfo = new PathInfo(
                mActivity.getString(R.string.ali_localstorage),
                "");
        mCurrentPath.add(rootPathInfo);

        //volume
        String volumePath = "";
        KbxLocalFile[] rootList = FileMgrCore.getRootFolderList();
        for (KbxLocalFile file:rootList) {
            volumePath = file.getFilePath();
            if (path.length() >= volumePath.length() &&
                    volumePath.equals(path.substring(0, volumePath.length()))) {
                PathInfo pathInfo = new PathInfo(file.getShowName(), file.getFilePath());
                mCurrentPath.add(pathInfo);
                path = path.substring(volumePath.length());
                break;
            }
        }

        if (volumePath.isEmpty()) {
            throw new IllegalArgumentException("path is error:" + path);
        }

        //sub path
        String subPath = volumePath;
        String[] subPathNames = path.split(File.separator);
        for (String subPathName:subPathNames) {
            if (!subPathName.isEmpty()) {
                subPath += File.separator + subPathName;

                PathInfo pathInfo = new PathInfo(subPathName, subPath);
                mCurrentPath.add(pathInfo);
            }
        }

        gotoPath(mCurrentPath);
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
    protected boolean onListItemClick(View view, int id) {
        if (super.onListItemClick(view, id)) {
            return true;
        }

        FileInfo[] fileInfoList = mListAdapter.getFileInfoList();
        if (id > fileInfoList.length)
            throw new IllegalArgumentException("id is out of index");

        FileInfo fileInfo = fileInfoList[id];

        if (fileInfo.isFolder()) {
            if (mCurrentMode != Mode.GetContext) {
                setMode(Mode.Normal);
            }
            final int lastIndex = mListView.getFirstVisiblePosition();
            mCurrentPath.get(mCurrentPath.size() - 1).setLastIndex(lastIndex);
            gotoSubPath(fileInfo.getName(), fileInfo.getPath());
        } else {
            String mime = ShareUtil.getMimeType(mActivity, fileInfo.getPath());
            if((mime != null)&&(mime.equalsIgnoreCase("application/zip"))) {
                if (mActivity instanceof FileMgrMainActivity) {
                    ((FileMgrMainActivity)mActivity).scrollToTab(FileMgrMainActivity.FragmentTab.LocalTab);
                    FileMgrClipboard.getInstance().unzipLocalFile(fileInfo);
                } else {
                    Toast.makeText(mActivity, R.string.ali_need_exec_by_fmgr, Toast.LENGTH_SHORT).show();
                }
            }else {
                MediaUtil.openMediaByMime(mActivity, fileInfo, mime);
            }
        }

        return true;
    }

    protected boolean onListItemLongClick(View itemView, int id) {
        if (mCurrentPath.size() == 1) {
            // skip on root path.
            return true;
        }

        if (mCurrentMode == Mode.Selecting || mCurrentMode == Mode.GetContext) {
            return true;
        }

        FileInfo[] fileInfoList = mListAdapter.getFileInfoList();
        if (id < 0 || id > fileInfoList.length)
            return true;

        final FileInfo fileInfo = fileInfoList[id];
        if (!fileInfo.isFolder()) {
            String mime = ShareUtil.getMimeType(mActivity, fileInfo.getPath());
            if ((mime != null) && (mime.equalsIgnoreCase("application/zip"))) {
                final int[] menuIds = mZipFileLongClickIds;
                FileActionSheet actionSheet = new FileActionSheet(mActivity);
                actionSheet.showMenu(menuIds, new ActionSheet.CommonButtonListener() {
                    @Override
                    public void onClick(int position) {
                        onActionMenuClick(fileInfo, menuIds[position]);
                    }

                    @Override
                    public void onDismiss(ActionSheet actionSheet) {
                    }
                });
                return true;
            }
        }

        return super.onListItemLongClick(itemView, id);
    }

    @Override
    protected void onActionMenuClick(FileInfo fileInfo, int action) {
        super.onActionMenuClick(fileInfo, action);
        switch (action) {
            case FileUtil.FILE_OPERATOR_SHOW_DETAIL:
                FileDetailDialog.showDialog(mActivity, (LocalFileInfo)fileInfo);
                break;
            case FileUtil.FILE_OPERATOR_RENAME:
                RenameDialog.showDialog(mActivity, fileInfo);
                break;
            case FileUtil.FILE_OPERATOR_COPY:
                FileMgrClipboard.getInstance().copyString(getFullPathFromInfo(mCurrentPath));
                FileMgrClipboard.getInstance().copyLocalFile(fileInfo);
                break;
            case FileUtil.FILE_OPERATOR_DELETE:
                final KbxLocalFile localFiles[] = new KbxLocalFile[1];
                LocalFileInfo localFileInfo = (LocalFileInfo) fileInfo;
                localFiles[0] = localFileInfo.localFile;
                DeleteDialog.showDialog(mActivity, new DeleteDialog.DeleteDialogListener() {
                    @Override
                    public void onConfirmClicked() {
                        FileMgrCore.deleteFiles(localFiles, mActivity);
                    }

                    @Override
                    public void onCancelClicked() {

                    }
                });
                break;
            case FileUtil.FILE_OPERATOR_CUT:
                FileMgrClipboard.getInstance().copyString(getFullPathFromInfo(mCurrentPath));
                FileMgrClipboard.getInstance().cutLocalFile(fileInfo);
                break;
            case FileUtil.FILE_OPERATOR_ZIP:
                File file = new File(fileInfo.getPath());
                ArrayList<FileInfo> fileInfos = new ArrayList<FileInfo>();
                fileInfos.add(fileInfo);
                FileMgrClipboard.getInstance().zipLocalFile();
                ZipDialog mZipDialog = new ZipDialog(mActivity, fileInfos,file.getParentFile().getPath());
                mZipDialog.show();
                break;
            case FileUtil.FILE_OPERATOR_CRUSH:
                CrushDialog.showDialog(mActivity, fileInfo.getPath());
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
                    FileMgrClipboard.getInstance().copyLocalFiles(fileInfos);
                    break;
                case FileUtil.FILE_OPERATOR_CUT:
                    FileMgrClipboard.getInstance().copyString(getFullPathFromInfo(mCurrentPath));
                    FileMgrClipboard.getInstance().cutLocalFiles(fileInfos);
                    break;

                case FileUtil.FILE_OPERATOR_SHARE:
                    if(mListAdapter.getSelectedItemCount() > 0 ) {
                        for (FileInfo f: fileInfos) {
                            File file = new File(f.getPath());
                            if(file.isDirectory()) {
                                Toast.makeText(mActivity, R.string.ali_share_fold, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        backToUpLevel();
                        ShareUtil.shareDialog(mActivity, fileInfos);
                    }
                    break;
                case FileUtil.FILE_OPERATOR_ZIP:
                    File file = new File(fileInfos.get(0).getPath());
                    FileMgrClipboard.getInstance().zipLocalFile();
                    ZipDialog mZipDialog = new ZipDialog(mActivity, fileInfos,file.getParentFile().getPath());
                    mZipDialog.show();
                    break;
                case FileUtil.FILE_OPERATOR_DELETE:
                    final KbxLocalFile localFiles[] = new KbxLocalFile[fileInfos.size()];
                    for (int i = 0; i < fileInfos.size(); ++i) {
                        LocalFileInfo localFileInfo = (LocalFileInfo) fileInfos.get(i);
                        localFiles[i] = localFileInfo.localFile;
                    }
                    DeleteDialog.showDialog(mActivity, new DeleteDialog.DeleteDialogListener() {
                        @Override
                        public void onConfirmClicked() {
                            FileMgrCore.deleteFiles(localFiles, mActivity);
                            setMode(Mode.Normal);
                        }

                        @Override
                        public void onCancelClicked() {

                        }
                    });
                    break;
            }
        } else {
            super.onFooterItemClick(view, id);
            switch (id) {
                case FileUtil.FILE_OPERATOR_CANCEL:
                    FileMgrClipboard.getInstance().finishClipboard();
                    break;
                case FileUtil.FILE_OPERATOR_PASTE:
                    if (FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.LocalFiles_Copy
                            || FileMgrClipboard.getInstance().getMode()  == FileMgrClipboard.ClipboardMode.LocalFiles_Cut) {
                        ArrayList<FileInfo> selectedFileInfos = FileMgrClipboard.getInstance().getClipboardFileList();
                        KbxLocalFile localFiles[] = new KbxLocalFile[selectedFileInfos.size()];

                        for (int i = 0; i < selectedFileInfos.size(); ++i) {
                            LocalFileInfo localFileInfo = (LocalFileInfo)selectedFileInfos.get(i);
                            localFiles[i] = localFileInfo.localFile;
                        }
                        String path = getFullPathFromInfo(mCurrentPath) + "/";

                        //异常检测，确保目标文件夹不是源文件夹本身或者子文件夹；
                        boolean isValidPath = true;
                        for(int i = 0;  i< selectedFileInfos.size(); ++i) {
                            String srcPath = localFiles[i].getFilePath();
                            File file = new File(srcPath);
                            if (file.isDirectory()) {
                                if (srcPath.equals(mCurrentPath) || path.indexOf(srcPath + "/") == 0) {
                                    isValidPath = false;
                                    break;
                                }
                            }
                        }

                        if (FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.LocalFiles_Copy) {
                            if (!isValidPath) {
                                Toast.makeText(mActivity, mActivity.getString(R.string.copy_failed), Toast.LENGTH_SHORT).show();
                                FileMgrClipboard.getInstance().finishClipboard();
                                return;
                            }
                            FileMgrCore.copyFiles(localFiles, path, mActivity);
                        }
                        else if (FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.LocalFiles_Cut) {
                            if (!isValidPath) {
                                Toast.makeText(mActivity, mActivity.getString(R.string.cut_failed), Toast.LENGTH_SHORT).show();
                                FileMgrClipboard.getInstance().finishClipboard();
                                return;
                            }
                            FileMgrCore.moveFiles(localFiles, path, mActivity);
                        }

                    }
                    break;
                case FileUtil.FILE_OPERATOR_DOWNLOAD:
                    String rootPath = FileMgrClipboard.getInstance().getStringContent();
                    String currentPath = getFullPathFromInfo(mCurrentPath);
                    if (FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.CloudFiles_Cut) {
                        cutCloudFiles(FileMgrClipboard.getInstance().getClipboardFileList(), rootPath, currentPath);
                    } else if (FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.CloudFiles_Copy) {
                        downloadCloudFiles(FileMgrClipboard.getInstance().getClipboardFileList(), rootPath, currentPath);
                    }
                    break;
                case FileUtil.FILE_OPERATOR_SHOW_HIDE_FILE:
                    PreferenceUtil.setBoolean(PreferenceUtil.Setting.SETTINGS_SHOW_HIDDEN, true);
                    gotoPath(mCurrentPath);
                    resetFooterBarShowHiddenItem(true);
                    break;
                case FileUtil.FILE_OPERATOR_HIDE_FILE:
                    PreferenceUtil.setBoolean(PreferenceUtil.Setting.SETTINGS_SHOW_HIDDEN, false);
                    gotoPath(mCurrentPath);
                    resetFooterBarShowHiddenItem(true);
                    break;
                case FileUtil.FILE_OPERATOR_NEWFOLDER:
                    NewFolderDialog.showDialog(mActivity, getFullPathFromInfo(mCurrentPath));
                    break;
                case FileUtil.FILE_OPERATOR_UNZIP:
                    ArrayList<FileInfo> fileInfoList = FileMgrClipboard.getInstance().getClipboardFileList();
                    UnZipDialog mUnZipDialog = new UnZipDialog(mActivity,fileInfoList.get(0), getFullPathFromInfo(mCurrentPath));
                    mUnZipDialog.show();
                    break;
            }
        }
    }


    @Subscribe
    public void onClipboardChange(FileMgrClipboard.ClipboardChangeEvent event) {
        setMode(Mode.Normal);
        resetFootbarItemsByClipboard();
    }

    private void resetFootbarItemsByClipboard() {
        switch (FileMgrClipboard.getInstance().getMode()) {
            case Empty:
            case LocalFiles_Zip:
                setMainFooterBarItems(FooterBarUtil.getItemsByIds(mMainFooterBarIds));
                break;
            case LocalFiles_Copy:
            case LocalFiles_Cut:
                setMainFooterBarItems(FooterBarUtil.getItemsByIds(new int[]{
                        FileUtil.FILE_OPERATOR_CANCEL,
                        FileUtil.FILE_OPERATOR_PASTE
                }));
                break;
            case CloudFiles_Copy:
            case CloudFiles_Cut:
                setMainFooterBarItems(FooterBarUtil.getItemsByIds(new int[]{
                        FileUtil.FILE_OPERATOR_CANCEL,
                        FileUtil.FILE_OPERATOR_DOWNLOAD
                }));
                break;
            case LocalFiles_UnZip:
                setMainFooterBarItems(FooterBarUtil.getItemsByIds(new int[] {
                        FileUtil.FILE_OPERATOR_CANCEL,
                        FileUtil.FILE_OPERATOR_UNZIP
                }));


                if (mCurrentMode == Mode.Normal) {
                    String currentPath = getFullPathFromInfo(mCurrentPath);
                    if (currentPath.equals("/")) {
                        Toast.makeText(FileMgrApplication.getInstance().getApplicationContext(),
                                R.string.select_unzip_path, Toast.LENGTH_SHORT).show();
                    }
                }
        }
    }

    private void resetFooterBarShowHiddenItem(boolean refresh) {
        if (PreferenceUtil.getBoolean(PreferenceUtil.Setting.SETTINGS_SHOW_HIDDEN)) {
            mMainFooterBarIds[mMainFooterBarIds.length - 1] = FileUtil.FILE_OPERATOR_HIDE_FILE;
        } else {
            mMainFooterBarIds[mMainFooterBarIds.length - 1] = FileUtil.FILE_OPERATOR_SHOW_HIDE_FILE;
        }

        if (refresh) {
            setMainFooterBarItems(FooterBarUtil.getItemsByIds(mMainFooterBarIds));
        }
    }

    void checkRootDir() {
        String currentPath = getFullPathFromInfo(mCurrentPath);
        if (currentPath == "/" || mCurrentPath == null || mCurrentPath.size() == 0) {
            Log.d(TAG, "current path is root path");
            gotoPath(mCurrentPath);
        } else {
            boolean isRootChanged = true;
            KbxLocalFile[] rootFileList = KbxLocalFileManager.getRootFolderList();
            for (KbxLocalFile root : rootFileList) {
                if (currentPath.startsWith(root.getFilePath())) {
                    isRootChanged = false;
                    break;
                }
            }
            if (isRootChanged) {
                Log.d(TAG, "path is removed: " + currentPath);
                setMode(Mode.Normal);
                gotoRootPath();
            }
        }
    }

    @Subscribe
    public void onScanCompleted(FileMgrScanEvent event) {
    }

    @Subscribe
    public void onRefreshCurrentList(FileMgrLocalEvent event) {
        String path = mCurrentPath.get(mCurrentPath.size() - 1).getPath();
        if (path.isEmpty()) {
            setCrumbsPath(getShowNameFromInfo(mCurrentPath));
            mListAdapter.setFileInfoList(
                    ((LocalFileListAdapter)mListAdapter).convertToFileInfo(FileMgrCore.getRootFolderList()));
        } else {
            if (!mISGetFileList) {
                mGetPathFromList = true;
                mGetPathInfoList = mCurrentPath;
                getFileList(path);
            }
        }
    }

    public  void dismissTranserView() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Subscribe
    public void onFileChange(FileMgrFileEvent event) {
        if (event.getEventType() == FileMgrFileEvent.EVENT_TYYP_ROOTDIRCHANGE_EVENT) {
            checkRootDir();
        } else {
            if (FileMgrTransfer.getsInstance().isTransfering()
                    || FileMgrCore.isIgnoreFileWatch()) {
                return;
            }
            if (event.getEventType() == FileMgrFileEvent.EVENT_TYPE_FILECHANGE) {
                String currentPath = getFullPathFromInfo(mCurrentPath);
                String eventPath = event.getFilePath();
                File eventFile = new File(eventPath);
                if (eventFile.getParent().equalsIgnoreCase(currentPath)) {
                    gotoPath(mCurrentPath);
                }
            } else if (event.getEventType() == FileMgrFileEvent.EVENT_TYPE_FILECHANGE_EVENT_LIST) {
                KbxFileChangeEvent[] eventList = event.getEventList();
                String currentPath = getFullPathFromInfo(mCurrentPath);
                for (KbxFileChangeEvent ev : eventList) {
                    File eventFile = new File(ev.getPath());
                    if (eventFile.getParent().equalsIgnoreCase(currentPath)) {
                        gotoPath(mCurrentPath);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void resetFooterBarMenu() {
        super.resetFooterBarMenu();
        if (isRootPath()) {
            mFooterBarContainer.setVisibility(View.INVISIBLE);
        }

        if (mCurrentMode == Mode.Normal &&
            FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.Empty) {
            if (FileMgrCore.isScanned()) {
                boolean enable = mListAdapter.getCount() != 0;
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_SORT, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_COPY, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_DELETE, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_CUT, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_SHARE, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_ZIP, enable);
            }
        }
    }

    @Override
    protected void onListViewPullDownFromTop(MotionEvent event, int delta) {
        if (!isRootPath()) {
            super.onListViewPullDownFromTop(event, delta);
        }
    }

    private void showLoadDialog() {
        // YUNOS BEGIN
        // BugID:8137550
        // Date:2016/04/14 Author:jessie.yj@alibaba-inc.com
        if(!NewFolderDialog.isShowing()){
            mLoadDialog = new MessageDialog(mActivity);
            mLoadDialog.setMessage(mActivity.getString(R.string.ali_read_file));
            mLoadDialog.show();
        }
        // YUNOS END
    }

    private void cancelLoadDialog() {
        if (mLoadDialog != null) {
            mLoadDialog.dismiss();
        }
    }

    private void showProgressDialog(String message, int taskCount) {
        mProgressDialog = new ProgressDialog(mActivity, message);
        mProgressDialog.setTaskCount(taskCount);
        mProgressDialog.setMessage(mActivity.getString(R.string.downloading));
        mProgressDialog.setCancelDelegate(new ProgressDialog.ProgressDialogDelegate() {
            @Override
            public void onCancel() {
                FileMgrTransfer.getsInstance().cancelDownloadTask();
                BusProvider.getInstance().post(new FileMgrLocalEvent());
            }
        });
        mProgressDialog.show();
    }

    private void checkNetwork(boolean isUpload) {
        int networkType = FileMgrNetworkInfo.getsInstance().getNetworkType();
        if (networkType == FileMgrNetworkInfo.NETWORK_OTHER) {
            Toast.makeText(mActivity, R.string.network_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadCloudFiles(final List<FileInfo> cloudFiles, final String rootPath, final String downloadPath) {
        FileMgrNetworkInfo.getsInstance().showNetworkIsAvailable(mActivity, false, new FileMgrNetworkInfo.NetworkInfoListener() {
            @Override
            public void onOK() {
                Log.d(TAG, "downloadCloudFiles");
                showProgressDialog(mActivity.getString(R.string.downloading), cloudFiles.size());
                mDelegate = new FileMgrTransfer.FileMgrTransferDelegate() {
                    @Override
                    public void onComplete() {
                        FileMgrClipboard.getInstance().finishClipboard();
                        gotoPath(mCurrentPath);
                        mHandle.sendEmptyMessage(MSG_CLOSE_PROGRESS_DIALOG);
                    }

                    @Override
                    public void onCancel() {
                        FileMgrClipboard.getInstance().finishClipboard();
                        gotoPath(mCurrentPath);
                        BusProvider.getInstance().post(new FileMgrLocalEvent());
                        mHandle.sendEmptyMessage(MSG_CLOSE_PROGRESS_DIALOG);
                    }

                    @Override
                    public void onError(int errorCode) {
                        FileMgrClipboard.getInstance().finishClipboard();
                        gotoPath(mCurrentPath);
                        BusProvider.getInstance().post(new FileMgrLocalEvent());
                        mHandle.sendEmptyMessage(MSG_CLOSE_PROGRESS_DIALOG);
                        checkNetwork(false);
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
                FileMgrTransfer.getsInstance().downloadCloudFiles(cloudFiles, rootPath, downloadPath, mDelegate);
            }

            @Override
            public void onForbid() {

            }
        });
    }

    private void cutCloudFiles(final List<FileInfo> cloudFiles, final String rootPath, final String downloadPath) {
        FileMgrNetworkInfo.getsInstance().showNetworkIsAvailable(mActivity, false, new FileMgrNetworkInfo.NetworkInfoListener() {
            @Override
            public void onOK() {
                Log.d(TAG, "cutCloudFiles");
                showProgressDialog(mActivity.getString(R.string.downloading), cloudFiles.size());
                mDelegate = new FileMgrTransfer.FileMgrTransferDelegate() {
                    @Override
                    public void onComplete() {
                        // 刷新本地文件
                        FileMgrClipboard.getInstance().finishClipboard();
                        gotoPath(mCurrentPath);
                        // 下载完成，删除云端文件
                        mHandle.sendEmptyMessage(MSG_CLOSE_PROGRESS_DIALOG);
                        Message msg = new Message();
                        msg.what = MSG_SHOW_MESSAGE_DIALOG;
                        msg.obj = mActivity.getString(R.string.deleting);
                        mHandle.sendMessage(msg);
                        List<KbxSetFileListRequest.FileData> fileDatas = new ArrayList<KbxSetFileListRequest.FileData>();
                        for (FileInfo fileInfo : cloudFiles) {
                            KbxSetFileListRequest.FileData fileData = new KbxSetFileListRequest.FileData();
                            fileData.fileName = fileInfo.getName();
                            fileData.filePath = ((CloudFileInfo)fileInfo).getParentPath();
                            fileData.isDir = fileInfo.isFolder();
                            fileDatas.add(fileData);
                        }
                        CloudFileMgr.getsInstance().deleteFiles(fileDatas.toArray(new KbxSetFileListRequest.FileData[fileDatas.size()]),
                                new KbxRequest<KbxDefaultResponse>(new KbxResponse<KbxDefaultResponse>() {
                                    @Override
                                    public void onResponse(KbxDefaultResponse response) {
                                        mHandle.sendEmptyMessage(MSG_CLOSE_PROGRESS_DIALOG);
                                        BusProvider.getInstance().post(new FileMgrCloudEvent(FileMgrCloudEvent.CLOUD_EVENT_CUT_COMPLETE));
                                        if (response.getErrorNo() == 0) {
                                            Log.d(TAG, "delete cut files success");
                                        } else {
                                            Log.d(TAG, "delete cut files failed: " + response.getErrorNo());
                                        }
                                    }
                                }));
                    }

                    @Override
                    public void onCancel() {
                        // 刷新本地文件
                        FileMgrClipboard.getInstance().finishClipboard();
                        gotoPath(mCurrentPath);
                        BusProvider.getInstance().post(new FileMgrLocalEvent());
                        mHandle.sendEmptyMessage(MSG_CLOSE_PROGRESS_DIALOG);
                    }

                    @Override
                    public void onError(int errorCode) {
                        // 刷新本地文件
                        FileMgrClipboard.getInstance().finishClipboard();
                        gotoPath(mCurrentPath);
                        BusProvider.getInstance().post(new FileMgrLocalEvent());
                        mHandle.sendEmptyMessage(MSG_CLOSE_PROGRESS_DIALOG);
                        checkNetwork(false);
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
                FileMgrTransfer.getsInstance().downloadCloudFiles(cloudFiles, rootPath, downloadPath, mDelegate);
            }

            @Override
            public void onForbid() {

            }
        });
    }
}
