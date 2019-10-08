package com.aliyunos.filemanager.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.aliyunos.filemanager.FileMgrApplication;

public class PreferenceUtil {
    private static final String SETTINGS_FILENAME = "com.aliyunos.filemanager";

    public enum Setting {
        SETTINGS_FIRST_USE("filemanager_first_use", Boolean.TRUE),
        SETTINGS_SHOW_HIDDEN("filemanager_show_hidden", Boolean.FALSE),
        SETTINGS_KEY_ORDER("filemanager_key_order", Integer.valueOf(0)),
        SETTINGS_CATEGORY_KEY_ORDER("filemanager_category_key_order", Integer.valueOf(2)),
        SETTINGS_SHOW_TIPS("showTips",Boolean.FALSE),
        SETTTING_LOGIN_ID("havanaid",""),
        SETTTING_SPACE_WARNING("space_warning", Long.valueOf(0)),
        SETTTING_CLOSE_CLOUD("closecloud", Boolean.FALSE);


        private final String mId;
        private final Object mDefaultValue;

        private Setting(String id, Object defaultValue) {
            mId = id;
            mDefaultValue = defaultValue;
        }

        public String getId() { return this.mId; }
        public Object getDefaultValue() { return this.mDefaultValue; }

        public static Setting fromId(String id) {
            Setting[] values = values();
            for (int i = 0; i < values.length; i++) {
                if (values[i].mId == id) {
                    return values[i];
                }
            }
            return null;
        }
    }

    static SharedPreferences getSharedPreferences() {
        return FileMgrApplication.getInstance().getSharedPreferences(
                SETTINGS_FILENAME, Context.MODE_PRIVATE);
    }

    public static Boolean getBoolean(Setting setting) {
        return getSharedPreferences().getBoolean(setting.getId(), (Boolean) setting.getDefaultValue());
    }

    public static void setBoolean(Setting setting, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(setting.getId(), value);
        editor.commit();
    }

    public static void setInteger(Setting setting, int value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(setting.getId(), value);
        editor.commit();
    }
    public static int getInteger(Setting setting) {
        return getSharedPreferences().getInt(setting.getId(), (Integer)setting.getDefaultValue());
    }

    public static void setString(Setting setting, String value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(setting.getId(),value);
        editor.commit();
    }

    public static String getString(Setting setting) {
        return getSharedPreferences().getString(setting.getId(), (String)setting.getDefaultValue());
    }

    public static void setLong(Setting setting, long value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(setting.getId(),value);
        editor.commit();
    }

    public static long getLong(Setting setting) {
        return getSharedPreferences().getLong(setting.getId(), 0);
    }
}
