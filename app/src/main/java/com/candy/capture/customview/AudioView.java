package com.candy.capture.customview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.candy.capture.R;
import com.candy.capture.util.DensityUtil;

/**
 * Created by zhanghq on 2016/11/1.
 */

public final class AudioView extends View {

    private static final int ARC_LEFT_MARGIN_IN_DP = 10;

    private static final int ARC_ANGLE = 90;

    private static final int BORDER_STROKE_WIDTH_IN_DP = 2;

    private static final int STATE_STOP = 1;
    private static final int STATE_PLAYING = 2;

    private static final int DRAW_STATE_SMALL = 0;
    private static final int DRAW_STATE_MIDDLE = 1;
    private static final int DRAW_STATE_BIG = 2;

    private Paint mPaint;
    //    private GestureDetector mDetector;
    private int mState;
    private int mDrawState;
    private int mDuration;

    private int mCurrentProgress;

    private double mMinAdd;
    private double mProgressRectWidth;
    private double mProgressPercent;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (STATE_PLAYING == mState) {
                mCurrentProgress += 1;
                invalidate();
                // 0.1秒重绘一次以实现进度流畅变化
                mHandler.sendEmptyMessageDelayed(0, 100);
            }
            return true;
        }
    });
    private int strokeWidth;

    public AudioView(Context context) {
        super(context);
    }

    public AudioView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);

//        mDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
//            @Override
//            public boolean onSingleTapUp(MotionEvent e) {
//                if (STATE_STOP == mState) {
//                    mState = STATE_PLAYING;
//                    mDrawState = DRAW_STATE_SMALL;
//                    mHandler.sendEmptyMessage(0);
//                } else {
//                    stopAnimation();
//                }
//                return true;
//            }
//        });
        mState = STATE_STOP;
        mDrawState = DRAW_STATE_BIG;
        strokeWidth = DensityUtil.dip2px(getContext(), BORDER_STROKE_WIDTH_IN_DP);
    }

    public void setDuration(int duration) {
        mDuration = duration * 10;
    }

    public void startAnimation() {
        mCurrentProgress = 0;
        mProgressRectWidth = 0;
        mMinAdd = (getMeasuredWidth() - strokeWidth) / (double) mDuration;
        mState = STATE_PLAYING;
        mDrawState = DRAW_STATE_SMALL;
        mHandler.sendEmptyMessage(0);
    }

    public void startAnimation(double progressInPercent) {
        // 注意这里getMeasuredWidth拿到的可能是0，所以在onMeasure里还会设置mProgressRectWidth和mMinAdd
        mCurrentProgress = 0;
        mProgressRectWidth = progressInPercent * getMeasuredWidth();
        mProgressPercent = progressInPercent;
        mMinAdd = (getMeasuredWidth() - strokeWidth) / (double) mDuration;
        mState = STATE_PLAYING;
        mDrawState = DRAW_STATE_SMALL;
        mHandler.sendEmptyMessage(0);
    }

    public void stopAnimation() {
        mState = STATE_STOP;
        mDrawState = DRAW_STATE_BIG;
        invalidate();
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        mDetector.onTouchEvent(event);
//        return super.onTouchEvent(event);
//    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getMeasuredWidth() > 0) {
            mProgressRectWidth = mProgressPercent * getMeasuredWidth();
            mMinAdd = (getMeasuredWidth() - strokeWidth) / (double) mDuration;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawRect(canvas);
        if (STATE_PLAYING == mState) {
            drawProgress(canvas);
        }
        drawArc(canvas);
    }

    private void drawRect(Canvas canvas) {
        mPaint.setColor(0xff656565);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(strokeWidth);
        canvas.drawRoundRect(new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight()),
                getMeasuredHeight() / 6f, getMeasuredHeight() / 6f,
                mPaint);
    }

    private void drawArc(Canvas canvas) {
        mPaint.setColor(0xff828282);
        mPaint.setStrokeWidth(strokeWidth);
        // 三个环每个环角度相同，小环Rect宽高为控件的四分之一高，往外每环比内环宽八分之一控件高度。至于为什么，把每个Arc的rectF画出来就知道了。
        if (mDrawState >= DRAW_STATE_SMALL) {
            drawSmallArc(canvas);
        }
        if (mDrawState >= DRAW_STATE_MIDDLE) {
            drawMiddleArc(canvas);
        }
        if (mDrawState >= DRAW_STATE_BIG) {
            drawBigArc(canvas);
        }

        if (STATE_PLAYING == mState) {
            if (mCurrentProgress % 5 == 0) {
                mDrawState = (++mDrawState) % 3;
            }
        }
    }

    private void drawSmallArc(Canvas canvas) {
        mPaint.setColor(0xff828282);
        mPaint.setStyle(Paint.Style.FILL);
        int smallArcSize = getMeasuredHeight() / 4;
        RectF rectF = new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - smallArcSize) / 2,
                DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + smallArcSize, (getMeasuredHeight() + smallArcSize) / 2);
        canvas.drawArc(rectF, -ARC_ANGLE / 2f, ARC_ANGLE, true, mPaint);
    }

    private void drawMiddleArc(Canvas canvas) {
        mPaint.setColor(0xff828282);
        mPaint.setStyle(Paint.Style.STROKE);
        int middleArcSize = getMeasuredHeight() / 2;
        RectF rectF = new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - middleArcSize) / 2,
                DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + getMeasuredHeight() * 3 / 8, (getMeasuredHeight() + middleArcSize) / 2);
        canvas.drawArc(rectF, -ARC_ANGLE / 2f, ARC_ANGLE, false, mPaint);
    }

    private void drawBigArc(Canvas canvas) {
        mPaint.setColor(0xff828282);
        mPaint.setStyle(Paint.Style.STROKE);
        int bigArcSize = getMeasuredHeight() * 3 / 4;
        RectF rectF = new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - bigArcSize) / 2,
                DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + getMeasuredHeight() / 2, (getMeasuredHeight() + bigArcSize) / 2);
        canvas.drawArc(rectF, -ARC_ANGLE / 2f, ARC_ANGLE, false, mPaint);
    }

    private void drawProgress(Canvas canvas) {

        float lineCenter = strokeWidth / 2f;
        mProgressRectWidth += mMinAdd;
        mProgressRectWidth = mProgressRectWidth > getMeasuredWidth() - lineCenter ? getMeasuredWidth() - lineCenter : mProgressRectWidth;
        mPaint.setColor(0xffdddddd);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.save();
        Path path = new Path();
        path.addRoundRect(new RectF(lineCenter, lineCenter, getMeasuredWidth() - lineCenter, getMeasuredHeight() - lineCenter),
                getMeasuredHeight() / 6f - lineCenter, getMeasuredHeight() / 6f - lineCenter, Path.Direction.CW);
        canvas.clipPath(path);
        canvas.drawRect(new RectF(0, 0, (float) mProgressRectWidth, getMeasuredHeight()), mPaint);
        canvas.restore();
    }
}
