package com.candy.capture.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.candy.capture.core.SharedPreferenceManager;
import com.candy.capture.service.FloatingWindowService;

public class CaptureReceiver extends BroadcastReceiver {
    private static final String TAG = "CaptureReceiver";

    public CaptureReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (SharedPreferenceManager.getInstance(context).isAllowFastCapture()) {
            Intent intent1 = new Intent(context, FloatingWindowService.class);
            context.startService(intent1);
        }
    }
}
