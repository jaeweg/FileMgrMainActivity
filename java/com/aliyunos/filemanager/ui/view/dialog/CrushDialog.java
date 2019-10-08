package com.aliyunos.filemanager.ui.view.dialog;

import android.content.Context;
import android.graphics.Color;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrCore;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

public class CrushDialog {
    private String mFilePath;
    private AlertDialog mAlertDialog;

    public CrushDialog(final Context context, String filePath) {
        mFilePath = filePath;
        mAlertDialog =  new AlertDialog.Builder(context)
                .setMessage(R.string.CrushFileWarnning)
                .setPositiveButton(R.string.actions_menu_Crush, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FileMgrCore.crushFile(mFilePath, context);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();
    }

    public void show() {
        mAlertDialog.show();
        mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }

    public static void showDialog(Context context, String filePath) {
        new CrushDialog(context, filePath).show();
    }
}
