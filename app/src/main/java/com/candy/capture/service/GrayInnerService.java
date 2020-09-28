package com.candy.capture.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.candy.capture.core.ConstantValues;

/**
 * 给 API >= 18 的平台上用的灰色保活手段
 */
public class GrayInnerService extends Service {

    public GrayInnerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(ConstantValues.FLOAT_SERVICE_ID, new Notification());
        stopForeground(true);
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
