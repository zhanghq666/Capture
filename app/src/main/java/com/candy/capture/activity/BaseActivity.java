package com.candy.capture.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.candy.capture.core.BackStackManager;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by zhanghq on 2016/10/26.
 */

public class BaseActivity extends AppCompatActivity {

    protected Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        BackStackManager.getInstance().push(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BackStackManager.getInstance().removeSpecificActivity(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
