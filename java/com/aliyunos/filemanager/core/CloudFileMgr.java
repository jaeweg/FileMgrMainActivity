package com.aliyunos.filemanager.core;


import android.os.Handler;
import android.os.Message;

import com.kanbox.sdk.common.KbxDefaultResponse;
import com.kanbox.sdk.common.KbxRequest;
import com.kanbox.sdk.common.KbxResponse;
import com.kanbox.sdk.common.KbxAcInfoResponse;
import com.kanbox.sdk.filelist.KbxFileManager;
import com.kanbox.sdk.filelist.request.KbxSetFileListRequest;
import com.kanbox.sdk.filelist.response.KbxUpdateFileListResponse;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

// 仅仅转一次线程
public class CloudFileMgr {
    private static CloudFileMgr sInstance;

    private final int MSG_ON_RESPONSE = 0;
    class Response {
        Response(KbxRequest request, Object response) {
            this.request = request;
            this.response = response;
        }

        KbxRequest request;
        Object response;
    }
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_ON_RESPONSE:
                    Response r = (Response)msg.obj;
                    r.request.getResponse().onResponse(r.response);
                    break;
            }
        }
    };

    public static CloudFileMgr getsInstance() {
        if (sInstance == null) {
            sInstance = new CloudFileMgr();
        }
        return sInstance;
    }

    private void perform(KbxRequest request, KbxDefaultResponse response) {
        Message msg = new Message();
        msg.what = MSG_ON_RESPONSE;
        msg.obj = new Response(request, response);
        mHandler.sendMessage(msg);
    }

    private void perform(KbxRequest request, KbxUpdateFileListResponse response) {
        Message msg = new Message();
        msg.what = MSG_ON_RESPONSE;
        msg.obj = new Response(request, response);
        mHandler.sendMessage(msg);
    }

    private void perform(KbxRequest request, KbxAcInfoResponse response) {
        Message msg = new Message();
        msg.what = MSG_ON_RESPONSE;
        msg.obj = new Response(request, response);
        mHandler.sendMessage(msg);
    }

    static class UpdateFileListResponse implements KbxResponse<KbxUpdateFileListResponse> {
        public UpdateFileListResponse(CloudFileMgr self,
                                               KbxRequest<KbxUpdateFileListResponse> request) {
            mself = new WeakReference<CloudFileMgr>(self);
            synchronized (UpdateFileListResponse.class){
                mMap.put(this, request);
            }
        }

        @Override
        public void onResponse(KbxUpdateFileListResponse response) {
            KbxRequest<KbxUpdateFileListResponse> reqeust = mMap.get(this);
            if(mself != null && mself.get() != null) {
                mself.get().perform(reqeust, response);
            }
            synchronized (UpdateFileListResponse.class) {
                mMap.remove(this);
            }
        }

        WeakReference<CloudFileMgr> mself = null;
        static Map< UpdateFileListResponse,KbxRequest<KbxUpdateFileListResponse>>  mMap =
                new HashMap<UpdateFileListResponse, KbxRequest<KbxUpdateFileListResponse>>();
    }


    //通用
    static class DefaultResponse implements KbxResponse<KbxDefaultResponse> {
        public DefaultResponse(CloudFileMgr self, KbxRequest<KbxDefaultResponse> request) {
            mself = new WeakReference<CloudFileMgr>(self);

            synchronized (DefaultResponse.class){
                mMap.put(this, request);
            }
        }

        @Override
        public void onResponse(KbxDefaultResponse response) {
            KbxRequest<KbxDefaultResponse> reqeust = mMap.get(this);
            if(mself != null && mself.get() != null) {
                mself.get().perform(reqeust, response);
            }
            synchronized (DefaultResponse.class) {
                mMap.remove(this);
            }
        }

        WeakReference<CloudFileMgr> mself = null;
        static Map< DefaultResponse,KbxRequest<KbxDefaultResponse>>  mMap =
                new HashMap<DefaultResponse, KbxRequest<KbxDefaultResponse>>();
    }

    static class AcInfoResponse implements KbxResponse<KbxAcInfoResponse> {
        public AcInfoResponse(CloudFileMgr self, KbxRequest<KbxAcInfoResponse> request) {
            mself = new WeakReference<CloudFileMgr>(self);

            synchronized (AcInfoResponse.class){
                mMap.put(this, request);
            }
        }

        @Override
        public void onResponse(KbxAcInfoResponse response) {
            KbxRequest<KbxAcInfoResponse> reqeust = mMap.get(this);
            if(mself != null && mself.get() != null) {
                mself.get().perform(reqeust, response);
            }
            synchronized (AcInfoResponse.class) {
                mMap.remove(this);
            }
        }

        WeakReference<CloudFileMgr> mself = null;
        static Map<AcInfoResponse, KbxRequest<KbxAcInfoResponse>>  mMap =
            new HashMap<AcInfoResponse, KbxRequest<KbxAcInfoResponse>>();
    }

    public void updateFileList(String filePath, KbxRequest<KbxUpdateFileListResponse> request) {
        UpdateFileListResponse customKbxUpdateFileListResponse = new UpdateFileListResponse(this,request);
        KbxFileManager.updateFileList(filePath, new KbxRequest<KbxUpdateFileListResponse>(customKbxUpdateFileListResponse));
    }

    public void moveFiles(KbxSetFileListRequest.FileData[] fileList, String dstPath, KbxRequest<KbxDefaultResponse> request) {
        DefaultResponse customResponse = new DefaultResponse(this,request);
        KbxFileManager.moveFiles(fileList, dstPath, new KbxRequest<KbxDefaultResponse>(customResponse));
    }

    public void deleteFiles(KbxSetFileListRequest.FileData[] fileList,
                            KbxRequest<KbxDefaultResponse> request) {
        DefaultResponse customResponse = new DefaultResponse(this,request);
        KbxFileManager.deleteFiles(fileList,new KbxRequest<KbxDefaultResponse>(customResponse));
    }

    public void copyFiles(KbxSetFileListRequest.FileData[] fileList, String dstPath, KbxRequest<KbxDefaultResponse> request) {
        DefaultResponse customResponse = new DefaultResponse(this,request);
        KbxFileManager.copyFiles(fileList, dstPath, new  KbxRequest<KbxDefaultResponse>(customResponse));
    }

    public void newFolder(String folderName, String cloudPath, KbxRequest<KbxDefaultResponse> request) {
        DefaultResponse customResponse = new DefaultResponse(this,request);
        KbxFileManager.addFolder(folderName, cloudPath, new  KbxRequest<KbxDefaultResponse>(customResponse));
    }

    public void renameFile(String fullPath, String newName, boolean isDir, KbxRequest<KbxDefaultResponse> request) {
        DefaultResponse customResponse = new DefaultResponse(this,request);
        KbxFileManager.renameFile(fullPath,newName,isDir,new KbxRequest<KbxDefaultResponse>(customResponse));
    }

    public void checkSpace(long fileSize, KbxRequest<KbxDefaultResponse> request) {
        DefaultResponse customResponse = new DefaultResponse(this,request);
        KbxFileManager.checkSpace(fileSize, new KbxRequest<KbxDefaultResponse>(customResponse));
    }

    public void getAcInfo(KbxRequest<KbxAcInfoResponse> request){
        AcInfoResponse customResponse = new AcInfoResponse(this, request);
        KbxFileManager.getAcInfo(new KbxRequest<KbxAcInfoResponse>(customResponse));
    }
}
