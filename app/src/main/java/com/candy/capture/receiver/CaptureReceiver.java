package com.candy.capture.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.candy.capture.core.SharedReferenceManager;
import com.candy.capture.service.FloatingWindowService;
import com.candy.capture.util.LogUtil;
import com.candy.capture.util.TipsUtil;

public class CaptureReceiver extends BroadcastReceiver {
    private static final String TAG = "CaptureReceiver";

    public CaptureReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (SharedReferenceManager.getInstance(context).isAllowFastCapture()) {
            Intent intent1 = new Intent(context, FloatingWindowService.class);
            context.startService(intent1);
        }
    }
}
