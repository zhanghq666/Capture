package com.candy.capture.util;

/**
 * Created by zhanghq on 2016/11/10.
 */

public class TimeUtil {
    public static String formatDuration(long duration) {
        String time;
        if (duration< 60) {
            time = duration + "\"";
        } else {
            time = String.format("%d'%d\"", duration / 60, duration % 60);
        }
        return time;
    }
}
