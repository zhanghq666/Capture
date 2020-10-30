package com.candy.capture.util

/**
 * @Description 该工具类会在xml数据绑定里用到，必须写成companion object + @JvmStatic的形式，数据绑定才认，
 * 所以这里没有使用对象类，因为kotlin对象类的实现其实是一个静态实例在调用非静态方法，而非真正意义上的静态方法。
 * @Author zhanghaiqiang
 * @Date 2020/10/20 9:47
 */
class TimeUtils {
    companion object {
        /**
         *  格式化时间
         * @param duration 秒
         * @return
         */
        @JvmStatic
        fun formatDuration(duration: Long): String? {
            return String.format("%02d:%02d", duration / 60, duration % 60)
        }
    }
}