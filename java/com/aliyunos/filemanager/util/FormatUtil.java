package com.aliyunos.filemanager.util;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FormatUtil {
    public static String formatCapacitySize(long size) {
        if (size == 0) {
            return "0M";
        }
        DecimalFormat df = new DecimalFormat("#.00");
        String str = "";
        if (size < 1024) {
            str = df.format((double) size) + "B";
        } else if (size < 1048576) {
            str = df.format((double) size / 1024) + "K";
        } else if (size < 1073741824) {
            str = df.format((double) size / 1048576) + "M";
        } else {
            str = df.format((double) size / 1073741824) + "G";
        }
        return str;
    }

    public static String formatTime(Context context, long time) {
        boolean flag = DateFormat.is24HourFormat(context);
        String language = Locale.getDefault().getLanguage();
        SimpleDateFormat formatter = null;
        if (flag) {
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        } else if (language.equals("en")){
            formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aa");
        } else {
            formatter = new SimpleDateFormat("yyyy-MM-dd aa hh:mm:ss");
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return formatter.format(cal.getTime());
    }
    
    /*
     *add by hjw at 2016/9/9
     *总存储量 
     */
    public static String totalStorage(long romTotal,long doovTotal,long flag){
    	long gb = 1024 * 1024 * 1024;
    	long romTemp = romTotal / gb;
    	long gapValue = doovTotal - romTotal;
    	// 首先判断手机总内存的大小在哪个区间
    	int rom = 0;
    	DecimalFormat df = new DecimalFormat("#.00");
    	if (romTotal > 0) {
			if (romTemp <= 16 ) {
				rom = 16;
			}else if(romTemp > 16 && romTemp <= 32 ){
				rom = 32;
			}else if(romTemp > 32  && romTemp <= 64 ) {
				rom = 64;
			}else if(romTemp > 64  && romTemp <= 128 ){
				rom = 128;
			}else if(romTemp > 128 && romTemp <= 256 ){
				rom = 256;
			}else {
				rom = 256;
			}
		}
    	
    	if (flag == 1) {
			return rom + "G";
		}else {
			return df.format((double) (rom * gb + gapValue)/gb) + "G";
		}
    }
    
    /* 
     * add by hjw at 2016/9/9 
     * 手机可用内存
     */
    public static String avalStorage(long romAval,long sdAval){
    	long size = romAval + sdAval;
    	if (size == 0) {
            return "0M";
        }
        DecimalFormat df = new DecimalFormat("#.00");
        String str = "";
        if (size < 1024) {
            str = df.format((double) size) + "B";
        } else if (size < 1048576) {
            str = df.format((double) size / 1024) + "K";
        } else if (size < 1073741824) {
            str = df.format((double) size / 1048576) + "M";
        } else {
            str = df.format((double) size / 1073741824) + "G";
        }
        return str;
    }
}
