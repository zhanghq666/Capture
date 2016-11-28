package com.candy.capture.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.candy.capture.R;
import com.candy.capture.util.TipsUtil;

public class FloatingWindowService extends Service {

    private static final String TAG = "FloatingWindowService";

    public static final String EXTRA_TOOGLE = "extra_toogle";

    WindowManager.LayoutParams wmParams;
    WindowManager mWindowManager;
    private RelativeLayout mFloatLayout;
    private ImageButton mHomeBtn;

    private boolean mIsAdded;

    public FloatingWindowService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        boolean openWindow = intent.getBooleanExtra(EXTRA_TOOGLE, true);
        if (openWindow) {
            addFloatView();
        } else {
            removeFloatView();
        }
        return new InnerBinder();
    }

    public void addFloatView() {
        if (mIsAdded)
            return;
        wmParams = new WindowManager.LayoutParams();
        //获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(getApplication().WINDOW_SERVICE);
        Log.d(TAG, "mWindowManager--->" + mWindowManager);
        //设置window type
        wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

         /*// 设置悬浮窗口长宽数据
        wmParams.width = 200;
        wmParams.height = 80;*/

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (RelativeLayout) inflater.inflate(R.layout.floatint_layout, null);
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);
        mIsAdded = true;

        //浮动窗口按钮
        mHomeBtn = (ImageButton) mFloatLayout.findViewById(R.id.ib_home);

        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        Log.i(TAG, "Width/2--->" + mHomeBtn.getMeasuredWidth() / 2);
        Log.i(TAG, "Height/2--->" + mHomeBtn.getMeasuredHeight() / 2);
        //设置监听浮动窗口的触摸移动
        mHomeBtn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //getRawX是触摸位置相对于屏幕的坐标，getX是相对于按钮的坐标
                wmParams.x = (int) event.getRawX() - mHomeBtn.getMeasuredWidth() / 2;
                Log.i(TAG, "RawX" + event.getRawX());
                Log.i(TAG, "X" + event.getX());
                //减25为状态栏的高度
                wmParams.y = (int) event.getRawY() - mHomeBtn.getMeasuredHeight() / 2 - 25;
                Log.i(TAG, "RawY" + event.getRawY());
                Log.i(TAG, "Y" + event.getY());
                //刷新
                mWindowManager.updateViewLayout(mFloatLayout, wmParams);
                return false;
            }
        });

        mHomeBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                TipsUtil.showToast(FloatingWindowService.this, "Home Btn Click");
            }
        });
    }

    public void removeFloatView() {
        if (mFloatLayout != null && mIsAdded) {
            mIsAdded = false;
            //移除悬浮窗口
            mWindowManager.removeView(mFloatLayout);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeFloatView();
    }

    public void toggleFloatView(boolean toggle) {
        if (toggle) {
            addFloatView();
        } else {
            removeFloatView();
        }
    }

    public class InnerBinder extends Binder {
        public FloatingWindowService getService() {
            return FloatingWindowService.this;
        }
    }
}
