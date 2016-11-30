package com.candy.capture.util;

import android.util.Log;

/**
 * Created by zhanghq on 2016/11/30.
 */

public class LogUtil {
    public static final boolean ALLOW_LOG = true;

    public static void i(String tag, String msg) {
        if (ALLOW_LOG) {
            Log.i(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (ALLOW_LOG) {
            Log.d(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (ALLOW_LOG) {
            Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (ALLOW_LOG) {
            Log.e(tag, msg);
        }
    }
}
