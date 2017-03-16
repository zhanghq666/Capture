package com.candy.capture.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.candy.capture.IFloatAidlInterface;
import com.candy.capture.R;
import com.candy.capture.core.ConstantValues;
import com.candy.capture.core.SharedPreferenceManager;
import com.candy.capture.util.LogUtil;
import com.candy.capture.util.TipsUtil;

import java.lang.reflect.Field;

public class FloatingWindowService extends Service {

    private static final String TAG = "FloatingWindowService";

    public static final String EXTRA_TOGGLE = "extra_toggle";

    WindowManager.LayoutParams wmParams;
    WindowManager mWindowManager;
    private RelativeLayout mFloatLayout;
    private ImageButton mHomeBtn;

    private boolean mIsAdded;

    private Handler mHandler = new Handler();

    public FloatingWindowService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand " + SharedPreferenceManager.getInstance(this).isAllowFastCapture());
        if (Build.VERSION.SDK_INT < 18) {
            startForeground(ConstantValues.FLOAT_SERVICE_ID, new Notification());//API < 18 ，此方法能有效隐藏Notification上的图标
        } else {
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(ConstantValues.FLOAT_SERVICE_ID, new Notification());
        }


        if (SharedPreferenceManager.getInstance(this).isAllowFastCapture()) {
            addFloatView();
        } else {
            removeFloatView();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        boolean openWindow = intent.getBooleanExtra(EXTRA_TOGGLE, true);
        LogUtil.d(TAG, "onBind openWindow--->" + openWindow);
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
        mWindowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
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

        //获取浮动窗口视图所在布局
        mFloatLayout = (RelativeLayout) LayoutInflater.from(getApplicationContext()).inflate(R.layout.floatint_layout, null);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mFloatLayout.getParent() != null) {
                    LogUtil.d(TAG, "mFloatLayout has parent");
                    mWindowManager.removeView(mFloatLayout);
                }
                mWindowManager.addView(mFloatLayout, wmParams);
                mIsAdded = true;
            }
        });

        //浮动窗口按钮
        mHomeBtn = (ImageButton) mFloatLayout.findViewById(R.id.ib_home);

        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        //设置监听浮动窗口的触摸移动
        mHomeBtn.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        mCurrentX = event.getRawX();
                        mCurrentY = event.getRawY();
                        mDownX = event.getX();
                        mDownY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mCurrentX = event.getRawX();
                        mCurrentY = event.getRawY();

                        int newX = (int) (mCurrentX - (mDownX - mHomeBtn.getMeasuredWidth() / 2));
                        int newY = (int) (mCurrentY - (mDownY - mHomeBtn.getMeasuredHeight() / 2));
                        updatePosition(newX, newY);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:

                        break;
                }

                return false;
            }
        });

//        mHomeBtn.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                TipsUtil.showToast(getApplicationContext(), "Home Btn Click");
//            }
//        });
    }

    private float mDownX, mDownY, mCurrentX, mCurrentY;

    private void updatePosition(int newX, int newY) {
        Log.d(TAG, "updatePosition, x:" + newX + " y:" + newY);

        //getRawX是触摸位置相对于屏幕的坐标，getX是相对于按钮的坐标
        wmParams.x = newX - mHomeBtn.getMeasuredWidth() / 2;
        wmParams.y = newY - mHomeBtn.getMeasuredHeight() / 2 - getStatusBarHeight();
        //刷新
        mWindowManager.updateViewLayout(mFloatLayout, wmParams);
    }

    /**
     * 用于获取状态栏的高度。
     *
     * @return 返回状态栏高度的像素值。
     */
    private int getStatusBarHeight() {
        int height = 0;
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object o = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = (Integer) field.get(o);
            height = getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return height;
    }

    public void removeFloatView() {
        if (mIsAdded) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mIsAdded = false;
                    //移除悬浮窗口
                    LogUtil.d(TAG, "removeViewImmediate");
                    mWindowManager.removeViewImmediate(mFloatLayout);
                }
            });
        }
    }

    public void toggleFloatView(boolean toggle) {
        LogUtil.d(TAG, "toggleFloatView toggle=" + toggle);
        if (toggle) {
            addFloatView();
        } else {
            removeFloatView();
        }
    }

    @Override
    public void onDestroy() {
        LogUtil.d(TAG, "onDestroy");
        super.onDestroy();
        removeFloatView();
    }

    public class InnerBinder extends IFloatAidlInterface.Stub {
        @Override
        public void toggleFloatWindow(boolean toggle) throws RemoteException {
            FloatingWindowService.this.toggleFloatView(toggle);
        }
    }

}
