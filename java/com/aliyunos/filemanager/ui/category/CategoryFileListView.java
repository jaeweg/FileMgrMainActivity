package com.aliyunos.filemanager.ui.category;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.aliyunos.filemanager.FileMgrMainActivity;
import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrCategoryEvent;
import com.aliyunos.filemanager.core.FileMgrClipboard;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.core.FileMgrScanEvent;
import com.aliyunos.filemanager.core.FileMgrTransfer;
import com.aliyunos.filemanager.core.FileMgrWatcher;
import com.aliyunos.filemanager.ui.local.LocalFileInfo;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListView;
import com.aliyunos.filemanager.ui.view.FileListViewAdapter;
import com.aliyunos.filemanager.ui.view.dialog.CrushDialog;
import com.aliyunos.filemanager.ui.view.dialog.DeleteDialog;
import com.aliyunos.filemanager.ui.view.dialog.FileDetailDialog;
import com.aliyunos.filemanager.ui.view.dialog.RenameDialog;
import com.aliyunos.filemanager.util.FileUtil;
import com.aliyunos.filemanager.util.FooterBarUtil;
import com.aliyunos.filemanager.util.MediaUtil;
import com.aliyunos.filemanager.util.PreferenceUtil;
import com.aliyunos.filemanager.util.ShareUtil;
import com.kanbox.filemgr.KbxLocalFile;
import com.kanbox.filemgr.KbxLocalFileManager;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.ArrayList;

import hwdroid.widget.ActionSheet;

public class CategoryFileListView extends FileListView {
    KbxLocalFileManager.CategoryType mCurrentCategoryType;

    public interface CategoryFileListCallback {
        void onRootClicked();
    }
    CategoryFileListCallback mCallback;

    public CategoryFileListView(Activity activity, CategoryFileListCallback callback) {
        super(activity);
        mCallback = callback;
        mItemLongClickIds = new int[] {
                FileUtil.FILE_OPERATOR_SHOW_DETAIL,
                FileUtil.FILE_OPERATOR_RENAME,
                FileUtil.FILE_OPERATOR_COPY,
                FileUtil.FILE_OPERATOR_DELETE,
                FileUtil.FILE_OPERATOR_CUT,
                FileUtil.FILE_OPERATOR_SHARE,
                FileUtil.FILE_OPERATOR_CRUSH
        };
    }

    public void init(ViewGroup container, CategoryFileListAdapter adapter) {
        super.init(container, adapter);
        if (FileMgrCore.isScanned()) {
        	FileMgrCore.doFirstScan(true);
		}
        setMainFooterBarItems(FooterBarUtil.getItemsByIds(new int[] {
                FileUtil.FILE_OPERATOR_SORT,
                FileUtil.FILE_OPERATOR_COPY,
                FileUtil.FILE_OPERATOR_DELETE,
                FileUtil.FILE_OPERATOR_CUT,
                FileUtil.FILE_OPERATOR_SHARE
        }));
        int sortType = PreferenceUtil.getInteger(PreferenceUtil.Setting.SETTINGS_CATEGORY_KEY_ORDER);
        mListAdapter.setSortMode(FileListViewAdapter.SortMode.values()[sortType]);
    }

    // show files with category type
    public void showFileList(KbxLocalFileManager.CategoryType categoryType){
        if (categoryType == null) {
            return;
        }

        mCurrentCategoryType = categoryType;
        String title = mActivity.getString(R.string.CategoryBrowse) + "/";
        switch (categoryType) {
            case Image:
                title += mActivity.getString(R.string.CategoryItemPic);
                break;
            case Audio:
                title += mActivity.getString(R.string.CategoryItemMusic);
                break;
            case Video:
                title += mActivity.getString(R.string.CategoryItemVideo);
                break;
            case Apk:
                title += mActivity.getString(R.string.CategoryItemApk);
                break;
            case Doc:
                title += mActivity.getString(R.string.CategoryItemText);
                break;
            case Zip:
                title += mActivity.getString(R.string.CategoryItemZip);
                break;
        }

        long curTime = System.currentTimeMillis();

        setCrumbsPath(title);
        Log.d("CategoryFileListView", "setCrumbsPath = " + (System.currentTimeMillis() - curTime));
        mListAdapter.showSearchButton(false);
        if (mCurrentMode == Mode.Selecting) {
            boolean isAllSelected = ((CategoryFileListAdapter)mListAdapter).setCategoryTypeWithKeepSelectMode(categoryType);
            Log.d("CategoryFileListView", "setCategoryType Selecting= " + (System.currentTimeMillis() - curTime));
            if (isAllSelected) {
                mRightSelectCheckBox.setChecked(true);
            } else {
                mRightSelectCheckBox.setChecked(false);
            }

        } else {
            ((CategoryFileListAdapter) mListAdapter).setCategoryType(categoryType);
            Log.d("CategoryFileListView", "setCategoryType normal= " + (System.currentTimeMillis() - curTime));
        }
    }

    @Override
    public void onFooterItemClick(View view, int id) {
        if (mCurrentMode == Mode.Selecting) {
            ArrayList<FileInfo> fileInfos = mListAdapter.getSelectedItems();
//            setMode(Mode.Normal);
            switch (id) {
                case FileUtil.FILE_OPERATOR_COPY:
                    setMode(Mode.Normal);
                    ((FileMgrMainActivity)mActivity).scrollToTab(FileMgrMainActivity.FragmentTab.LocalTab);
                    FileMgrClipboard.getInstance().copyString("");
                    FileMgrClipboard.getInstance().copyLocalFiles(fileInfos);
                    break;
                case FileUtil.FILE_OPERATOR_CUT:
                    setMode(Mode.Normal);
                    ((FileMgrMainActivity)mActivity).scrollToTab(FileMgrMainActivity.FragmentTab.LocalTab);
                    FileMgrClipboard.getInstance().cutLocalFiles(fileInfos);
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
            }
        } else {
            if (id == FileUtil.FILE_OPERATOR_SORT) {
                //Category的排序要特殊处理
                ActionSheet actionSheet = new ActionSheet(mActivity);
                actionSheet.setTitle(mActivity.getString(R.string.actions_menu_Sort));
                actionSheet.setSingleChoiceItems(new String[] {
                                mActivity.getString(R.string.ali_order_name),
                                mActivity.getString(R.string.ali_order_size),
                                mActivity.getString(R.string.ali_order_date),
                                mActivity.getString(R.string.ali_order_type)},
                        PreferenceUtil.getInteger(PreferenceUtil.Setting.SETTINGS_CATEGORY_KEY_ORDER),
                        new ActionSheet.SingleChoiceListener() {
                            @Override
                            public void onClick(int position) {
                                PreferenceUtil.setInteger(PreferenceUtil.Setting.SETTINGS_CATEGORY_KEY_ORDER, position);
                                mListAdapter.setSortMode(
                                        FileListViewAdapter.SortMode.values()[position]
                                );
                            }
                            @Override
                            public void onDismiss(ActionSheet actionSheet) {}
                        });
                actionSheet.show(view);

            } else {
                super.onFooterItemClick(view, id);
            }
        }
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
                FileMgrClipboard.getInstance().copyLocalFile(fileInfo);
                if (mCurrentMode == Mode.Searching) {
                    cancelSearchView();
                }
                ((FileMgrMainActivity)mActivity).scrollToTab(FileMgrMainActivity.FragmentTab.LocalTab);
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
                FileMgrClipboard.getInstance().cutLocalFile(fileInfo);
                if (mCurrentMode == Mode.Searching) {
                    cancelSearchView();
                }
                ((FileMgrMainActivity)mActivity).scrollToTab(FileMgrMainActivity.FragmentTab.LocalTab);
                break;
            case FileUtil.FILE_OPERATOR_SHARE:
                break;
            case FileUtil.FILE_OPERATOR_CRUSH:
                CrushDialog.showDialog(mActivity, fileInfo.getPath());
                break;
        }
    }

    @Override
    protected void resetFooterBarMenu() {
        super.resetFooterBarMenu();

        if (mCurrentMode == Mode.Normal &&
                FileMgrClipboard.getInstance().getMode() == FileMgrClipboard.ClipboardMode.Empty) {
            if (FileMgrCore.isScanned()) {
                boolean enable = mListAdapter.getCount() != 0;
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_SORT, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_COPY, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_DELETE, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_CUT, enable);
                mFooterBarMenu.setItemEnable(FileUtil.FILE_OPERATOR_SHARE, enable);
            }
        }
    }

    @Subscribe
    public void onCategoryFileChange(FileMgrCategoryEvent event) {
        if (FileMgrTransfer.getsInstance().isTransfering()
                || FileMgrCore.isIgnoreFileWatch()) {
            return;
        }


        if(FileMgrWatcher.getInstance().isNeedRefreshCategory()) {
            FileMgrWatcher.getInstance().setNeedRefreshCattegory(false);
            long curTime = System.currentTimeMillis();
            showFileList(mCurrentCategoryType);
            long consmeTime = System.currentTimeMillis() - curTime;
            Log.d("CategoryFieListView", "onCategoryFileChange consumeTime = " + consmeTime);
        }
    }

    @Override
    protected void onTagViewClick(String tag, String path) {
        if (tag.equals(mActivity.getString(R.string.CategoryBrowse))) {
            if (mCallback != null) {
                mCallback.onRootClicked();
            }
        }
    }

    @Subscribe
    public void onScanCompleted(FileMgrScanEvent event) {
        if (mContainer != null && mContainer.getVisibility() == View.VISIBLE) {
            showFileList(mCurrentCategoryType);
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
           return true;
        } else {
            String mime = ShareUtil.getMimeType(mActivity, fileInfo.getPath());
            if((mime != null)&&(mime.equalsIgnoreCase("application/zip"))) {
                if (mCurrentMode == Mode.Searching) {
                    cancelSearchView();
                }
                ((FileMgrMainActivity)mActivity).scrollToTab(FileMgrMainActivity.FragmentTab.LocalTab);
                FileMgrClipboard.getInstance().unzipLocalFile(fileInfo);
            }else {
                MediaUtil.openMediaByMime(mActivity, fileInfo, mime);
            }
        }

        return true;
    }
}
