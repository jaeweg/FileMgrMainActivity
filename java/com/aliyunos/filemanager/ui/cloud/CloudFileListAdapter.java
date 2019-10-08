package com.aliyunos.filemanager.ui.cloud;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.aliyunos.filemanager.core.FileMgrThumbnailLoader;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListItem;
import com.aliyunos.filemanager.ui.view.FileListItemView;
import com.aliyunos.filemanager.ui.view.FileListViewAdapter;
import com.aliyunos.filemanager.util.GraphicUtil;
import com.kanbox.filemgr.KbxLocalFileManager;
import com.kanbox.sdk.filelist.model.KbxFile;

public class CloudFileListAdapter extends FileListViewAdapter {
    public CloudFileListAdapter(Context ctx) {
        super(ctx);
        mAlphaShowHidden = false;
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
        FileMgrThumbnailLoader.getInstance().loadCloudThumnail(convertView, item, fileInfo.getPath(),
                ((CloudFileInfo)fileInfo).getFileId(), thumbnailSize, thumbnailSize, this);
    }

    public FileInfo[] convertToFileInfo(KbxFile[] cloudFiles) {
        if (cloudFiles == null) {
            return null;
        }
        FileInfo[] fileInfos = new FileInfo[cloudFiles.length];
        for (int i = 0; i < cloudFiles.length; i++) {
            KbxFile cloudFile = cloudFiles[i];
            FileInfo fileInfo;
            if (!cloudFile.getIsDir()) {
                boolean isDownloaded = KbxLocalFileManager.isDownloaded(cloudFile.getFullPath());
                fileInfo = new CloudFileInfo(cloudFile, isDownloaded);
            } else {
                fileInfo = new CloudFileInfo(cloudFile);
            }
            fileInfos[i] = fileInfo;
        }
        return fileInfos;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        int itemPosition = position;
        if (mIsShowSearchButton) {
            if (position == 0) {
                return view;
            } else {
                itemPosition -= 1;
            }
        }

        FileInfo[] fileInfoList = getFileInfoList();
        FileInfo fileInfo = null;

        if (itemPosition < fileInfoList.length) {
            fileInfo = fileInfoList[itemPosition];
        }
        if (fileInfo != null && !fileInfo.isFolder()) {
            if (((CloudFileInfo) fileInfo).getIsDownloaded()) {
                ((FileListItemView) view).setIsDownloaded(true);
            } else {
                ((FileListItemView) view).setIsDownloaded(false);
            }
        }

        return view;
    }
}
