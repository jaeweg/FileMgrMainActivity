package com.aliyunos.filemanager.ui.view.dialog;

import android.content.Context;
import android.graphics.Color;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrCore;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

public class DeleteDialog {

    public interface DeleteDialogListener {
        void onConfirmClicked();
        void onCancelClicked();
    }

    private DeleteDialogListener mListener = null;
    private AlertDialog mAlertDialog;

    public DeleteDialog(Context context, DeleteDialogListener listener) {
        mListener = listener;
        mAlertDialog =  new AlertDialog.Builder(context)
                .setMessage(R.string.DeleteFileWarnning)
                .setPositiveButton(R.string.actions_menu_Delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null) {
                            mListener.onConfirmClicked();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mListener != null) {
                            mListener.onCancelClicked();
                        }
                    }
                }).create();
    }

    public void show() {
        mAlertDialog.show();
        mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }

    public static void showDialog(Context context, DeleteDialogListener listener) {
        new DeleteDialog(context, listener).show();
    }
}
