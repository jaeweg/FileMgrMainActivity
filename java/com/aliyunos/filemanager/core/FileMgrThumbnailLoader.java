package com.aliyunos.filemanager.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.aliyunos.filemanager.R;
import com.kanbox.filemgr.KbxGetLocalThumbnailListener;
import com.kanbox.filemgr.KbxLocalFile;
import com.kanbox.sdk.transfer.KbxThumbnailManager;

import java.util.HashMap;

public class FileMgrThumbnailLoader {
    private static FileMgrThumbnailLoader sInstance;
    public static FileMgrThumbnailLoader getInstance() {
        if (sInstance == null) {
            synchronized (FileMgrThumbnailLoader.class) {
                if (sInstance == null) {
                    sInstance = new FileMgrThumbnailLoader();
                }
            }
        }
        return sInstance;
    }

    public static void destroy() {
        getInstance().mLoaderMap.clear();
    }

    private HashMap<View, LoaderInfo> mLoaderMap = new HashMap<View, LoaderInfo>();

    class LoaderInfo {
        View itemView;
        Object obj;
        KbxGetLocalThumbnailListener listener;
        LoadThumbnailCallback callback;
        int width;
        int height;

        public LoaderInfo(final View itemView, Object o, int w, int h, LoadThumbnailCallback cb) {
            this.itemView = itemView;
            this.obj = o;
            this.callback = cb;
            this.width = w;
            this.height = h;
            this.listener = new KbxGetLocalThumbnailListener() {
                @Override
                public void onGetThumbnail(String path, Bitmap bitmap) {
                    LoaderInfo info = mLoaderMap.get(itemView);
                    if (info != null &&
                        info.obj == obj &&
                        info.listener == listener &&
                        info.callback == callback) {
                        callback.onLoadThumbnailComplete(itemView, obj, bitmap, width, height);
                    }
                    mLoaderMap.remove(itemView);
                }
            };
        }
    }

    public interface LoadThumbnailCallback {
        void onLoadThumbnailComplete(View itemView, Object obj, Bitmap bitmap, int width, int height);
    }

    public void removeLoadingKey(View itemView) {
        mLoaderMap.remove(itemView);
    }

    public void loadLocalThumbnail(View itemView, Object obj, String filePath,
                              int width, int height, LoadThumbnailCallback callback) {
        Bitmap cacheThumbnail = FileMgrCore.getThumbnailCache(filePath);
        if (cacheThumbnail != null) {
            callback.onLoadThumbnailComplete(itemView, obj,cacheThumbnail, width, height);
            mLoaderMap.remove(itemView);
            return;
        }

        LoaderInfo loaderInfo = new LoaderInfo(itemView, obj, width, height, callback);
        mLoaderMap.put(itemView, loaderInfo);

        FileMgrCore.getThumbnail(filePath, width, height, loaderInfo.listener);
    }

    public void loadCloudThumnail(View itemView, Object obj, String filePath, String fileId,
                                  int width, int height, LoadThumbnailCallback callback) {
        Bitmap cacheThumbnail = FileMgrCore.getCloudThumbnailCache(filePath, fileId, KbxThumbnailManager.ZoomType.ZOOMTYPE_256);
        if (cacheThumbnail != null) {
            callback.onLoadThumbnailComplete(itemView, obj, cacheThumbnail, width, height);
            mLoaderMap.remove(itemView);
            return;
        }
        LoaderInfo loaderInfo = new LoaderInfo(itemView, obj, width, height, callback);
        mLoaderMap.put(itemView, loaderInfo);

        FileMgrCore.getCloudThumbnail(filePath, fileId, KbxThumbnailManager.ZoomType.ZOOMTYPE_256, loaderInfo.listener);
    }

    public Drawable getDefaultThumbnailByType(Context ctx, KbxLocalFile.FileType type, String filename) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case Image:
                return ctx.getResources().getDrawable(R.drawable.ic_photo_normal);
            case Audio:
                return ctx.getResources().getDrawable(R.drawable.ic_music_normal);
            case Video:
                return ctx.getResources().getDrawable(R.drawable.ic_video_normal);
            case Zip:
                return ctx.getResources().getDrawable(R.drawable.ic_zip_normal);
            case Apk:
                return ctx.getResources().getDrawable(R.drawable.ic_apk_normal);
            case Doc:
                if (filename.endsWith("pdf")) {
                    return ctx.getResources().getDrawable(R.drawable.ic_pdf_normal);
                } else if (filename.endsWith("doc") || filename.endsWith("docx")) {
                    return ctx.getResources().getDrawable(R.drawable.ic_doc_normal);
                } else if (filename.endsWith("xls") || filename.endsWith("xlsx")) {
                    return ctx.getResources().getDrawable(R.drawable.ic_xls_normal);
                } else if (filename.endsWith("ppt") || filename.endsWith("pptx")) {
                    return ctx.getResources().getDrawable(R.drawable.ic_ppt_normal);
                } else {
                    return ctx.getResources().getDrawable(R.drawable.ic_txt_normal);
                }

            case Other:
                return ctx.getResources().getDrawable(R.drawable.ic_file_normal);
            case Folder:
                return null;
        }
        return ctx.getResources().getDrawable(R.drawable.ic_unknown_normal);
    }
}
