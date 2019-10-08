package com.aliyunos.filemanager.core;


public class FileMgrCloudEvent {
    public final static int CLOUD_EVENT_REFRESH_CURRENT_LIST = 1;
    public final static int CLOUD_EVENT_REFRESH_LIST_VIEW = 2;
    public final static int CLOUD_EVENT_DOWNLOAD_COMPLETE = 3;
    public final static int CLOUD_EVENT_UPLOAD_COMPLETE = 4;
    public final static int CLOUD_EVENT_CUT_COMPLETE = 5;
    public final static int CLOUD_EVENT_DOWNLOAD_FILE_DELETE = 6;
    private int mEventType;
    private String mEventContent;
    private String[] mDeleteFiles;
    public FileMgrCloudEvent(int type) {
        mEventType = type;
    }

    public FileMgrCloudEvent(int type, String content) {
        mEventType = type;
        mEventContent = content;
    }

    public FileMgrCloudEvent(int type, String[] deleteFiles) {
        mEventType = type;
        mDeleteFiles = deleteFiles;
    }

    public int getEventType() { return mEventType; }

    public String getEventContent() { return mEventContent; }

    public String[] getDeleteFiles() { return mDeleteFiles; }
}
