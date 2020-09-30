package com.candy.commonlibrary.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import java.io.*
import java.util.*
import java.util.regex.Pattern

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 16:18
 */
object CommonTools {
    private const val randomUUID_filePath = "data/data/%s/randomUUID.xml"

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

    /**
     * Returns whether the network is available
     */
    fun isNetworkAvailable(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val connectivity = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivity == null) {
            LogUtil.e("CommonTools", "couldn't get connectivity manager")
        } else {
            val info = connectivity.allNetworkInfo
            if (info != null) {
                var i = 0
                val length = info.size
                while (i < length) {
                    if (info[i].state == NetworkInfo.State.CONNECTED) {
                        return true
                    }
                    i++
                }
            }
        }
        return false
    }

    /**
     * Returns whether the network is mobile
     */
    fun isMobileNetwork(context: Context): Boolean {
        val connectivity = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivity == null) {
            // LogUtil.w(Constants.TAG, "couldn't get connectivity manager");
        } else {
            val info = connectivity.activeNetworkInfo
            if (info != null && info.type == ConnectivityManager.TYPE_MOBILE) {
                return true
            }
        }
        return false
    }

    fun isWifiNetwork(context: Context): Boolean {
        val connectivity = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivity == null) {
            // LogUtil.w(Constants.TAG, "couldn't get connectivity manager");
        } else {
            val info = connectivity.activeNetworkInfo
            if (info != null && info.type == ConnectivityManager.TYPE_WIFI) {
                return true
            }
        }
        return false
    }

    /**
     * 隐藏软键盘
     *
     * @param activity
     */
    fun hideSoftKeyboard(activity: Activity?) {
        if (null == activity) {
            return
        }
        try {
            val v = activity.window.peekDecorView()
            if (v != null && v.windowToken != null) {
                val imm = activity
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 键盘是否弹出了
     *
     * @param activity
     * @return
     */
    fun isKeyboardShowing(activity: Activity?): Boolean {
        var isShowing = false
        if (null != activity) {
            try {
                val v = activity.window.peekDecorView()
                if (null != v) {
                    val imm = activity
                            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    isShowing = imm.isActive(v)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return isShowing
    }

    /**
     * 使editText获取焦点并弹出键盘
     *
     * @param editText
     */
    fun showKeyboard(editText: EditText?) {
        if (editText == null) {
            return
        }
        editText.isFocusable = true
        editText.isFocusableInTouchMode = true
        editText.requestFocus()
        val imm = editText.context
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText,
                InputMethodManager.SHOW_FORCED) //.toggleSoftInput(0, InputMethodManager
        // .SHOW_FORCED);
    }


    /**
     * 获取设备唯一标识ID
     *
     * @param context
     * @return
     */
    fun getDeviceId(context: Context): String? {
        var deviceId: String? = ""
        try {
            deviceId = getIMIEStatus(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (TextUtils.isEmpty(deviceId) || "0" == deviceId) {
            try {
                deviceId = getLocalMac(context).replace(":", "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (TextUtils.isEmpty(deviceId) || "0" == deviceId) {
            try {
                deviceId = getAndroidId(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (TextUtils.isEmpty(deviceId) || "0" == deviceId) {
            try {
                deviceId = readRandomUUID(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (TextUtils.isEmpty(deviceId) || "0" == deviceId) {
                val uuid = UUID.randomUUID()
                deviceId = uuid.toString().replace("-", "")
                saveRandomUUID(deviceId, context)
            }
        }
        return deviceId
    }

    private fun replaceBlank(mString: String): String? {
        val p = Pattern.compile("\\s*|\t|\r|\n")
        val m = p.matcher(mString)
        return m.replaceAll("")
    }

    /**
     * 获取IMEI码
     *
     * @param context
     * @return
     */
    private fun getIMIEStatus(context: Context): String? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.deviceId
    }

    /**
     * 获取Mac地址
     *
     * @param context
     * @return
     */
    private fun getLocalMac(context: Context): String {
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifi.connectionInfo.macAddress
    }

    /**
     * 获取ANDROID_ID
     *
     * @param context
     * @return
     */
    private fun getAndroidId(context: Context): String? {
        return Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun saveRandomUUID(str: String, context: Context) {
        try {
            val file = File(String.format(randomUUID_filePath, context.packageName))
            val fos = FileOutputStream(file)
            val out: Writer = OutputStreamWriter(fos, "UTF-8")
            out.write(str)
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readRandomUUID(context: Context): String? {
        val buffer = StringBuffer()
        return try {
            val file = File(String.format(randomUUID_filePath, context.packageName))
            val fis = FileInputStream(file)
            val isr = InputStreamReader(fis, "UTF-8")
            val `in`: Reader = BufferedReader(isr)
            var i: Int
            while (`in`.read().also { i = it } > -1) {
                buffer.append(i.toChar())
            }
            `in`.close()
            buffer.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 安装APK
     *
     * @param context
     * @param apkFile
     */
    fun openApk(context: Context, apkFile: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setDataAndType(Uri.parse("file://" + apkFile.absolutePath),
                "application/vnd.android.package-archive")
        context.startActivity(intent)
    }
}