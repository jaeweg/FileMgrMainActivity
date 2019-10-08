package com.aliyunos.filemanager.ui.local;

import android.content.Context;
import android.view.View;

import com.aliyunos.filemanager.core.FileMgrThumbnailLoader;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListItem;
import com.aliyunos.filemanager.ui.view.FileListViewAdapter;
import com.aliyunos.filemanager.util.GraphicUtil;
import com.aliyunos.filemanager.util.PreferenceUtil;
import com.kanbox.filemgr.KbxLocalFile;

import java.util.ArrayList;

public class LocalFileListAdapter extends FileListViewAdapter {

    private FileInfo[] mFullFileList;

    public LocalFileListAdapter(Context ctx) {
        super(ctx);
        mAlphaShowHidden = true;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    @Override
    protected void loadThumbnail(View convertView, FileListItem item, FileInfo fileInfo) {
        if (mContext == null) {
            return;
        }
        Context context = mContext.get();
        if (context == null) {
            return;
        }
        int thumbnailSize = (int) GraphicUtil.convertDpToPixel(context, 40.0f);
        FileMgrThumbnailLoader.getInstance().loadLocalThumbnail(convertView, item, fileInfo.getPath(),
            thumbnailSize, thumbnailSize, this);
    }

    public FileInfo[] convertToFileInfo(KbxLocalFile[] localFiles) {
        if (localFiles == null) {
            return null;
        }

        boolean showHidden = PreferenceUtil.getBoolean(PreferenceUtil.Setting.SETTINGS_SHOW_HIDDEN);

        ArrayList<FileInfo> fileInfoList = new ArrayList<FileInfo>();
        for (int i = 0; i < localFiles.length; i++) {
            KbxLocalFile localFile = localFiles[i];

            if (localFile.getShowName().startsWith(".")) {
                if (!showHidden) {
                    continue;
                }
            }

            FileInfo fileInfo = new LocalFileInfo(localFile);
            fileInfoList.add(fileInfo);
        }
        return fileInfoList.toArray(new FileInfo[fileInfoList.size()]);
    }
}
