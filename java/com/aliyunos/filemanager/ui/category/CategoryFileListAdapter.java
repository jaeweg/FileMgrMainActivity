package com.aliyunos.filemanager.ui.category;

import android.content.Context;

import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.ui.local.LocalFileListAdapter;
import com.kanbox.filemgr.KbxLocalFileManager;

public class CategoryFileListAdapter extends LocalFileListAdapter {

    public CategoryFileListAdapter(Context ctx) {
        super(ctx);
    }

    public void setCategoryType(KbxLocalFileManager.CategoryType categoryType) {
        setFileInfoList(convertToFileInfo(FileMgrCore.getCategoryFileList(categoryType)));
    }

    public boolean setCategoryTypeWithKeepSelectMode(KbxLocalFileManager.CategoryType categoryType) {
        return setFileInfoListWithKeepSelectMode(convertToFileInfo(FileMgrCore.getCategoryFileList(categoryType)));
    }
}
