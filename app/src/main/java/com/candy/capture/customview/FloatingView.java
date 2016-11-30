package com.candy.capture.customview;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.candy.capture.R;
import com.candy.capture.service.FloatingWindowService;
import com.candy.capture.util.FloatWindowManager;
import com.candy.capture.util.LogUtil;
import com.candy.capture.util.TipsUtil;

import java.lang.reflect.Field;

/**
 * Created by zhanghq on 2016/11/28.
 */

public class FloatingView extends FrameLayout {
    private static final String TAG = "FloatingView";

    /**
     * 记录小悬浮窗的宽度
     */
    public static int viewWidth;

    /**
     * 记录小悬浮窗的高度
     */
    public static int viewHeight;

    /**
     * 记录系统状态栏的高度
     */
    private static int statusBarHeight;

    /**
     * 用于更新小悬浮窗的位置
     */
    private WindowManager windowManager;

    /**
     * 小悬浮窗的参数
     */
    private WindowManager.LayoutParams mParams;

    /**
     * 记录当前手指位置在屏幕上的横坐标值
     */
    private float xInScreen;

    /**
     * 记录当前手指位置在屏幕上的纵坐标值
     */
    private float yInScreen;

    /**
     * 记录手指按下时在屏幕上的横坐标的值
     */
    private float xDownInScreen;

    /**
     * 记录手指按下时在屏幕上的纵坐标的值
     */
    private float yDownInScreen;

    /**
     * 记录手指按下时在小悬浮窗的View上的横坐标的值
     */
    private float xInView;

    /**
     * 记录手指按下时在小悬浮窗的View上的纵坐标的值
     */
    private float yInView;


    private RelativeLayout mFloatLayout;
    private ImageButton mHomeBtn;

    public FloatingView(Context context) {
        super(context);

        //获取浮动窗口视图所在布局
        mFloatLayout = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.floatint_layout, this);
        //浮动窗口按钮
        mHomeBtn = (ImageButton) mFloatLayout.findViewById(R.id.ib_home);

        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        LogUtil.i(TAG, "Width/2--->" + mHomeBtn.getMeasuredWidth() / 2);
        LogUtil.i(TAG, "Height/2--->" + mHomeBtn.getMeasuredHeight() / 2);
//        //设置监听浮动窗口的触摸移动
//        mHomeBtn.setOnTouchListener(new View.OnTouchListener() {
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                //getRawX是触摸位置相对于屏幕的坐标，getX是相对于按钮的坐标
//                wmParams.x = (int) event.getRawX() - mHomeBtn.getMeasuredWidth() / 2;
//                LogUtil.i(TAG, "RawX" + event.getRawX());
//                LogUtil.i(TAG, "X" + event.getX());
//                //减25为状态栏的高度
//                wmParams.y = (int) event.getRawY() - mHomeBtn.getMeasuredHeight() / 2 - 25;
//                LogUtil.i(TAG, "RawY" + event.getRawY());
//                LogUtil.i(TAG, "Y" + event.getY());
//                //刷新
//                mWindowManager.updateViewLayout(mFloatLayout, wmParams);
//                return false;
//            }
//        });

//        mHomeBtn.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                TipsUtil.showToast(FloatingView.this, "Home Btn Click");
//            }
//        });
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 手指按下时记录必要数据,纵坐标的值都需要减去状态栏高度
                xInView = event.getX();
                yInView = event.getY();
                xDownInScreen = event.getRawX();
                yDownInScreen = event.getRawY() - getStatusBarHeight();
                xInScreen = event.getRawX();
                yInScreen = event.getRawY() - getStatusBarHeight();
                break;
            case MotionEvent.ACTION_MOVE:
                xInScreen = event.getRawX();
                yInScreen = event.getRawY() - getStatusBarHeight();
                // 手指移动的时候更新小悬浮窗的位置
                updateViewPosition();
                break;
            case MotionEvent.ACTION_UP:
                // 如果手指离开屏幕时，xDownInScreen和xInScreen相等，且yDownInScreen和yInScreen相等，则视为触发了单击事件。
                if (xDownInScreen == xInScreen && yDownInScreen == yInScreen) {
                    TipsUtil.showToast(getContext(), "Home click");
//                    openBigWindow();
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 将小悬浮窗的参数传入，用于更新小悬浮窗的位置。
     *
     * @param params
     *            小悬浮窗的参数
     */
    public void setParams(WindowManager.LayoutParams params) {
        mParams = params;
    }

    /**
     * 更新小悬浮窗在屏幕中的位置。
     */
    private void updateViewPosition() {
        mParams.x = (int) (xInScreen - xInView);
        mParams.y = (int) (yInScreen - yInView);
        windowManager.updateViewLayout(this, mParams);
    }

    /**
     * 打开大悬浮窗，同时关闭小悬浮窗。
     */
    private void openBigWindow() {
//        FloatWindowManager.createBigWindow(getContext());
        FloatWindowManager.removeSmallWindow(getContext());
    }

    /**
     * 用于获取状态栏的高度。
     *
     * @return 返回状态栏高度的像素值。
     */
    private int getStatusBarHeight() {
        if (statusBarHeight == 0) {
            try {
                Class<?> c = Class.forName("com.android.internal.R$dimen");
                Object o = c.newInstance();
                Field field = c.getField("status_bar_height");
                int x = (Integer) field.get(o);
                statusBarHeight = getResources().getDimensionPixelSize(x);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return statusBarHeight;
    }
}
