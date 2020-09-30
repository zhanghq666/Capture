package com.candy.commonlibrary.utils;

import android.util.Log;

/**
 * Created by zhanghq on 2016/11/30.
 */

public class LogUtil {

    private static boolean isDebug = true;

    public static void disableLog() {
        isDebug = false;
    }

    public static void i(String tag, String msg) {
        if (isDebug) {
            Log.i(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (isDebug) {
            Log.d(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (isDebug) {
            Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (isDebug) {
            Log.e(tag, msg);
        }
    }
}
