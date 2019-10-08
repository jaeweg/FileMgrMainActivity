package com.aliyunos.filemanager.ui.view;

import android.content.Context;

import com.kanbox.filemgr.KbxLocalFile;

/**
 * Created by sjjwind on 9/24/15.
 */
public abstract class FileInfo {
    private boolean mSelected;

    public FileInfo() {
        mSelected = false;
    }
    public abstract String getPath();
    public abstract String getName();
    public abstract String getAppName();
    public abstract long getSize();
    public abstract KbxLocalFile.FileType getType();
    public abstract boolean isFolder();
    public abstract long getLastModifyTime();
    public abstract String getMetaInfo(Context context);
    public boolean isSelected() { return mSelected; }
    public void setSelected(boolean selected) { mSelected = selected; }
}