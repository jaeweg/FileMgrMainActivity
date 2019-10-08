package com.aliyunos.filemanager.core;

public class FileMgrLoginEvent {
    private int mEvent;

    public int getEvent() { return mEvent; }

    public FileMgrLoginEvent(int event) {
        mEvent = event;
    }
}
