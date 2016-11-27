package com.candy.capture.core;

import android.app.Application;

import com.candy.capture.util.FileUtil;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by zhanghq on 2016/10/19.
 */

public class CaptureApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FileUtil.initRootFolder(this);
        // 正式发布后注释掉下面这句代码
        MobclickAgent.setDebugMode(true);
    }
}
