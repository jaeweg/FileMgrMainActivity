package com.aliyunos.filemanager;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import android.util.SparseIntArray;

import com.aliyunos.filemanager.core.FileMgrCore;
import com.aliyunos.filemanager.core.FileMgrCoreInitializer;

import yunos.ui.util.DynColorSetting;

public final class FileMgrApplication extends Application {
    private static final String TAG = "FileMgrApplication";

    private static FileMgrApplication sInstance = null;
    private DynColorSetting mDynColorSetting;

    public FileMgrApplication() { sInstance = this; }

    public static FileMgrApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        mDynColorSetting = new DynColorSetting(getResources().getConfiguration());
        DynColorSetting.setColorIDReady(getResources(), getPackageName());
        overlayDynColorRes(getResources(), mDynColorSetting);
        super.onCreate();

        Log.d(TAG, "FileManagerApplication.onCreate");
        FileMgrCoreInitializer.init(this);
        FileMgrCore.doFirstScan(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if( !mDynColorSetting.isSameColorMap(newConfig) ) {
            mDynColorSetting.updateColorMap(newConfig);
            overlayDynColorRes(getResources(), mDynColorSetting);
        }
        super.onConfigurationChanged(newConfig);
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "onTerminate");
    }

    public void overlayDynColorRes(Resources res, DynColorSetting dynColorSetting) {
        DynColorSetting.clearNewResArray(res);
        if ( !dynColorSetting.isRestoreMode() ) {
            SparseIntArray newcolors = new SparseIntArray();

            //head
            int bgcolor = dynColorSetting.getColorValue(DynColorSetting.HEADER_COLOR,getResources().getColor(R.color.setting_header_color));
            newcolors.put(R.color.setting_header_color, bgcolor);

            //text
            int textcolor = dynColorSetting.getColorValue(DynColorSetting.HEADER_TEXT_COLOR, getResources().getColor(R.color.tab_text_select_color));
            newcolors.put(R.color.tab_text_select_color, textcolor);

            int textUncheckColor = dynColorSetting.getColorValue(DynColorSetting.HEADER_TEXT_COLOR_UNCHECKED,
                    getResources().getColor(R.color.tab_text_unselect_color));
            newcolors.put(R.color.tab_text_unselect_color,textUncheckColor);

            int selectedTextColor = dynColorSetting.getColorValue(DynColorSetting.HEADER_TEXT_COLOR,getResources().getColor(R.color.select_text_color));
            newcolors.put(R.color.select_text_color,selectedTextColor);

            //searchBackIcon
            int searchBackIconColor = dynColorSetting.getColorValue(DynColorSetting.HW_COLOR_PRIMARY,
                    getResources().getColor(R.color.hw_color_primary));
            newcolors.put(R.color.hw_color_primary,searchBackIconColor);

            int searchBackIconColorDark = dynColorSetting.getColorValue(DynColorSetting.HW_COLOR_PRIMARY_DARK,
                    getResources().getColor(R.color.hw_color_primary_dark));
            newcolors.put(R.color.hw_color_primary_dark,searchBackIconColorDark);

            int searchBackIconColorDisable = dynColorSetting.getColorValue(DynColorSetting.HW_COLOR_PRIMARY_DISABLED,
                    getResources().getColor(R.color.hw_color_primary_disabled));
            newcolors.put(R.color.hw_color_primary_disabled,searchBackIconColorDisable);

            //AcionBarBackIcon
            int ActionBarBackIconColor = dynColorSetting.getColorValue(DynColorSetting.HEADER_WIDGET_NORMAL,
                    getResources().getColor(R.color.back_btn_color));
            newcolors.put(R.color.back_btn_color,ActionBarBackIconColor);

            int ActionBarBackIconColorDark = dynColorSetting.getColorValue(DynColorSetting.HEADER_WIDGET_PRESSED,
                    getResources().getColor(R.color.back_btn_color_dark));
            newcolors.put(R.color.back_btn_color_dark,ActionBarBackIconColorDark);

            int ActionBarBackIconColorDisable = dynColorSetting.getColorValue(DynColorSetting.HEADER_WIDGET_DISABLE,
                    getResources().getColor(R.color.back_btn_color_disable));
            newcolors.put(R.color.back_btn_color_disable,ActionBarBackIconColorDisable);

            //login_btn
            int loginBtn_normalColor = dynColorSetting.getColorValue(DynColorSetting.HW_COLOR_PRIMARY,
                    getResources().getColor(R.color.login_button_normal_color));
            newcolors.put(R.color.login_button_normal_color,loginBtn_normalColor);

            int loginBtn_pressColor = dynColorSetting.getColorValue(DynColorSetting.HW_COLOR_PRIMARY_DARK,
                    getResources().getColor(R.color.login_button_press_color));
            newcolors.put(R.color.login_button_press_color, loginBtn_pressColor);

            int loginBtn_disableColor = dynColorSetting.getColorValue(dynColorSetting.HW_COLOR_PRIMARY_DISABLED,
                    getResources().getColor(R.color.login_button_disable_color));
            newcolors.put(R.color.login_button_disable_color, loginBtn_disableColor);

            //darkMode
            int darkmode = dynColorSetting.getDarkMode(getResources().getBoolean(R.bool.status_dark_mode));
            newcolors.put(R.bool.status_dark_mode, darkmode);
            DynColorSetting.setNewAUIDynColorRes(this, dynColorSetting,  newcolors);
        }
    }
}
