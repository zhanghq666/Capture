package com.candy.capture.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.candy.capture.core.SharedPreferenceManager
import com.candy.capture.service.FloatingWindowService

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 15:27
 */
class CaptureReceiver: BroadcastReceiver() {
    private val TAG = "CaptureReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action:" + intent.action)
        if (SharedPreferenceManager.getInstance(context).isAllowFastCapture()) {
            val intent1 = Intent(context, FloatingWindowService::class.java)
            context.startService(intent1)
        }
    }
}