package com.candy.capture.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.RelativeLayout
import com.candy.capture.IFloatAidlInterface
import com.candy.capture.R
import com.candy.capture.core.ConstantValues
import com.candy.capture.core.SharedPreferenceManager
import com.candy.commonlibrary.utils.LogUtil

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 15:37
 */
class FloatingWindowService : Service() {
    companion object {
        private val TAG = "FloatingWindowService"

        val EXTRA_TOGGLE = "extra_toggle"
    }

    var wmParams: WindowManager.LayoutParams? = null
    var mWindowManager: WindowManager? = null
    private var mFloatLayout: RelativeLayout? = null
    private var mHomeBtn: ImageButton? = null

    private var mIsAdded = false

    private val mHandler = Handler()

    private var mDownX = 0f
    private var mDownY: Float = 0f
    private var mCurrentX: Float = 0f
    private var mCurrentY: Float = 0f


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand " + SharedPreferenceManager.getInstance(this).isAllowFastCapture())
        if (Build.VERSION.SDK_INT < 18) {
            startForeground(ConstantValues.FLOAT_SERVICE_ID, Notification()) //API < 18 ，此方法能有效隐藏Notification上的图标
        } else {
            val innerIntent = Intent(this, GrayInnerService::class.java)
            startService(innerIntent)
            startForeground(ConstantValues.FLOAT_SERVICE_ID, Notification())
        }
        if (SharedPreferenceManager.getInstance(this).isAllowFastCapture()) {
            addFloatView()
        } else {
            removeFloatView()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        val openWindow = intent.getBooleanExtra(EXTRA_TOGGLE, true)
        LogUtil.d(TAG, "onBind openWindow--->$openWindow")
        if (openWindow) {
            addFloatView()
        } else {
            removeFloatView()
        }
        return InnerBinder()
    }

    fun addFloatView() {
        if (mIsAdded) return
        wmParams = WindowManager.LayoutParams()
        //获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager
        //设置window type
        wmParams!!.type = WindowManager.LayoutParams.TYPE_TOAST
        //设置图片格式，效果为背景透明
        wmParams!!.format = PixelFormat.RGBA_8888
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams!!.gravity = Gravity.LEFT or Gravity.TOP
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams!!.x = 0
        wmParams!!.y = 0

        //设置悬浮窗口长宽数据
        wmParams!!.width = WindowManager.LayoutParams.WRAP_CONTENT
        wmParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT

        //获取浮动窗口视图所在布局
        mFloatLayout = LayoutInflater.from(applicationContext).inflate(R.layout.floatint_layout, null) as RelativeLayout?
        mHandler.post {
            if (mFloatLayout!!.parent != null) {
                LogUtil.d(TAG, "mFloatLayout has parent")
                mWindowManager!!.removeView(mFloatLayout)
            }
            mWindowManager!!.addView(mFloatLayout, wmParams)
            mIsAdded = true
        }

        //浮动窗口按钮
        mHomeBtn = mFloatLayout!!.findViewById<View>(R.id.ib_home) as ImageButton
        mFloatLayout!!.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        //设置监听浮动窗口的触摸移动
        mHomeBtn!!.setOnTouchListener { v, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    mCurrentX = event.rawX
                    mCurrentY = event.rawY
                    mDownX = event.x
                    mDownY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    mCurrentX = event.rawX
                    mCurrentY = event.rawY
                    val newX = (mCurrentX - (mDownX - mHomeBtn!!.measuredWidth / 2)) as Int
                    val newY = (mCurrentY - (mDownY - mHomeBtn!!.measuredHeight / 2)) as Int
                    updatePosition(newX, newY)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                }
            }
            false
        }

//        mHomeBtn.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                TipsUtil.showToast(getApplicationContext(), "Home Btn Click");
//            }
//        });
    }

    private fun updatePosition(newX: Int, newY: Int) {
        Log.d(TAG, "updatePosition, x:$newX y:$newY")

        //getRawX是触摸位置相对于屏幕的坐标，getX是相对于按钮的坐标
        wmParams!!.x = newX - mHomeBtn!!.measuredWidth / 2
        wmParams!!.y = newY - mHomeBtn!!.measuredHeight / 2 - getStatusBarHeight()
        //刷新
        mWindowManager!!.updateViewLayout(mFloatLayout, wmParams)
    }

    /**
     * 用于获取状态栏的高度。
     *
     * @return 返回状态栏高度的像素值。
     */
    private fun getStatusBarHeight(): Int {
        var height = 0
        try {
            val c = Class.forName("com.android.internal.R\$dimen")
            val o = c.newInstance()
            val field = c.getField("status_bar_height")
            val x = field[o] as Int
            height = resources.getDimensionPixelSize(x)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return height
    }

    fun removeFloatView() {
        if (mIsAdded) {
            mHandler.post {
                mIsAdded = false
                //移除悬浮窗口
                LogUtil.d(TAG, "removeViewImmediate")
                mWindowManager!!.removeViewImmediate(mFloatLayout)
            }
        }
    }

    fun toggleFloatView(toggle: Boolean) {
        LogUtil.d(TAG, "toggleFloatView toggle=$toggle")
        if (toggle) {
            addFloatView()
        } else {
            removeFloatView()
        }
    }

    override fun onDestroy() {
        LogUtil.d(TAG, "onDestroy")
        super.onDestroy()
        removeFloatView()
    }

    inner class InnerBinder : IFloatAidlInterface.Stub() {
        @Throws(RemoteException::class)
        override fun toggleFloatWindow(toggle: Boolean) {
            toggleFloatView(toggle)
        }
    }
}