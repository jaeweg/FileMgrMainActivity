package com.aliyunos.filemanager.ui.view.dialog;

import android.content.Context;
import android.text.Editable;
import android.text.Selection;
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
import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.local.LocalFileInfo;
import com.aliyunos.filemanager.ui.local.LocalFileListAdapter;
import com.aliyunos.filemanager.ui.view.FileInfo;
import com.aliyunos.filemanager.ui.view.FileListViewAdapter;
import com.aliyunos.filemanager.util.FileUtil;
import com.kanbox.filemgr.KbxCloudAccount;
import com.kanbox.sdk.common.KbxDefaultResponse;
import com.kanbox.sdk.common.KbxRequest;
import com.kanbox.sdk.common.KbxResponse;

import java.io.File;
import java.lang.ref.WeakReference;

import hwdroid.dialog.AlertDialog;
import hwdroid.dialog.Dialog;
import hwdroid.dialog.DialogInterface;

public class RenameDialog {
    private final static int FILENAME_MAX_NAME = 85;
    private final int CLOUD_FILE_IS_EXIST = 10731;
    private final int ClOUD_FOLDER_IS_EXIST = 10730;

    private WeakReference<Context> mContext;
    private View mDialogView;
    private String mPath;
    private String mOldName;
    private EditText mEditText;
    private FileInfo mFileInfo;

    public RenameDialog(Context context, FileInfo info) {
        mContext = new WeakReference<Context>(context);
        mFileInfo = info;

        String path = mFileInfo.getPath();
        int lastSeparator = path.lastIndexOf(File.separator);
        mPath = path.substring(0, lastSeparator);
        mOldName = path.substring(lastSeparator + 1);
        
        mDialogView = LayoutInflater.from(context).inflate(R.layout.mkdir_dialog, null);
        mEditText = (EditText)mDialogView.findViewById(R.id.mkdir_edit);
        mEditText.setText(mOldName);
        // add by huangjiawei Rename file select name not all begin
        
        int selectIndex = mOldName.lastIndexOf(".");
        if (selectIndex != -1) {
        	Selection.setSelection(mEditText.getText(), 0 ,selectIndex);
		}else {
			mEditText.setSelectAllOnFocus(true);
		}
        
        // add by huangjiawei Rename file select name not all end
//        mEditText.setSelectAllOnFocus(true);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (mContext != null && mContext.get() != null) {
                    if (charSequence.length() + i3 >= FILENAME_MAX_NAME) {
                        Toast.makeText(mContext.get(),
                                mContext.get().getResources().getString(R.string.name_too_long),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) { }
            @Override
            public void afterTextChanged(Editable editable) { }
        });
    }

    public void show() {
        if (mContext != null && mContext.get() != null) {
            Dialog dialog = new AlertDialog.Builder(mContext.get())
                    .setTitle(R.string.dialog_title_rename)
                    .setView(mDialogView)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final String newName = mEditText.getText().toString().trim();
                            if (newName.length() >= FILENAME_MAX_NAME) {
                                if (mContext != null && mContext.get() != null) {
                                    Toast.makeText(mContext.get(),
                                            mContext.get().getResources().getString(R.string.name_too_long),
                                            Toast.LENGTH_LONG).show();
                                }
                                return;
                            }

                            String newNameExt = FileUtil.getExtension(newName);
                            String oldNameExt = FileUtil.getExtension(mOldName);
                            boolean isNameValid = true;
                            if (newName.equals(".") || newName.equals("..") || newName.indexOf("/") != -1) {
                                isNameValid = false;
                            }
                            
                         // add by huangjiawei Rename file select name not all begin
                            int mIndex = newName.lastIndexOf(".");
                            if (mIndex != -1) {
                            	String nameString = newName.substring(0, mIndex);
                            	if ("".equals(nameString) || nameString.isEmpty()) {
                            		if (mContext != null && mContext.get() != null) {
                                        Toast.makeText(mContext.get(),
                                                mContext.get().getResources().getString(R.string.ali_dirNameEmtpy),
                                                Toast.LENGTH_SHORT).show();
                                        return;
                                    }
    							}
							}
                         // add by huangjiawei Rename file select name not all end

                            if (newName.isEmpty() || !isNameValid) {
                                if (mContext != null && mContext.get() != null) {
                                    Toast.makeText(mContext.get(),
                                            mContext.get().getResources().getString(R.string.ali_dirNameEmtpy),
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else if ((newNameExt == null && oldNameExt != null)
                                    || (newNameExt != null && !newNameExt.equalsIgnoreCase(oldNameExt))) {
                                ChangeExtDialog.showDialog(mContext.get(), new ChangeExtDialog.ChangeExtDialogListener() {
                                    @Override
                                    public void onConfirmClicked() {
                                        renameFile(newName);
                                    }
                                    @Override
                                    public void onCancelClicked() {

                                    }
                                });
                            } else {
                                renameFile(newName);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, null).create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();
        }
    }

    public void renameFile(String newName) {
    	
    	// add by huangjiawei at 20170727 begin 
    	// if String contain spicialCharaters
    	if (null != newName && NewFolderDialog.isSpecialCharacter(newName)) {
    		if (mContext != null && mContext.get() != null) {
                Toast.makeText(mContext.get(),
                        mContext.get().getResources().getString(R.string.doov_contain_specialchar),
                        Toast.LENGTH_LONG).show();
            }
    		return;
		}
    	// add by huangjiawei at 20170727 end 
    	// if String contain spicialCharaters
    	
        if (mFileInfo instanceof LocalFileInfo) {
            if (mContext != null && mContext.get() != null) {
                FileMgrCore.renameFile(mPath, mOldName, newName, mContext.get());
            }
        } else {
            String path = mFileInfo.getPath();
            if (mFileInfo.getName().equals(newName)) {
                if (mContext != null && mContext.get() != null) {
                    Toast.makeText(mContext.get(), mContext.get().getString(R.string.RenameFileExist), Toast.LENGTH_LONG).show();
                }
                return;
            }
            CloudFileMgr.getsInstance().renameFile(path, newName, mFileInfo.isFolder(),
                    new KbxRequest<KbxDefaultResponse>(new KbxResponse<KbxDefaultResponse>() {
                        @Override
                        public void onResponse(KbxDefaultResponse response) {
                            if (response.getErrorNo() == 0) {
                                BusProvider.getInstance().post(
                                        new FileMgrCloudEvent(FileMgrCloudEvent.CLOUD_EVENT_REFRESH_CURRENT_LIST));
                            } else if (mFileInfo.isFolder() && response.getErrorNo() == ClOUD_FOLDER_IS_EXIST) {
                                if (mContext != null && mContext.get() != null) {
                                    Toast.makeText(mContext.get(), mContext.get().getString(R.string.ali_dirIsExist), Toast.LENGTH_SHORT).show();
                                }
                            } else if (response.getErrorNo() == CLOUD_FILE_IS_EXIST) {
                                if (mContext != null && mContext.get() != null) {
                                    Toast.makeText(mContext.get(), mContext.get().getString(R.string.rename_failed_for_same_name), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                if (mContext != null && mContext.get() != null) {
                                    Toast.makeText(mContext.get(), mContext.get().getString(R.string.rename_failed), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }));
        }
    }
    public static void showDialog(Context context, FileInfo fileInfo) {
        String path = fileInfo.getPath();
        int lastSeparator = path.lastIndexOf(File.separator);
        if (lastSeparator < 0) {
            return;
        }
        new RenameDialog(context, fileInfo).show();
    }
}
