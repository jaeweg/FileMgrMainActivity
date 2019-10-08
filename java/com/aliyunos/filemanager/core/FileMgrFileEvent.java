package com.aliyunos.filemanager.core;

import com.kanbox.filemgr.KbxFileChangeEvent;

public class FileMgrFileEvent {
    public static final int ACCESS = 0x00000001;
    public static final int MODIFY = 0x00000002;
    public static final int ATTRIB = 0x00000004;
    public static final int CLOSE_WRITE = 0x00000008;
    public static final int CLOSE_NOWRITE = 0x00000010;
    public static final int OPEN = 0x00000020;
    public static final int MOVED_FROM = 0x00000040;
    public static final int MOVED_TO = 0x00000080;
    public static final int CREATE = 0x00000100;
    public static final int DELETE = 0x00000200;
    public static final int DELETE_SELF = 0x00000400;
    public static final int MOVE_SELF = 0x00000800;
    public static final int UNMOUNT = 0x00002000;
    public static final int Q_OVERFLOW = 0x00004000;
    public static final int IGNORED = 0x00008000;
    public static final int ONLYDIR = 0x01000000;
    public static final int DONT_FOLLOW = 0x02000000;
    public static final int MASK_ADD = 0x20000000;
    public static final int ISDIR = 0x40000000;
    public static final int ONESHOT = 0x80000000;

    public static final int EVENT_TYPE_FILECHANGE = 0;
    public static final int EVENT_TYPE_REFRESH = 1;
    public static final int EVENT_TYPE_FILECHANGE_EVENT_LIST = 2;
    public static final int EVENT_TYYP_ROOTDIRCHANGE_EVENT = 3;
    private int mEventType;
    private KbxFileChangeEvent[] mEventList;
    private String mFilePath;
    private boolean mIsFolder;
    private boolean mIsNewEvent;
    private boolean mIsDeleteEvent;
    private boolean mIsModifyEvent;

    public KbxFileChangeEvent[] getEventList() { return mEventList; }
    public int getEventType() { return mEventType; }
    public String getFilePath() { return mFilePath; }
    public boolean isFolder() { return mIsFolder; }
    public boolean isNewEvent() { return mIsNewEvent; }
    public boolean isDeleteEvent() { return mIsDeleteEvent; }
    public boolean isModifyEvent() { return mIsModifyEvent; }

    public FileMgrFileEvent(KbxFileChangeEvent[] eventList) {
        mEventType = EVENT_TYPE_FILECHANGE_EVENT_LIST;
        mEventList = eventList;
    }

    public FileMgrFileEvent(String path, int event) {
        mEventType = EVENT_TYPE_FILECHANGE;
        mFilePath = path;
        if ((event & DELETE) != 0 || (event & DELETE_SELF) != 0 ||
                (event & MOVED_FROM) != 0 || (event & MOVE_SELF) != 0) {
            mIsDeleteEvent = true;
        }

        if ((event & ISDIR) != 0) {
            mIsFolder = true;
        }

        if ((event & CREATE) != 0 || (event & MOVED_TO) != 0) {
            mIsNewEvent = true;
        }

        if ((event & MODIFY) != 0) {
            mIsModifyEvent = true;
        }
    }

    public FileMgrFileEvent() {
        mEventType = EVENT_TYYP_ROOTDIRCHANGE_EVENT;
    }
}
