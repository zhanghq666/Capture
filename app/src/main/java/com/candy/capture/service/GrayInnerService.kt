package com.candy.capture.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.candy.capture.core.ConstantValues

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 15:40
 */
class GrayInnerService: Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(ConstantValues.FLOAT_SERVICE_ID, Notification())
        stopForeground(true)
        stopSelf()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}