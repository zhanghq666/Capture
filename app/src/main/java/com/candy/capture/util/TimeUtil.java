package com.candy.capture.util;

/**
 * Created by zhanghq on 2016/11/10.
 */

public class TimeUtil {
    /**
     *
     * @param duration 秒
     * @return
     */
    public static String formatDuration(long duration) {
        String time = String.format("%02d:%02d", duration / 60, duration % 60);
        return time;
    }
}
