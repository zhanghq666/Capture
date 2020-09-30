package com.candy.capture.util

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 15:47
 */
object TimeUtil {
    /**
     *
     * @param duration 秒
     * @return
     */
    fun formatDuration(duration: Long): String? {
        return String.format("%02d:%02d", duration / 60, duration % 60)
    }
}