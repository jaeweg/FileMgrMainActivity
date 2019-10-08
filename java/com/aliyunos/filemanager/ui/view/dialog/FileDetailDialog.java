package com.aliyunos.filemanager.ui.view.dialog;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.ui.local.LocalFileInfo;
import com.aliyunos.filemanager.ui.local.LocalFileListAdapter;
import com.aliyunos.filemanager.util.FormatUtil;
import com.kanbox.filemgr.KbxGetFileAttributeListener;
import com.kanbox.filemgr.KbxLocalFile;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

public class FileDetailDialog {
    private final static int MSG_FILE_SIZE_UPDATE = 1;
    private View mDialogView;
    private AlertDialog mAlertDialog;

    public FileDetailDialog(Context context, LocalFileInfo fileInfo) {
        mDialogView = LayoutInflater.from(context).inflate(R.layout.file_detail_dialog, null);

        TextView pathTextView = (TextView)mDialogView.findViewById(R.id.path_context);
        final TextView sizeTextView = (TextView)mDialogView.findViewById(R.id.size_context);
        TextView timeTextView = (TextView)mDialogView.findViewById(R.id.time_context);
        TextView readableTextView = (TextView)mDialogView.findViewById(R.id.readable_context);
        TextView writeableTextView = (TextView)mDialogView.findViewById(R.id.writeable_context);
        TextView hideTextView = (TextView)mDialogView.findViewById(R.id.hide_context);

        pathTextView.setText(fileInfo.getPath());
        sizeTextView.setText(FormatUtil.formatCapacitySize(fileInfo.getSize()));
        timeTextView.setText(FormatUtil.formatTime(context, fileInfo.getLastModifyTime()));

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_FILE_SIZE_UPDATE) {
                    Long size = (Long)msg.obj;
                    sizeTextView.setText(FormatUtil.formatCapacitySize(size));
                }
            }
        };

        KbxLocalFile.KbxLocalFileAttribute attr = fileInfo.localFile.getAttribute(new KbxGetFileAttributeListener() {
            @Override
            public void onProgress(long size) {
                handler.sendMessage(handler.obtainMessage(MSG_FILE_SIZE_UPDATE, Long.valueOf(size)));
            }

            @Override
            public void onComplete() {

            }
        });

        readableTextView.setText(attr.canRead() ? R.string.yes : R.string.no);
        writeableTextView.setText(attr.canWrite() ? R.string.yes : R.string.no);
        hideTextView.setText(fileInfo.getName().startsWith(".") ? R.string.yes : R.string.no);

        mAlertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.detail_title)
                .setView(mDialogView)
                .setNegativeButton(R.string.ali_iknow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //todo: stop getting attr task
                    }
                }).create();
    }

    public void show() {
        mAlertDialog.show();
    }

    public static void showDialog(Context context, LocalFileInfo fileInfo) {
        new FileDetailDialog(context, fileInfo).show();
    }
}
