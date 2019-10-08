package com.aliyunos.filemanager.ui.view.dialog;

import android.content.Context;

import com.aliyunos.filemanager.R;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;

/**
 * Created by mengzheng on 15/10/14.
 */
    public class ChangeExtDialog {
    public interface ChangeExtDialogListener {
        void onConfirmClicked();
        void onCancelClicked();
    }

    private ChangeExtDialogListener mListener = null;
    private AlertDialog mAlertDialog;

    public ChangeExtDialog(Context context, ChangeExtDialogListener listener) {
        mListener = listener;
        mAlertDialog =  new AlertDialog.Builder(context)
                .setMessage(R.string.change_ext_alert_info)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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
    }

    public static void showDialog(Context context, ChangeExtDialogListener listener) {
        new ChangeExtDialog(context, listener).show();
    }
}
