package com.aliyunos.filemanager.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;

import yunos.ui.util.ReflectHelper;

/**
 * Created by sjjwind on 2/23/16.
 */
public class DeviceUtil {
    private static String TAG = "DeviceUtil";
    private static final String YUNOS_BUILD_VERSION_FIELD_KEY="YUNOS_BUILD_VERSION";

    public static String getUUID() {
        Class<?> clazz = null;
        Method method = null;
        String uuid = null;
        try {
            clazz = Class.forName("android.os.SystemProperties");
            method = clazz.getDeclaredMethod("get", new Class[] {
                    String.class
            });
            uuid = (String) method.invoke(null, new Object[] {
                    "ro.aliyun.clouduuid"
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clazz = null;
            method = null;
        }
        Log.d(TAG, "uuid = " + uuid);
        return uuid;
    }

    public static String getYunosVersion(){
        String yunosVersion ="";

        Object versionReflect = null;

        try {
            versionReflect = Build.class.getField(YUNOS_BUILD_VERSION_FIELD_KEY).get(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        if(versionReflect != null && versionReflect instanceof String){
            yunosVersion = (String) versionReflect;
        }else{
            yunosVersion = Build.VERSION.RELEASE;
        }

        return yunosVersion;
    }

    public static String getAppVersion(Context context){
        String appVersion="";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            appVersion= info.versionName;

        } catch (NameNotFoundException e){
            e.printStackTrace();
        }

        return appVersion;
    }


    static Method systemProperties_get = null;
    public static String getDeviceType() {
        String device_type = "";
        if (systemProperties_get == null) {
            try {
                systemProperties_get = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
            } catch (Exception e) {
                Log.e(TAG, "get method android.os.SystemProperties.get() error:" + e);
                return null;
            }
        }
        try {
            device_type = (String) systemProperties_get.invoke(null, "ro.yunos.device.device_type");
        } catch (Exception e) {
            Log.e(TAG, "invoke method android.os.SystemProperties.get() error:" + e);
            return null;
        }
        return device_type;
    }

    public static String getDeviceModel() {
        String device_model = null;
        if (systemProperties_get == null) {
            try {
                systemProperties_get = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
            } catch (Exception e) {
                Log.e(TAG, "get method android.os.SystemProperties.get() error:" + e);
            }
        }
        if (systemProperties_get != null) {
            try {
                device_model = (String) systemProperties_get.invoke(null, "ro.yunos.model");
            } catch (Exception e) {
                Log.e(TAG, "invoke method android.os.SystemProperties.get() error:" + e);
            }
        }
        if (TextUtils.isEmpty(device_model)) {
            device_model = Build.MODEL;
        }
        return device_model;
    }


}
