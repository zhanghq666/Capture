package com.candy.capture.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.candy.capture.R
import com.candy.capture.core.BackStackManager
import com.jaeger.library.StatusBarUtil
import com.umeng.analytics.MobclickAgent

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 10:44
 */
open class BaseActivity: AppCompatActivity() {
    protected lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        BackStackManager.getInstance().push(this)
        StatusBarUtil.setDarkMode(this)
        StatusBarUtil.setColor(this, resources.getColor(R.color.colorPrimary), 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        BackStackManager.getInstance().removeSpecificActivity(this)
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}