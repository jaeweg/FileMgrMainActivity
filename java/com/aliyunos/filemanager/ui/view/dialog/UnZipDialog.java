
package com.aliyunos.filemanager.ui.view.dialog;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.DialogInterface.OnClickListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.FileMgrClipboard;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.ui.cloud.CloudFileListAdapter;
import com.aliyunos.filemanager.ui.local.LocalFileInfo;
import com.aliyunos.filemanager.ui.local.LocalFileListAdapter;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListViewAdapter;
import com.kanbox.filemgr.KbxLocalFile;
import com.kanbox.filemgr.KbxLocalFileManager;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipException;

public class UnZipDialog {
    private static final String TAG = "UnZipDialog";
    WeakReference<Context> mActivity;
    private AlertDialog mProgressDialog;
    private ProgressBar mProgressBar;
    private TextView mprogress_percent;
    private TextView mprogress_message;

    String FileFullPath;
    String  FileDir;
    int MAX = 100;
    int MAX_SHOW = 100;
    Thread mThread;
    LocalHandler mLocalHandler;
    boolean bIsForceStop =false;
    boolean bIsFirst = false;
    View detailView;
    String mDelete;

    public UnZipDialog(Context m, FileInfo fileInfo, String Dir) {
        bIsFirst = true;
        mActivity = new WeakReference<Context>(m);
        if (fileInfo instanceof LocalFileInfo) {
            FileFullPath = fileInfo.getPath();
        } else {
            KbxLocalFile downloadedFile = KbxLocalFileManager.getDownloadedFile(fileInfo.getPath());
            if (downloadedFile != null) {
                FileFullPath = downloadedFile.getFilePath();
            }
        }

        FileDir = Dir;
        Log.v(TAG, "UnZipDialog filename:" + fileInfo.getPath());
        Log.v(TAG, "UnZipDialog FileDir:" + FileDir);
        LayoutInflater factory = LayoutInflater.from(m);
        detailView = factory.inflate(R.layout.progress_dialog, null);

        mProgressBar = (ProgressBar)detailView.findViewById(R.id.progress);
        mprogress_message = (TextView)detailView.findViewById(R.id.progress_message);
        mprogress_percent = (TextView)detailView.findViewById(R.id.progress_percent);
        detailView.setMinimumWidth(10000);
        mDelete = m.getResources().getString(R.string.dialog_title_do_Unzip);
        mprogress_percent.setText(mDelete);
        mprogress_message.setText(m.getResources().getString(R.string.dialog_title_analyze_file));

        mProgressBar.setMax(MAX);
        mprogress_percent.setText(R.string.dialog_title_do_Unzip);
        mProgressDialog = new AlertDialog.Builder(m)
        .setView(detailView)
        .setNegativeButton(R.string.cancel, new OnClickListener() {

            public void onClick(hwdroid.dialog.DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                bIsForceStop = true;
            }
        }).create();
        mProgressDialog.setCanceledOnTouchOutside(false);
        Log.v(TAG, "UnZipDialog");
   }
    final int Unzip_finish_id = 1;
    final int Unzip_ShowProgress_id = 2;
    final int Unzip_NotSpace_id = 3;
    final int Unzip_SameName_id = 4;
    final String Unzip_ShowProgress_key_precent = "progress_precent";
    final String Unzip_ShowProgress_key_filename = "progress_filename";
    public void show() {
        Log.v(TAG, "UnZipDialog show:" + FileFullPath);
        mProgressDialog.show();
        FileMgrCore.setIgnoreFileWatch(true);

        mLocalHandler = new LocalHandler();
        mThread = new Thread(new Runnable()
        {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    unZipFile(FileFullPath, FileDir,"GBK");
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ZipException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        unZipFile(FileFullPath, FileDir,"UTF-8");
                    }catch (Exception err) {
                        err.printStackTrace();
                    }
                }
                FileMgrCore.notifyFolderChange(FileDir);
                mLocalHandler.sendEmptyMessage(Unzip_finish_id);
            }

        });
        mThread.start();
    }
    public boolean checkSameNameFile(){
        String archive = FileFullPath; 
        String decompressDir = FileDir;
        File file = new File(decompressDir);
        int i = archive.lastIndexOf(".");
        if (file.exists() && file.isDirectory() && i > 0) {
            String name = archive.substring(0, i);
            String path = "";
            String path1 = "";
            int j;
            Log.d(TAG, "huachao111:checkSameNameFile:"+name+":"+archive);
            File []files = file.listFiles();
            for (File f : files) {
                path = f.getAbsolutePath();
                if (path.startsWith(name)) {
                    j = path.lastIndexOf(".");
                    if (j > 0) {
                        path1 = path.substring(0, j);
                    }else {
                        path1 = path;
                    }
                    if (path1.equalsIgnoreCase(name) && !archive.equalsIgnoreCase(f.getAbsolutePath())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public void    unZipFile(String archive, String decompressDir,String encoding)
            throws IOException, FileNotFoundException, ZipException
    {
        BufferedInputStream bi;
        ZipFile zf = new ZipFile(archive,encoding);
        Enumeration e = zf.getEntries();
        long unzip_size = 0;
        File m = new File(archive);
        long file_size = m.length();
        long total_len = 0;

        while (e.hasMoreElements() && (!bIsForceStop)) {
            ZipEntry ze2 = (ZipEntry) e.nextElement();
            total_len += ze2.getSize();
        }

        File tmpFile = new File(decompressDir);
        if(total_len > tmpFile.getFreeSpace()) {
            Message msg = mLocalHandler.obtainMessage(Unzip_NotSpace_id);
            mLocalHandler.sendMessage(msg);
            return;
        }

        e = zf.getEntries();
        while (e.hasMoreElements()&& (!bIsForceStop))
        {
            ZipEntry ze2 = (ZipEntry) e.nextElement();
            String entryName = ze2.getName();
            String path = decompressDir + "/" + entryName;
            if (ze2.isDirectory())
            {
                File decompressDirFile = new File(path);
                if (!decompressDirFile.exists())
                {
                    decompressDirFile.mkdirs();
                }
            } else
            {
                String fileDir = path.substring(0, path.lastIndexOf("/"));
                File fileDirFile = new File(fileDir);
                if (!fileDirFile.exists())
                {
                    fileDirFile.mkdirs();
                }
                File file = new File(decompressDir + '/' + entryName);
                if (file.exists() && bIsFirst) {
                    mLocalHandler.sendEmptyMessage(Unzip_SameName_id);
                    bIsFirst = false;
                }
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(decompressDir + "/" + entryName));
                bi = new BufferedInputStream(zf.getInputStream(ze2));
                byte[] readContent = new byte[1024];
                int readCount = bi.read(readContent);
                unzip_size += readCount;
                while ((readCount != -1)&&(!bIsForceStop))
                {
                    bos.write(readContent, 0, readCount);
                    readCount = bi.read(readContent);
                    unzip_size += readCount;
                    int precent;
                    if (total_len == 0) {
                        precent = (int)(unzip_size * MAX / 1);
                    } else {
                        precent = (int)(unzip_size * MAX / total_len);
                    }
                    Message msg = mLocalHandler.obtainMessage(Unzip_ShowProgress_id);
                    Bundle msg_data = new Bundle();
                    msg_data.putInt(Unzip_ShowProgress_key_precent, precent);
                    msg_data.putString(Unzip_ShowProgress_key_filename, path.toString());
                    msg.setData(msg_data);
                    mLocalHandler.removeMessages(Unzip_ShowProgress_id);
                    mLocalHandler.sendMessage(msg);
                }
                bi.close();
                bos.close();
                }
        }
        zf.close();
    }

    @SuppressLint("HandlerLeak")
    public class LocalHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, "FileDeleteDialog msg.what:" + msg.what);
            switch (msg.what) {
                case Unzip_finish_id:
                    //filenameTxt.setText("Finished!!!");
                    mLocalHandler.removeMessages(Unzip_finish_id);
                    mProgressDialog.dismiss();
                    FileMgrClipboard.getInstance().finishClipboard();
                    FileMgrCore.setIgnoreFileWatch(false);
                    break;
                case Unzip_ShowProgress_id:
                    mLocalHandler.removeMessages(Unzip_ShowProgress_id);
                    Bundle data = msg.getData();
                    int precent = data.getInt(Unzip_ShowProgress_key_precent);
                    if(precent > MAX_SHOW)
                        precent = MAX_SHOW;

                    String precent_max = String.valueOf(precent)+"/"+MAX;
                    String filename = data.getString(Unzip_ShowProgress_key_filename);
                    mprogress_message.setText(filename);
                    //precentTxt.setText(precent_max);
                    String mpercent =String.format(Locale.getDefault(),"(%d", precent);
                    mpercent += "%)";
                    mprogress_percent.setText(mDelete + mpercent);
                    mProgressBar.setProgress(precent);
                    break;
                case Unzip_NotSpace_id:
                    mLocalHandler.removeMessages(Unzip_NotSpace_id);
                    mProgressDialog.dismiss();
                    if (mActivity != null && mActivity.get() != null) {
                        Toast.makeText(mActivity.get(), R.string.ali_NotEnoughSpace, Toast.LENGTH_LONG).show();
                    }
                    break;
                case Unzip_SameName_id:
                    if (mActivity != null && mActivity.get() != null) {
                        Toast.makeText(mActivity.get(), R.string.same_name_file_exist, Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
