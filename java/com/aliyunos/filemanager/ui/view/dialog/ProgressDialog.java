package com.aliyunos.filemanager.ui.view.dialog;


import com.aliyunos.filemanager.R;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface;
import hwdroid.dialog.DialogInterface.OnCancelListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;

@SuppressLint("HandlerLeak")
public class ProgressDialog  {
    private CharSequence mMessage;
    private ProgressBar mProgressView;
    private TextView mMessageView;
    private TextView mProgressNumberView;

    private Handler mViewUpdateHandler;

    private String mProgressNumberFormat;
    private String mActionString;
    private int mTaskCount = 0;
    private int mCurrentTaskIndex = -1;
    private static int mSingleTaskMax = 100;
    AlertDialog mAlertDialog;

    private static final int MSG_UPDATE_TASK_COUNT = 0;
    private static final int MSG_UPDATE_CONTENT = 1;
    private static final int MSG_UPDATE_PROGRESS = 2;
    private static final int MSG_UPDATE_CURRENT_TASK = 3;
    private Handler mHandle = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_TASK_COUNT:
                    Integer taskCount = (Integer)msg.obj;
                    setTaskCount(taskCount.intValue());
                    break;
                case MSG_UPDATE_CONTENT:
                    String content = (String)msg.obj;
                    setMessage(content);
                    break;
                case MSG_UPDATE_PROGRESS:
                    Double progress = (Double)msg.obj;
                    if (progress != null) {
                        setProgress(progress.doubleValue());
                    }
                    break;
                case MSG_UPDATE_CURRENT_TASK:
                    Integer taskIndex = (Integer)msg.obj;
                    syncTaskInfo(taskIndex);
            }
        }
    };

    public static interface ProgressDialogDelegate {
        void onCancel();
    }

    private ProgressDialogDelegate mDelegate;

    public ProgressDialog(Context context,String message) {
        mActionString = message;
        initFormats();


        AlertDialog.Builder  mBuilder = new AlertDialog.Builder(context);

        mAlertDialog = mBuilder.create();
        mAlertDialog.setCanceledOnTouchOutside(false);
        onCreate(context);
    }

    private void initFormats() {
        mProgressNumberFormat = "(%d/%d)";
    }

    public void setMessage(CharSequence message) {
        mMessage = message;

        if(null != mMessageView) {
            mMessageView.setText(mMessage);
        }
    }

    protected void onCreate(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View contentView = inflater.inflate(R.layout.progress_dialog, null);
        mProgressView = (ProgressBar)contentView.findViewById(R.id.progress);
        mMessageView = (TextView)contentView.findViewById(R.id.progress_message);
        mProgressNumberView = (TextView)contentView.findViewById(R.id.progress_percent);
        contentView.setMinimumWidth(10000);
        mViewUpdateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                /* Update the number and percent */
                syncTaskInfo(mCurrentTaskIndex);
            }
        };

        mAlertDialog.setView(contentView);

        mAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mDelegate != null) {
                    mDelegate.onCancel();
                }
            }
        });

        mAlertDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mDelegate != null) {
                    mDelegate.onCancel();
                }
            }
        });

    }

    public void setProgress(double progress) {
        if (mTaskCount == 1 && mProgressView != null) {
            int iProgress = (int)(mSingleTaskMax * progress);
            iProgress = (iProgress > 100) ? 100 : iProgress;
            mProgressView.setProgress(iProgress);
            mProgressNumberView.setText(mActionString
                    + '(' + iProgress +"%)");
        }
    }

    public void increateCurrentTask() {
        mCurrentTaskIndex++;
        syncTaskInfo(mCurrentTaskIndex);
    }

    public void setTaskCount(int taskCount) {
        if (mTaskCount == taskCount) {
            return;
        }
        if (mProgressView != null) {
            mTaskCount = taskCount;
            if (taskCount == 1) {
                mProgressView.setMax(mSingleTaskMax);

            } else {
                mProgressView.setMax(taskCount);
            }

            syncTaskInfo(mCurrentTaskIndex);
            onProgressChanged();
        }
    }

    public void syncTaskInfo(int currentTask) {
        if (mTaskCount == 1) {
            int progress = mProgressView.getProgress();
            mProgressNumberView.setText(mActionString
                    + '(' + progress +"%)");
        }
        else if (currentTask >= 0 && currentTask <= mTaskCount) {
            String format = mProgressNumberFormat;
            mProgressNumberView.setText(mActionString + String.format(format, currentTask, mTaskCount));
            mProgressView.setProgress(currentTask);
        }
    }

    public boolean isFinished() {
    	Log.i("huangjiawei", "mCurrentTaskIndex == " + mCurrentTaskIndex);
    	Log.i("huangjiawei", "mTaskCount == " + mTaskCount);
        return mCurrentTaskIndex == mTaskCount;
    }

    private void onProgressChanged() {
        if(mViewUpdateHandler != null && !mViewUpdateHandler.hasMessages(0)) {
            mViewUpdateHandler.sendEmptyMessage(0);
        }
    }

    public void updateProgressDialogContent(String content) {
        Message msg = new Message();
        msg.what = MSG_UPDATE_CONTENT;
        msg.obj = content;
        mHandle.sendMessage(msg);
    }

    public void updateProgress(double progress) {
        Message msg = new Message();
        msg.what = MSG_UPDATE_PROGRESS;
        msg.obj = new Double(progress);
        mHandle.sendMessage(msg);
    }


    public void updateTaskCount(int taskCount) {
        if (mTaskCount == taskCount) {
            return;
        }
        mTaskCount = taskCount;
        Message msg = new Message();
        msg.what = MSG_UPDATE_TASK_COUNT;
        msg.obj = Integer.valueOf(taskCount);
        mHandle.sendMessage(msg);
    }

    public void updateCurrentTask() {
        Message msg = new Message();
        msg.what = MSG_UPDATE_CURRENT_TASK;
        msg.obj = Integer.valueOf(mCurrentTaskIndex);
        mHandle.sendMessage(msg);
    }

    public void finishCurrentTask() {
        mCurrentTaskIndex ++;
        syncTaskInfo(mCurrentTaskIndex);
    }

    public void setCancelDelegate(ProgressDialogDelegate delegate) {
        mDelegate = delegate;
    }

    public void show() {
        // TODO Auto-generated method stub
        mAlertDialog.show();
    }

    public void setOnCancelListener(OnCancelListener onCancelListener) {
        // TODO Auto-generated method stub
        mAlertDialog.setOnCancelListener(onCancelListener);
    }

    public boolean isShowing() {
        // TODO Auto-generated method stub
        return mAlertDialog.isShowing();
    }

    public void dismiss() {
        // TODO Auto-generated method stub
        mAlertDialog.dismiss();
    }
}
