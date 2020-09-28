package com.candy.capture.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import com.candy.capture.R;
import com.candy.capture.core.SharedPreferenceManager;
import com.candy.capture.service.LocationService;
import com.jaeger.library.StatusBarUtil;

public class SplashActivity extends BaseActivity {

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 99) {
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                finish();
            }
            return true;
        }
    });

    private Runnable mShowFengDong = new Runnable() {
        @Override
        public void run() {
            mAction1Iv.setVisibility(View.VISIBLE);
            mAction2Iv.setVisibility(View.VISIBLE);

            AnimationSet animationSetOut = new AnimationSet(true);
            animationSetOut.setDuration(300);
            animationSetOut.setInterpolator(new AccelerateInterpolator());
            AlphaAnimation alphaOut = new AlphaAnimation(1, 0);
            animationSetOut.addAnimation(alphaOut);
            TranslateAnimation translateOut = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, -1,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0);
            animationSetOut.addAnimation(translateOut);

            AnimationSet animationSetIn = new AnimationSet(true);
            animationSetIn.setDuration(300);
            animationSetOut.setInterpolator(new DecelerateInterpolator());
            AlphaAnimation alphaIn = new AlphaAnimation(0, 1);
            animationSetIn.addAnimation(alphaIn);
            TranslateAnimation translateIn = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 1,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0);
            animationSetIn.addAnimation(translateIn);

            mAction1Iv.startAnimation(animationSetOut);
            mAction2Iv.startAnimation(animationSetIn);

            animationSetIn.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mAction1Iv.setVisibility(View.GONE);
                    mHandler.postDelayed(mShowXinDong, 500);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
    };

    private Runnable mShowXinDong = new Runnable() {
        @Override
        public void run() {
            mAction3Iv.setVisibility(View.VISIBLE);

            AnimationSet animationSetOut = new AnimationSet(true);
            animationSetOut.setDuration(300);
            animationSetOut.setInterpolator(new AccelerateInterpolator());
            AlphaAnimation alphaOut = new AlphaAnimation(1, 0);
            animationSetOut.addAnimation(alphaOut);
            TranslateAnimation translateOut = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, -1,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0);
            animationSetOut.addAnimation(translateOut);

            AnimationSet animationSetIn = new AnimationSet(true);
            animationSetIn.setDuration(300);
            animationSetOut.setInterpolator(new DecelerateInterpolator());
            AlphaAnimation alphaIn = new AlphaAnimation(0, 1);
            animationSetIn.addAnimation(alphaIn);
            TranslateAnimation translateIn = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 1,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0);
            animationSetIn.addAnimation(translateIn);

            mAction2Iv.startAnimation(animationSetOut);
            mAction3Iv.startAnimation(animationSetIn);

            animationSetIn.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mAction2Iv.setVisibility(View.GONE);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(99), 500);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
    };

    private ImageView mAction1Iv;
    private ImageView mAction2Iv;
    private ImageView mAction3Iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        findView();
        setView();

        StatusBarUtil.setTranslucent(this, 0);
    }

    private void findView() {
        mAction1Iv = (ImageView) findViewById(R.id.iv_action1);
        mAction2Iv = (ImageView) findViewById(R.id.iv_action2);
        mAction3Iv = (ImageView) findViewById(R.id.iv_action3);
    }

    private void setView() {
        if (SharedPreferenceManager.getInstance(this).isFirstRun()) {
            mHandler.postDelayed(mShowFengDong, 1000);
        } else {
            mAction1Iv.setVisibility(View.GONE);
            mAction2Iv.setVisibility(View.GONE);
            mAction3Iv.setVisibility(View.VISIBLE);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(99), 500);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void startLocationService() {
        // 权限未拿到时从Main去请求权限、启动定位Service，拿到以后从Splash启动
        // 低版本直接定位，高版本请求权限通过后定位
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(mContext, LocationService.class);
                startService(intent);
            }
        } else {
            Intent intent = new Intent(mContext, LocationService.class);
            startService(intent);
        }
    }
}
