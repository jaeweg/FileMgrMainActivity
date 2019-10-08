package com.aliyunos.filemanager.core;

import com.aliyunos.filemanager.provider.BusProvider;
import com.aliyunos.filemanager.ui.view.FileInfo;

import java.util.ArrayList;

public class FileMgrClipboard {
    private static FileMgrClipboard sInstance;
    public static FileMgrClipboard getInstance() {
        if (sInstance == null) {
            sInstance = new FileMgrClipboard();
        }
        return sInstance;
    }

    public enum ClipboardMode {
        Empty,
        LocalFiles_Copy,
        LocalFiles_Cut,
        CloudFiles_Copy,
        CloudFiles_Cut,
        LocalFiles_UnZip,
        LocalFiles_Zip,
    }

    public class ClipboardChangeEvent { }

    private ClipboardMode mCurrentMode;
    private ArrayList<FileInfo> mFileInfoList;
    private String mStringContent;

    protected FileMgrClipboard() {
        mCurrentMode = ClipboardMode.Empty;
        mFileInfoList = null;
    }

    public ClipboardMode getMode() {
        return mCurrentMode;
    }

    public ArrayList<FileInfo> getClipboardFileList() {
        return mFileInfoList;
    }

    public void copyString(String str) { mStringContent = str; }
    public String getStringContent() { return mStringContent; }

    public void copyLocalFile(FileInfo fileInfo) {
        ArrayList<FileInfo> fileInfoList = new ArrayList<FileInfo>();
        fileInfoList.add(fileInfo);
        copyLocalFiles(fileInfoList);
    }

    public void copyLocalFiles(ArrayList<FileInfo> fileInfoList) {
        mCurrentMode = ClipboardMode.LocalFiles_Copy;
        mFileInfoList = fileInfoList;
        BusProvider.getInstance().post(new ClipboardChangeEvent());
    }

    public void cutLocalFile(FileInfo fileInfo) {
        ArrayList<FileInfo> fileInfoList = new ArrayList<FileInfo>();
        fileInfoList.add(fileInfo);
        cutLocalFiles(fileInfoList);
    }

    public void cutLocalFiles(ArrayList<FileInfo> fileInfoList) {
        mCurrentMode = ClipboardMode.LocalFiles_Cut;
        mFileInfoList = fileInfoList;
        BusProvider.getInstance().post(new ClipboardChangeEvent());
    }

    public void copyCloudFile(FileInfo fileInfo) {
        ArrayList<FileInfo> fileInfoList = new ArrayList<FileInfo>();
        fileInfoList.add(fileInfo);
        copyCloudFiles(fileInfoList);
    }

    public void copyCloudFiles(ArrayList<FileInfo> fileInfoList) {
        mCurrentMode = ClipboardMode.CloudFiles_Copy;
        mFileInfoList = fileInfoList;
        BusProvider.getInstance().post(new ClipboardChangeEvent());
    }

    public void cutCloudFile(FileInfo fileInfo) {
        ArrayList<FileInfo> fileInfoList = new ArrayList<FileInfo>();
        fileInfoList.add(fileInfo);
        cutCloudFiles(fileInfoList);
    }

    public void cutCloudFiles(ArrayList<FileInfo> fileInfoList) {
        mCurrentMode = ClipboardMode.CloudFiles_Cut;
        mFileInfoList = fileInfoList;
        BusProvider.getInstance().post(new ClipboardChangeEvent());
    }

    public void unzipLocalFile(FileInfo fileInfo) {
        ArrayList<FileInfo> fileInfoList = new ArrayList<FileInfo>();
        fileInfoList.add(fileInfo);
        mCurrentMode = ClipboardMode.LocalFiles_UnZip;
        mFileInfoList = fileInfoList;
        BusProvider.getInstance().post(new ClipboardChangeEvent());
    }
    public void zipLocalFile() {
        mCurrentMode = ClipboardMode.LocalFiles_Zip;
    }

    public void finishClipboard() {
        mCurrentMode = ClipboardMode.Empty;
        mFileInfoList = null;
        mStringContent = null;
        BusProvider.getInstance().post(new ClipboardChangeEvent());
    }
}
