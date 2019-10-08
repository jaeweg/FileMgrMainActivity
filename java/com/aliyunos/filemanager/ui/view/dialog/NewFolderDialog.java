package com.aliyunos.filemanager.ui.view.dialog;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.aliyunos.filemanager.R;
import com.aliyunos.filemanager.core.CloudFileMgr;
import com.aliyunos.filemanager.core.FileMgrCloudEvent;
import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.core.FileMgrNetworkInfo;
import com.aliyunos.filemanager.provider.BusProvider;
import com.kanbox.sdk.common.KbxDefaultResponse;
import com.kanbox.sdk.common.KbxRequest;
import com.kanbox.sdk.common.KbxResponse;
import com.kanbox.sdk.filelist.KbxFileManager;

import java.io.File;
import java.lang.ref.WeakReference;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.Dialog;
import hwdroid.dialog.DialogInterface;

public class NewFolderDialog {
    private final static int FOLDERNAME_MAX_NAME = 85;
    private WeakReference<Context> mContext;
    private View mDialogView;
    private String mPath;
    private EditText mEditText;
    private boolean mIsCloudFile;
    private static Dialog mDialog;

    private final int CLOUD_FOLDER_IS_EXIST = 10724;

    public NewFolderDialog(Context context, String path, boolean isCloudFile) {
        mContext = new WeakReference<Context>(context);
        mPath = path;
        mIsCloudFile = isCloudFile;
        mDialogView = LayoutInflater.from(context).inflate(R.layout.mkdir_dialog, null);

        mEditText = (EditText)mDialogView.findViewById(R.id.mkdir_edit);
        mEditText.setText(R.string.newfolder_title);
        mEditText.setSelectAllOnFocus(true);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.length() + i3 >= FOLDERNAME_MAX_NAME) {
                    Toast.makeText(mContext.get(),
                            mContext.get().getResources().getString(R.string.name_too_long),
                            Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) { }
            @Override
            public void afterTextChanged(Editable editable) { }
        });
    }

    public void show() {
        if (mContext == null || mContext.get() == null) {
            return;
        }
        mDialog = new AlertDialog.Builder(mContext.get())
                .setTitle(R.string.input_dir_name)
                .setView(mDialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = mEditText.getText().toString().trim();
                        if (newName.length() >= FOLDERNAME_MAX_NAME) {
                            if (mContext != null && mContext.get() != null) {
                                Toast.makeText(mContext.get(),
                                        mContext.get().getResources().getString(R.string.name_too_long),
                                        Toast.LENGTH_LONG).show();
                            }
                            return;
                        }

                        if (newName.isEmpty() || newName.indexOf("/")!= -1) {
                            if (mContext != null && mContext.get() != null) {
                                Toast.makeText(mContext.get(),
                                        mContext.get().getResources().getString(R.string.ali_dirNameEmtpy),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } 
                        // add by huangjiawei at 20170727 begin 
                        else if (null != newName && isSpecialCharacter(newName)) {
                        	if (mContext != null && mContext.get() != null) {
                                Toast.makeText(mContext.get(),
                                        mContext.get().getResources().getString(R.string.doov_contain_specialchar),
                                        Toast.LENGTH_LONG).show();
                            }
                        
						} 
                        // add by huangjiawei at 20170727 end
                        else {
                            if (mIsCloudFile) {
                                CloudFileMgr.getsInstance().newFolder(newName, mPath,
                                        new KbxRequest<KbxDefaultResponse>(new KbxResponse<KbxDefaultResponse>() {
                                            @Override
                                            public void onResponse(KbxDefaultResponse response) {
                                                if (response.getErrorNo() == 0) {
                                                    // 刷新UI
                                                    BusProvider.getInstance().post(
                                                            new FileMgrCloudEvent(FileMgrCloudEvent.CLOUD_EVENT_REFRESH_CURRENT_LIST));
                                                } else {
                                                    if (mContext != null && mContext.get() != null) {
                                                        Log.d("CloudFileListView", "new folder Failed!");
                                                        String text;
                                                        int networkType = FileMgrNetworkInfo.getsInstance().getNetworkType();
                                                        if (networkType == FileMgrNetworkInfo.NETWORK_OTHER) {
                                                            text = mContext.get().getString(R.string.network_error);
                                                        } else {
                                                            if (response.getErrorNo() == CLOUD_FOLDER_IS_EXIST) {
                                                                text = mContext.get().getString(R.string.ali_dirIsExist);
                                                            } else {
                                                                text = mContext.get().getString(R.string.new_folder_failed);
                                                            }
                                                        }
                                                        Toast.makeText(mContext.get(), text, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }
                                        }));
                            } else {
                                if (mContext != null && mContext.get() != null) {
                                    FileMgrCore.newFolder(mPath, newName, mContext.get());
                                }
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null).create();
        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mDialog.show();
    }

    public static boolean isShowing(){
        boolean isShow = false;
        if(null != mDialog){
            isShow = mDialog.isShowing();
        }
        return isShow;
    }

    public static void showDialog(Context context, String path) {
        new NewFolderDialog(context, path, false).show();
    }

    public static void showCloudDialog(Context context, String path) {
        new NewFolderDialog(context, path, true).show();
    }
    
    /*
     * add by huangjiawei at 20170727
     * if String contain spicialCharaters,return true
     * spicialCharater "\/:*?<>|
     */
    public static boolean isSpecialCharacter(String newName){
    	if (newName.contains("\"") || newName.contains("\\") || newName.contains("/")
        		|| newName.contains(":") || newName.contains("*") || newName.contains("?") 
        		|| newName.contains("<") || newName.contains(">") 
        		|| newName.contains("|")) {
			return true;
		}
    	return false;
    }
}
