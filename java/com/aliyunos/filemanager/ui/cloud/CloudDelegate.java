package com.aliyunos.filemanager.ui.cloud;

public interface CloudDelegate {
    void onFetchListError(int errorCode);
    void onCloseCloud();
    void onShowSpaceWarning();
}
