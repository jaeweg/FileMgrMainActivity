package com.aliyunos.filemanager.core;

import android.content.Context;
import android.util.Log;

import com.aliyunos.filemanager.util.FileUtil;
import com.kanbox.filemgr.KbxLocalFileManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FileMgrCoreInitializer {
    private static Context mContext;
    private final static String TAG = "FileMgrCoreInitializer";

    public static void init(Context ctx) {
        mContext = ctx;
        copyAssetFolder("config", mContext.getDir("files", 0).getAbsolutePath() + "/config");

        System.loadLibrary("filemgr");

        String env = "release";

        long curTime = System.currentTimeMillis();
        KbxLocalFileManager.initFileMgr("files", new String[] {
//                "--trace-startup",     // log出SDK初始化相关事件
//                "--log-net-log",       // log出SDK网络相关事件
                "--enable-logging=stderr",
                "--auto-login=false",    // 不自动登录
                "--product-agent=2001",  //agent
                "--log-level=0",
                "--network-manually=true",
                "--net-env=" + env}, mContext);

        FileMgrTracker.TA_AppOncreate(ctx);
        long curTime2 = System.currentTimeMillis();
        Log.d(TAG,"initFileMgr comsume time = " + (curTime2- curTime) + "ms");
        FileMgrCore.init();
    }

    private static boolean copyAssetFolder(String fromAssetPath, String toPath) {
        try {
            File toFile = new File(toPath);
            if (toFile.exists() && toFile.isDirectory()) {
                return false;
            }
            toFile.mkdir();
            String[] files = mContext.getAssets().list(fromAssetPath);
            boolean res = true;
            for (String file : files) {
                if (file.contains(".")) {
                    res &= copyAsset(fromAssetPath + "/" + file,
                            toPath + "/" + file);
                } else {
                    res &= copyAssetFolder(fromAssetPath + "/" + file,
                            toPath + "/" + file);
                }
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAsset(String fromAssetPath, String toPath) {
        File toFile = new File(toPath);
        boolean rt = false;
        if (!toFile.exists()) {
            try {
                toFile.createNewFile();
                InputStream is = mContext.getAssets().open(fromAssetPath);
                rt = FileUtil.copyToFile(is, toFile);
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rt;
    }
}
