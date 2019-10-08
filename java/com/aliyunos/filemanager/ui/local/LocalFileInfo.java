package com.aliyunos.filemanager.ui.local;

import android.content.Context;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.util.FormatUtil;
import com.kanbox.filemgr.KbxLocalFile;
import com.kanbox.filemgr.KbxLocalFileManager;

/**
 * Created by sjjwind on 9/24/15.
 */
public class LocalFileInfo extends FileInfo {
    public KbxLocalFile localFile;
    private String mMetaInfo;

    public LocalFileInfo(KbxLocalFile file) {
        super();
        localFile = file;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public KbxLocalFile getLocalFile() { return localFile; }

    public int getSubfileCount() { return localFile.getSubfileCount(); }
    @Override
    public String getPath() { return localFile.getFilePath(); }
    @Override
    public String getName() { return localFile.getShowName(); }
    @Override
    public String getAppName() { return localFile.getAppName(); }
    @Override
    public long getSize() { return localFile.getFileSize(); }
    @Override
    public KbxLocalFile.FileType getType() { return localFile.getFileType(); }
    @Override
    public boolean isFolder() { return localFile.getIsFolder(); }
    @Override
    public long getLastModifyTime() { return localFile.getLastModifyTime(); }
    @Override
    public String getMetaInfo(Context context) {
        if (mMetaInfo == null) {
            if (isFolder()) {
                int subCount = getSubfileCount();
                if (!isRoot()) {
                    if (subCount != 0) {
                        mMetaInfo = FormatUtil.formatTime(context, getLastModifyTime())
                                + " " + subCount + context.getResources().getText(R.string.ali_item);
                    } else {
                        mMetaInfo = FormatUtil.formatTime(context, getLastModifyTime())
                                + " " + context.getResources().getText(R.string.ali_emptyfolder);
                    }
                } else {
                    if(subCount !=0) {
                        mMetaInfo = "" + subCount + context.getResources().getText(R.string.ali_item);

                    } else {
                        mMetaInfo = context.getResources().getText(R.string.ali_emptyfolder).toString();
                    }
                }
            } else {
                mMetaInfo = FormatUtil.formatTime(context, getLastModifyTime())
                        + " " + FormatUtil.formatCapacitySize(getSize());
            }
        }
        return mMetaInfo;
    }
    private boolean isRoot() {
        KbxLocalFile[] rootFileList = KbxLocalFileManager.getRootFolderList();
        String path = getPath();
        for (KbxLocalFile root : rootFileList) {
            if (getPath().equals(root.getFilePath())) {
                return true;
            }
        }
        return false;
    }
}