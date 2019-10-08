
package com.aliyunos.filemanager.ui.view.dialog;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.Dialog;
import hwdroid.dialog.DialogInterface;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrClipboard;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListViewAdapter;
import com.aliyunos.filemanager.util.FileUtil;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

public class ZipDialog {
    private static final String TAG = "ZipDialog";
    private UIHandler mUIHandler;
    private WeakReference<Context> mContext;
    private View mDialogView;
    private EditText mEditText;
    private ProgressDialog mProgressDialog;

    static final int zip_finish_id = 1;
    static final int zip_ShowProgress_id = 2;
    static final int zip_NotSpace_id = 3;
    static final int zip_SetMessage_id = 4;

    static final String zip_ShowProgress_key_filename = "progress_filename";
    static final String zip_SetMessage_key = "setmessage";
    static final String zip_ShowProgress_key_precent = "setpercent";

    final ArrayList<FileInfo> selectedFileInfoList;
    String FileDir;
    Thread mThread;
    boolean bIsForceStop = false;
    long systemtime = System.currentTimeMillis();
    long zippedCount= 0;
    static long fileCount = 1;
    private final static int FILENAME_MAX_NAME = 85;

    public ZipDialog(final Context context, ArrayList<FileInfo> fileinfos, String Dir) {
        bufSize = 1024 * 4;
        buf = new byte[bufSize];
        mContext = new WeakReference<Context>(context);
        selectedFileInfoList = fileinfos;
        FileDir = Dir;
        mUIHandler = new UIHandler();

        mDialogView = LayoutInflater.from(context).inflate(R.layout.mkdir_dialog, null);

        mEditText = (EditText)mDialogView.findViewById(R.id.mkdir_edit);
        mEditText.setText(R.string.newfolder_title);
        mEditText.setSelectAllOnFocus(true);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.length() + i3 >= FILENAME_MAX_NAME) {
                    Toast.makeText(context,
                            context.getResources().getString(R.string.name_too_long),
                            Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) { }
            @Override
            public void afterTextChanged(Editable editable) { }
        });

        String filename = selectedFileInfoList.get(0).getName();
        if(filename!=null)
        {
            if(!selectedFileInfoList.get(0).isFolder()) {
                int index = filename.lastIndexOf(".");
                if (index > 0)
                    filename = filename.substring(0, index);
            }
            mEditText.setText(filename);
        }
    }


    public void show() {
        if (mContext == null || mContext.get() == null) {
            return;
        }
        Dialog dialog = new AlertDialog.Builder(mContext.get())
                .setTitle(R.string.actions_title_Zip)
                .setView(mDialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean ret = false;
                        if(mEditText != null)
                        {
                            String name = mEditText.getText().toString();
                            if((name!=null)&&(name.length()>0))
                            {
                                FileDir = FileDir+File.separator+ name + ".zip";
                                File Dest = new File(FileDir);
                                if(!(Dest.exists()&&Dest.isFile()))
                                {
                                    ret = true;
                                }
                                else if (Dest.exists()) {
                                    Toast.makeText(mContext.get(), R.string.same_file_zipped, Toast.LENGTH_SHORT).show();
                                    return ;
                                }
                            }
                        }
                        if(!ret)
                        {
                            Toast.makeText(mContext.get(), R.string.ali_ZipError, Toast.LENGTH_SHORT).show();
                            return ;
                        }


                        fileCount = getTaskCount();
                        showProgressDialog(mContext.get().getResources().getString(R.string.dialog_title_do_Zip),(int)fileCount);
                        FileMgrCore.setIgnoreFileWatch(true);
                        mThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                List<File> filesList = new ArrayList<File>();
                                for(FileInfo fileInfo :selectedFileInfoList) {
                                    File file = new File(fileInfo.getPath());
                                    filesList.add(file);
                                }

                                // TODO Auto-generated method stub
                                doZip(filesList, FileDir);
                                if(bIsForceStop)
                                {
                                    File file = new File(FileDir);
                                    Log.d(TAG, "zipFile name:" + FileDir + ":" + (file != null && file.exists()));
                                    if (file != null && file.exists()) {
                                        file.delete();
                                    }
                                }
                                mUIHandler.removeMessages(zip_ShowProgress_id);
                                mUIHandler.sendEmptyMessage(zip_finish_id);
                            }

                        });
                        mThread.start();
                    }
                })
                .setNegativeButton(R.string.cancel, null).create();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    /**
     *
     * 
     * @param srcFile
     * @param destFile
     */
    private ZipFile zipFile;
    private ZipOutputStream zipOut;
    private int bufSize; // size of bytes
    private byte[] buf;

    public void doZip(List<File> srcFile, String destFile) {

        try {
            this.zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(
                    destFile)));
            zipOut.setComment("comment");
            zipOut.setEncoding("GBK");
            zipOut.setMethod(ZipOutputStream.DEFLATED);

            zipOut.setLevel(Deflater.DEFAULT_COMPRESSION);
            for (File zipFile : srcFile) {
                handleFile(zipFile, this.zipOut, "");
            }
            this.zipOut.close();
            FileMgrCore.notifyMediaFileChange(destFile);
        } catch (IOException ioe) {
            File tmpFile = new File(destFile);
            if(tmpFile.exists())
                tmpFile.delete();
            Message msg = mUIHandler.obtainMessage(zip_NotSpace_id);
            mUIHandler.sendMessage(msg);
            ioe.printStackTrace();
        }
    }

    /**
     *
     * 
     * @param zipFile
     * @param zipOut
     * @param dirName
     * @throws IOException
     */
    private void handleFile(File zipFile, ZipOutputStream zipOut, String dirName)
            throws IOException {
        if(bIsForceStop)
        {
            return;
        }
        if (zipFile.isDirectory()) {
            File[] files = zipFile.listFiles();

            if (files.length == 0) {
                this.zipOut.putNextEntry(new ZipEntry(dirName + zipFile.getName() + File.separator));
                this.zipOut.closeEntry();
            } else {
                for (File file : files) {
                    handleFile(file, zipOut, dirName + zipFile.getName() + File.separator);
                }
            }
        }
        else {
            FileInputStream fileIn = new FileInputStream(zipFile);
            this.zipOut.putNextEntry(new ZipEntry(dirName + zipFile.getName()));
            long maxsize = zipFile.length();
            long buff = 0;
            int length = 0;
            if (fileCount == 1) {
                mUIHandler.removeMessages(zip_SetMessage_id);
                Message msg = mUIHandler.obtainMessage(zip_SetMessage_id);
                Bundle msg_data = new Bundle();
                msg_data.putString(zip_SetMessage_key, zipFile.getName());
                msg.setData(msg_data);
                mUIHandler.sendMessage(msg);
            }
            while ((length = fileIn.read(this.buf)) > 0) {
                this.zipOut.write(this.buf, 0, length);
                buff += length;
                if (fileCount == 1 && maxsize > 0) {
                    final double progress = (double)buff/(double)maxsize;
                    Message msg = mUIHandler.obtainMessage(zip_ShowProgress_id);
                    Bundle msg_data = new Bundle();
                    msg_data.putString(zip_ShowProgress_key_filename, zipFile.getName());
                    msg_data.putDouble(zip_ShowProgress_key_precent, progress);
                    msg.setData(msg_data);
                    mUIHandler.removeMessages(zip_ShowProgress_id);
                    mUIHandler.sendMessage(msg);
                }
                if(bIsForceStop)
                    break;
            }
            this.zipOut.closeEntry();
            zippedCount++;
            if (fileCount!= 1) {
                Message msg = mUIHandler.obtainMessage(zip_ShowProgress_id);
                Bundle msg_data = new Bundle();
                msg_data.putString(zip_ShowProgress_key_filename, zipFile.getName());
                msg.setData(msg_data);
                mUIHandler.removeMessages(zip_ShowProgress_id);
                mUIHandler.sendMessage(msg);
                systemtime = System.currentTimeMillis();
            }
        }

    }

    private long  getTaskCount() {
        List<File> filesList = new ArrayList<File>();
        for (FileInfo fileInfo :selectedFileInfoList) {
            File file = new File(fileInfo.getPath());
            filesList.add(file);
        }

        long  taskcount = 0;
        for(File file:filesList)
        {
            taskcount = FileUtil.listDirFileNum(file, taskcount);
        }

        return taskcount;
    }

    private void showProgressDialog(String message, int task_count) {
        if (mContext != null && mContext.get() != null) {
            mProgressDialog = new ProgressDialog(mContext.get(), message);
            mProgressDialog.setMessage("");
            mProgressDialog.setTaskCount(task_count);
            mProgressDialog.setCancelDelegate(new ProgressDialog.ProgressDialogDelegate() {
                @Override
                public void onCancel() {
                    bIsForceStop = true;
                }
            });
            mProgressDialog.show();
            mProgressDialog.finishCurrentTask();
        }
    }

    class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case zip_finish_id:
                    mUIHandler.removeMessages(zip_finish_id);
                    mProgressDialog.dismiss();
                    FileMgrClipboard.getInstance().finishClipboard();
                    FileMgrCore.setIgnoreFileWatch(false);
                    break;
                case zip_ShowProgress_id:
                    mUIHandler.removeMessages(zip_ShowProgress_id);
                    Bundle data = msg.getData();
                    String filename = data.getString(zip_ShowProgress_key_filename);
                    mProgressDialog.setTaskCount((int) fileCount);
                    mProgressDialog.setMessage(filename);
                    if(fileCount <=1) {
                        double percent = data.getDouble(zip_ShowProgress_key_precent);
                        mProgressDialog.setProgress(percent);
                    }else {
                        mProgressDialog.finishCurrentTask();
                    }

                    break;
                case zip_NotSpace_id:
                    mUIHandler.removeMessages(zip_NotSpace_id);
                    mProgressDialog.dismiss();
                    if (mContext != null && mContext.get() != null) {
                        Toast.makeText(mContext.get(), R.string.ali_NotEnoughSpace, Toast.LENGTH_LONG).show();
                    }
                    break;
                case zip_SetMessage_id:
                    String name = msg.getData().getString(zip_SetMessage_key, "");
                    mProgressDialog.setMessage(name);
                    break;
                default:
                    break;
            }
        }

    }

}
