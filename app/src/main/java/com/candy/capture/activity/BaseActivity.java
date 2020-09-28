package com.candy.capture.activity;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.candy.capture.R;
import com.candy.capture.core.BackStackManager;
import com.jaeger.library.StatusBarUtil;
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

        StatusBarUtil.setDarkMode(this);
        StatusBarUtil.setColor(this, getResources().getColor(R.color.colorPrimary), 0);
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
