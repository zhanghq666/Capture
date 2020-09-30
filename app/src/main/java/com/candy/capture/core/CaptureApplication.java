package com.candy.capture.core;

import android.app.Application;
import android.content.Intent;

import com.candy.capture.BuildConfig;
import com.candy.capture.service.FloatingWindowService;
import com.candy.capture.util.FileUtil;
import com.candy.commonlibrary.utils.LogUtil;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by zhanghq on 2016/10/19.
 */

public class CaptureApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FileUtil.initRootFolder(this);
        if (!BuildConfig.DEBUG) {
            MobclickAgent.setDebugMode(true);
            LogUtil.disableLog();
        }

//        Intent intent = new Intent(this, FloatingWindowService.class);
//        startService(intent);
    }
}
