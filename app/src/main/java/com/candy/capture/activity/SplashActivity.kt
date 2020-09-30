package com.candy.capture.activity

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.*
import android.widget.ImageView
import com.candy.capture.R
import com.candy.capture.core.SharedPreferenceManager
import com.candy.capture.service.LocationService
import com.jaeger.library.StatusBarUtil

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 10:45
 */
class SplashActivity: BaseActivity() {
    private val mHandler = Handler { msg ->
        if (msg.what == 99) {
            val intent = Intent(mContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        true
    }

    private val mShowFengDong = Runnable {
        mAction1Iv!!.visibility = View.VISIBLE
        mAction2Iv!!.visibility = View.VISIBLE
        val animationSetOut = AnimationSet(true)
        animationSetOut.duration = 300
        animationSetOut.interpolator = AccelerateInterpolator()
        val alphaOut = AlphaAnimation(1f, 0f)
        animationSetOut.addAnimation(alphaOut)
        val translateOut = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, -1f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f)
        animationSetOut.addAnimation(translateOut)
        val animationSetIn = AnimationSet(true)
        animationSetIn.duration = 300
        animationSetOut.interpolator = DecelerateInterpolator()
        val alphaIn = AlphaAnimation(0f, 1f)
        animationSetIn.addAnimation(alphaIn)
        val translateIn = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f)
        animationSetIn.addAnimation(translateIn)
        mAction1Iv!!.startAnimation(animationSetOut)
        mAction2Iv!!.startAnimation(animationSetIn)
        animationSetIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                mAction1Iv!!.visibility = View.GONE
                mHandler.postDelayed(mShowXinDong, 500)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    private val mShowXinDong = Runnable {
        mAction3Iv!!.visibility = View.VISIBLE
        val animationSetOut = AnimationSet(true)
        animationSetOut.duration = 300
        animationSetOut.interpolator = AccelerateInterpolator()
        val alphaOut = AlphaAnimation(1f, 0f)
        animationSetOut.addAnimation(alphaOut)
        val translateOut = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, -1f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f)
        animationSetOut.addAnimation(translateOut)
        val animationSetIn = AnimationSet(true)
        animationSetIn.duration = 300
        animationSetOut.interpolator = DecelerateInterpolator()
        val alphaIn = AlphaAnimation(0f, 1f)
        animationSetIn.addAnimation(alphaIn)
        val translateIn = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 1f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f)
        animationSetIn.addAnimation(translateIn)
        mAction2Iv!!.startAnimation(animationSetOut)
        mAction3Iv!!.startAnimation(animationSetIn)
        animationSetIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                mAction2Iv!!.visibility = View.GONE
                mHandler.sendMessageDelayed(mHandler.obtainMessage(99), 500)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    private var mAction1Iv: ImageView? = null
    private var mAction2Iv: ImageView? = null
    private var mAction3Iv: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findView()
        setView()
        StatusBarUtil.setTranslucent(this, 0)
    }

    private fun findView() {
        mAction1Iv = findViewById<View>(R.id.iv_action1) as ImageView
        mAction2Iv = findViewById<View>(R.id.iv_action2) as ImageView
        mAction3Iv = findViewById<View>(R.id.iv_action3) as ImageView
    }

    private fun setView() {
        if (SharedPreferenceManager.getInstance(this).isFirstRun()) {
            mHandler.postDelayed(mShowFengDong, 1000)
        } else {
            mAction1Iv!!.visibility = View.GONE
            mAction2Iv!!.visibility = View.GONE
            mAction3Iv!!.visibility = View.VISIBLE
            mHandler.sendMessageDelayed(mHandler.obtainMessage(99), 500)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun startLocationService() {
        // 权限未拿到时从Main去请求权限、启动定位Service，拿到以后从Splash启动
        // 低版本直接定位，高版本请求权限通过后定位
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(mContext, LocationService::class.java)
                startService(intent)
            }
        } else {
            val intent = Intent(mContext, LocationService::class.java)
            startService(intent)
        }
    }
}