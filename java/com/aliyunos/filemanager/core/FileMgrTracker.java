package com.aliyunos.filemanager.core;

import android.app.Activity;
import android.content.Context;

import com.aliyun.ams.ta.StatConfig;
import com.aliyun.ams.ta.TA;

public class FileMgrTracker {
    public final static boolean bIsCloseUserTrack = false;
    static int LastPageId = 0;

    final static String []PageString = {
            "Page_HomeShell",
            "Page_CategoryBrowse",
            "Page_AllFiles",
            "Page_OnlineFiles",
            "Page_Guide"
    };

    public static void TA_Click(String  Button) {
        if(bIsCloseUserTrack)
            return;

        TA.getInstance().getDefaultTracker().commitEvent(PageString[LastPageId], 2101,Button, null, null, null);
    }

    public static void TA_EnterMode(String type) {
        if(bIsCloseUserTrack)
            return;

        TA.getInstance().getDefaultTracker().commitEvent(PageString[LastPageId], 2101,type, null, null, null);
    }

    public static void TA_EnterPage(int PageTo,int by) {
        if(bIsCloseUserTrack)
            return;

        if((PageTo >= PageString.length)||(PageTo < 0))
            return;

        TA.getInstance().getDefaultTracker().pageLeave(PageString[LastPageId]);
        TA.getInstance().getDefaultTracker().pageEnter(PageString[PageTo]);
        LastPageId = PageTo;
    }

    public static void TA_ActivityStart(Activity pActivity) {
        if(bIsCloseUserTrack)
            return;
        TA.getInstance().getDefaultTracker().activityStart(pActivity);

    }
    public static void TA_ActivityStop(Activity pActivity) {
        if(bIsCloseUserTrack)
            return;
        TA.getInstance().getDefaultTracker().activityStop(pActivity);

    }
    public static void TA_AppOncreate(Context pActivity) {
        if(bIsCloseUserTrack)
            return;

        StatConfig.getInstance().setContext(pActivity);
        com.aliyun.ams.ta.Tracker lTracker = TA.getInstance().getTracker("21736480");
        TA.getInstance().setDefaultTracker(lTracker);
    }
}
