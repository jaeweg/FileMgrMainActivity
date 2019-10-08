/*
 * Copyright (C) 2013 The Aliyunos Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aliyunos.filemanager.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.text.TextUtils;
import android.util.Log;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrTransfer;
import com.aliyunos.filemanager.ui.local.LocalFileInfo;
import com.aliyunos.filemanager.ui.local.LocalFileListAdapter;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListViewAdapter;
import com.kanbox.filemgr.KbxLocalFileManager;

import java.io.File;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.widget.ActionSheet;

/**
 * A helper class with useful methods to extract media data.
 */
public final class MediaUtil{

    private static final String TAG = "MediaUtil";

    private static final String EMULATED_STORAGE_SOURCE = System
            .getenv("EMULATED_STORAGE_SOURCE");
    private static final String EMULATED_STORAGE_TARGET = System
            .getenv("EMULATED_STORAGE_TARGET");
    private static final String EXTERNAL_STORAGE = System
            .getenv("EXTERNAL_STORAGE");

    private static final String INTERNAL_VOLUME = "internal";
    private static final String EXTERNAL_VOLUME = "external";

    static long double_click_time = 0;
    static long click_time = 0;

    /**
     *
     * @param ctx
     * @param fileInfo
     */
    public static void openMediaByMime(final Context ctx,
                                       FileInfo fileInfo, final String mime) {
        double_click_time = System.currentTimeMillis();
        double_click_time = double_click_time-click_time;
        click_time = System.currentTimeMillis();
        if (double_click_time<600)
        {
            return;
        }

        try {
            // Create the intent to open the file
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);

            String filePath;
            if (fileInfo instanceof LocalFileInfo) {
                filePath = fileInfo.getPath();
            } else {
                filePath = KbxLocalFileManager.getDownloadedFile(fileInfo.getPath()).getFilePath();
                if(filePath.isEmpty() && fileInfo.getSize() == 0){
                    filePath = FileMgrTransfer.getsInstance().getSingleDownloadPath(fileInfo.getPath());
                }
            }
            File file = new File(filePath);
            if(!file.exists()) {
                final String showMsg = "";
                if (!file.canRead()) {
                    new Handler(ctx.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            Log.v("rwei", "openFileSystemObject:111");
                            AlertDialog a = new AlertDialog.Builder(ctx)
                                    .setTitle(R.string.ali_openfile_error12)
                                    .setNegativeButton(R.string.ali_iknow, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                /* User clicked cancel so do some stuff */
                                        }
                                    }).create();

                            a.show();
                        }

                    });
                } else {
                    new Handler(ctx.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            Log.v("rwei", "openFileSystemObject:111");
                            AlertDialog a = new AlertDialog.Builder(ctx)
                                    .setTitle(R.string.ali_openfile_error11)
                                    .setNegativeButton(R.string.ali_iknow, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                /* User clicked cancel so do some stuff */
                                        }
                                    }).create();

                            a.show();
                        }

                    });
                }
                return;
            }
            if (mime != null) {
            	Log.i("huangjiawei", "MediaUtil--openMediaByMime.file == " + file);
            	Log.i("huangjiawei", "MediaUtil--openMediaByMime.filePath == " + filePath);
            	intent.setDataAndType(getUriFromFile(ctx, file), mime);
            } else {
                showSelectMimeTypeDialog(ctx,getUriFromFile(ctx, file));
                return ;
            }

            ctx.startActivity(intent);
        } catch (Exception e) {
            new Handler(ctx.getMainLooper()).post(new Runnable()
            {
                @Override
                public void run() {
                    // TODO Auto-generated method stub

                    AlertDialog a = new AlertDialog.Builder(ctx)
                            .setMessage(R.string.ali_openfile_error)
                            .setPositiveButton(R.string.ali_iknow, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                            /* User clicked cancel so do some stuff */
                                }
                            }).create();
                    a.show();
                }

            });
        }
    }

    /**
     * Method that returns the best Uri for the file (content uri, file uri,
     * ...)
     *
     * @param ctx
     *            The current context
     * @param file
     *            The file to resolve
     */
    private static Uri getUriFromFile(Context ctx, File file) {
        ContentResolver cr = ctx.getContentResolver();
        Uri uri = MediaUtil.fileToContentUri(cr, file);
        if (uri == null) {
            uri = Uri.fromFile(file);
        }
        Log.i("huangjiawei", "MediaUtil--getUriFromFile == " + uri);
        return uri;
    }
    private static void showSelectMimeTypeDialog(Context mActivity,final Uri data)
    {
        final Context mContext = mActivity;
        ActionSheet actionSheet = new ActionSheet(mActivity);
        actionSheet.setSingleChoiceItems(
                mContext.getResources().getStringArray(R.array.mime_type_item),
                -1,
                new ActionSheet.SingleChoiceListener() {
                    @Override
                    public void onDismiss(ActionSheet actionSheet) {
                    }
                    @Override
                    public void onClick(int position) {
                        int which = position;
                        String mime = null;
                        String[] items = mContext.getResources().getStringArray(
                                R.array.mime_type_item);
                        if (items[which].equalsIgnoreCase(mContext.getResources().getString(
                                R.string.ali_open_mimetype_text))) {
                            mime = "text/*";
                        }
                        else if (items[which].equalsIgnoreCase(mContext.getResources().getString(
                                R.string.ali_open_mimetype_audio))) {
                            mime = "audio/*";
                        }
                        else if (items[which].equalsIgnoreCase(mContext.getResources().getString(
                                R.string.ali_open_mimetype_video))) {
                            mime = "video/*";
                        }
                        else if (items[which].equalsIgnoreCase(mContext.getResources().getString(
                                R.string.ali_open_mimetype_image))) {
                            mime = "image/*";
                        }
                        try{
                            Intent intent = new Intent();
                            intent.setAction(android.content.Intent.ACTION_VIEW);
                            intent.setDataAndType(data, mime);
                            mContext.startActivity(intent);
                        }
                        catch (Exception e) {
                            new Handler(mContext.getMainLooper()).post(new Runnable()
                            {
                                @Override
                                public void run() {
                                    // TODO Auto-generated method stub

                                    AlertDialog a = new AlertDialog.Builder(mContext)
                                            .setTitle(R.string.ali_openfile_error)
                                            .setNegativeButton(R.string.ali_iknow, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                    /* User clicked cancel so do some stuff */
                                                }
                                            }).create();
                                    a.show();

                                }

                            });
                        }
                    }
                });

        actionSheet.showWithDialog();
    }


    /**
     * Method that converts a file reference to a content uri reference
     *
     * @param cr
     *            A content resolver
     * @param file
     *            The file reference
     * @return Uri The content uri or null if file not exists in the media
     *         database
     */
    public static Uri fileToContentUri(ContentResolver cr, File file) {
        // Normalize the path to ensure media search
        final String normalizedPath = normalizeMediaPath(file.getAbsolutePath());

        // Check in external and internal storages
        Uri uri = fileToContentUri(cr, normalizedPath, EXTERNAL_VOLUME);
        if (uri != null) {
            return uri;
        }
        uri = fileToContentUri(cr, normalizedPath, INTERNAL_VOLUME);
        if (uri != null) {
            return uri;
        }
        return null;
    }

    /**
     * Method that converts a file reference to a content uri reference
     *
     * @param cr
     *            A content resolver
     * @param path
     *            The path to search
     * @param volume
     *            The volume
     * @return Uri The content uri or null if file not exists in the media
     *         database
     */
    private static Uri fileToContentUri(ContentResolver cr, String path,
            String volume) {
        final String[] projection = { BaseColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE };
        final String where = MediaColumns.DATA + " = ?";
        Uri baseUri = MediaStore.Files.getContentUri(volume);
        Cursor c = cr.query(baseUri, projection, where, new String[] { path },
                null);
        try {
            if (c != null && c.moveToNext()) {
                int type = c
                        .getInt(c
                                .getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE));
                if (type != 0) {
                    // Do not force to use content uri for no media files
                    long id = c.getLong(c
                            .getColumnIndexOrThrow(BaseColumns._ID));
                    return Uri.withAppendedPath(baseUri, String.valueOf(id));
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return null;
    }


    /**
     * Method that converts a not standard media mount path to a standard media
     * path
     *
     * @param path
     *            The path to normalize
     * @return String The normalized media path
     */
    public static String normalizeMediaPath(String path) {
        // Retrieve all the paths and check that we have this environment vars
        if (TextUtils.isEmpty(EMULATED_STORAGE_SOURCE)
                || TextUtils.isEmpty(EMULATED_STORAGE_TARGET)
                || TextUtils.isEmpty(EXTERNAL_STORAGE)) {
            return path;
        }

        // We need to convert EMULATED_STORAGE_SOURCE -> EMULATED_STORAGE_TARGET
        if (path.startsWith(EMULATED_STORAGE_SOURCE)) {
            path = path.replace(EMULATED_STORAGE_SOURCE,
                    EMULATED_STORAGE_TARGET);
        }
        // We need to convert EXTERNAL_STORAGE -> EMULATED_STORAGE_TARGET /
        // userId
        if (path.startsWith(EXTERNAL_STORAGE)) {
            final String userId = String.valueOf(Process.myUid());
            final String target = new File(EMULATED_STORAGE_TARGET, userId)
                    .getAbsolutePath();
            path = path.replace(EXTERNAL_STORAGE, target);
        }
        return path;
    }
}
