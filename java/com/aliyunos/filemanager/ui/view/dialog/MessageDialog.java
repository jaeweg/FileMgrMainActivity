package com.aliyunos.filemanager.ui.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

import com.aliyunos.filemanager.R;

public class MessageDialog {
    private Dialog mDialog;
    public MessageDialog(Context ctx) {
        mDialog = new Dialog(ctx);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.getWindow().setBackgroundDrawableResource(R.drawable.loading_overall_bg);
        mDialog.setContentView(R.layout.message_dialog);
        mDialog.setCancelable(false);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public void setMessage(String message) {
        TextView textView = (TextView)mDialog.findViewById(R.id.message_view);
        textView.setText(message);
    }
    public void show(){
        mDialog.show();
    }
    public void dismiss(){
        mDialog.dismiss();
    }
    public boolean isShowing(){
        return mDialog.isShowing();
    }
}
