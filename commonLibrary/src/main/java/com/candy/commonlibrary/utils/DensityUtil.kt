package com.candy.commonlibrary.utils

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 16:20
 */
object DensityUtil {
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     *
     * @param context
     * @param dpValue
     * @return
     */
    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     *
     * @param context
     * @param pxValue
     * @return
     */
    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun getScreenRealSize(context: Activity): Point {
        var width = 0
        var height = 0
        val metrics = DisplayMetrics()
        val display = context.windowManager.defaultDisplay
        var mGetRawH: Method? = null
        var mGetRawW: Method? = null
        try {
            // For JellyBean 4.2 (API 17) and onward
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealMetrics(metrics)
                width = metrics.widthPixels
                height = metrics.heightPixels
            } else {
                mGetRawH = Display::class.java.getMethod("getRawHeight")
                mGetRawW = Display::class.java.getMethod("getRawWidth")
                try {
                    mGetRawH.isAccessible = true
                    mGetRawW.isAccessible = true
                    width = mGetRawW.invoke(display) as Int
                    height = mGetRawH.invoke(display) as Int
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            }
        } catch (e3: NoSuchMethodException) {
            e3.printStackTrace()
        }
        return Point(width, height)
    }
}