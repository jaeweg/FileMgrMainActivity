package com.aliyunos.filemanager.ui.cloud;

import android.content.Context;

import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.util.FormatUtil;
import com.kanbox.filemgr.KbxLocalFile;
import com.kanbox.sdk.filelist.model.KbxFile;

/**
 * Created by sjjwind on 9/24/15.
 */
public class CloudFileInfo extends FileInfo {
    public KbxFile cloudFile;
    private String mMetaInfo;
    private boolean mIsDownloaded;
    public CloudFileInfo(KbxFile file) {
        super();
        cloudFile = file;
    }

    public CloudFileInfo(KbxFile file, boolean isDownloaded) {
        super();
        cloudFile = file;
        mIsDownloaded = isDownloaded;
    }
    public String getFileId() { return cloudFile.getFileId(); }
    public String getParentPath() { return cloudFile.getFilePath(); }
    public boolean getIsDownloaded() { return mIsDownloaded; }
    public KbxFile getCloudFile() { return cloudFile; }
    @Override
    public String getPath() { return cloudFile.getFullPath(); }
    @Override
    public String getName() { return cloudFile.getFileName(); }
    @Override
    public String getAppName() { return ""; }
    @Override
    public long getSize() { return cloudFile.getFileSize(); }
    @Override
    public KbxLocalFile.FileType getType() { return FileMgrCore.getFileTypeByPath(cloudFile.getFullPath()); }
    @Override
    public boolean isFolder() { return cloudFile.getIsDir(); }
    @Override
    public long getLastModifyTime() { return cloudFile.getLastModifyTime() * 1000; }
    @Override
    public String getMetaInfo(Context context) {
        if (mMetaInfo == null) {
            if (isFolder()) {
                mMetaInfo = FormatUtil.formatTime(context, getLastModifyTime());
            } else {
                mMetaInfo = FormatUtil.formatTime(context, getLastModifyTime())
                        + " " + FormatUtil.formatCapacitySize(getSize());
            }
        }
        return mMetaInfo;
    }
}