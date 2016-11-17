package com.candy.capture.customview;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import com.candy.capture.R;
import com.candy.capture.util.DensityUtil;

import java.util.ArrayList;

/**
 * Created by zhanghq on 2016/11/15.
 */

public class AudioIndicators extends LinearLayout {

    private static final int INDICATOR_COUNT = 4;
    private static final int VERTICAL_PADDING_IN_DP = 4;
    private static final int HORIZONTAL_PADDING_IN_DP = 4;

    private static final int INDICATOR_SPACE_IN_DP = 12;
    private static final int MAX_LEVEL = 250;

    private int mVerticalPadding, mHorizontalPadding;

    private AudioIndicator mAudioIndicator1, mAudioIndicator2, mAudioIndicator3, mAudioIndicator4;
    private boolean mIsAnimating;

    public AudioIndicators(Context context) {
        super(context);
    }

    public AudioIndicators(Context context, AttributeSet attrs) {
        super(context, attrs);

        mVerticalPadding = DensityUtil.dip2px(context, VERTICAL_PADDING_IN_DP);
        mHorizontalPadding = DensityUtil.dip2px(context, HORIZONTAL_PADDING_IN_DP);
        setOrientation(HORIZONTAL);
        setPadding(mHorizontalPadding, mVerticalPadding, mHorizontalPadding, mVerticalPadding);

        init();
    }

    private void init() {
//        int width = DensityUtil.dip2px(getContext(), 8);
        mAudioIndicator1 = new AudioIndicator(getContext());
        mAudioIndicator1.setColor(getResources().getColor(R.color.colorAudioPoint1));
        LayoutParams layoutParams1 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams1.gravity = Gravity.CENTER_VERTICAL;
        addView(mAudioIndicator1, layoutParams1);

        mAudioIndicator2 = new AudioIndicator(getContext());
        mAudioIndicator2.setColor(getResources().getColor(R.color.colorAudioPoint2));
        LayoutParams layoutParams2 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams2.gravity = Gravity.CENTER_VERTICAL;
        layoutParams2.leftMargin = DensityUtil.dip2px(getContext(), INDICATOR_SPACE_IN_DP);
        addView(mAudioIndicator2, layoutParams2);

        mAudioIndicator3 = new AudioIndicator(getContext());
        mAudioIndicator3.setColor(getResources().getColor(R.color.colorAudioPoint3));
        addView(mAudioIndicator3, layoutParams2);

        mAudioIndicator4 = new AudioIndicator(getContext());
        mAudioIndicator4.setColor(getResources().getColor(R.color.colorAudioPoint4));
        addView(mAudioIndicator4, layoutParams2);

    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int level = msg.arg1;
            AudioIndicator indicator = null;
            switch (msg.what) {
                case 1:
                    indicator = mAudioIndicator1;
                    break;
                case 2:
                    indicator = mAudioIndicator2;
                    break;
                case 3:
                    indicator = mAudioIndicator3;
                    break;
                case 4:
                    indicator = mAudioIndicator4;
                    break;
            }
            final ObjectAnimator animator;
            if (level > 0) {
                float scale = (float) (level * 8.0 / MAX_LEVEL) ;
                animator = ObjectAnimator.ofFloat(indicator, "abc", indicator.getScale(), scale);
                animator.setRepeatMode(ValueAnimator.REVERSE);
                animator.setRepeatCount(1);
//                animator.setDuration(400 * MAX_LEVEL / level);
                animator.setDuration(400);
                animator.setInterpolator(new DecelerateInterpolator());
            } else {
                // 停止
                animator = ObjectAnimator.ofFloat(indicator, "abc", indicator.getScale(), 1);
                animator.setDuration(100);
                animator.setInterpolator(new AccelerateInterpolator());
            }

            final AudioIndicator finalIndicator = indicator;
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (mIsAnimating) {
                        finalIndicator.setScale((Float) animation.getAnimatedValue());
                    }
                }
            });

            animator.start();

            return true;
        }
    });

    public void setLevel(int level) {
        level = level > MAX_LEVEL ? MAX_LEVEL : level;

        Message message1 = mHandler.obtainMessage(1, level, 0);
        mHandler.sendMessage(message1);

        Message message2 = mHandler.obtainMessage(2, level, 0);
        mHandler.sendMessageDelayed(message2, 200);

        Message message3 = mHandler.obtainMessage(3, level, 0);
        mHandler.sendMessageDelayed(message3, 400);

        Message message4 = mHandler.obtainMessage(4, level, 0);
        mHandler.sendMessageDelayed(message4, 600);
    }

    public void turnOn() {
        mIsAnimating = true;
    }

    public void stop() {
        mIsAnimating = false;
        mHandler.removeCallbacksAndMessages(null);

        mAudioIndicator1.setScale(1);
        mAudioIndicator2.setScale(1);
        mAudioIndicator3.setScale(1);
        mAudioIndicator4.setScale(1);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacksAndMessages(null);
    }
}
