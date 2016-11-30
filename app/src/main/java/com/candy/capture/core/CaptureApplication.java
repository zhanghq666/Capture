package com.candy.capture.core;

import android.app.Application;
import android.content.Intent;

import com.candy.capture.service.FloatingWindowService;
import com.candy.capture.util.FileUtil;

/**
 * Created by zhanghq on 2016/10/19.
 */

public class CaptureApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FileUtil.initRootFolder(this);
        // 正式发布后注释掉下面这句代码
//        MobclickAgent.setDebugMode(true);

        Intent intent = new Intent(this, FloatingWindowService.class);
        startService(intent);
    }
}
