package com.candy.capture.core;

import android.app.Application;

import com.candy.capture.util.FileUtil;

/**
 * Created by zhanghq on 2016/10/19.
 */

public class CaptureApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FileUtil.initRootFolder(this);
    }
}
