package com.candy.capture.customview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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
//                int bigArcSize = getMeasuredHeight() * 3 / 4;
//                invalidate(new Rect(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - bigArcSize) / 2,
//                        DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + bigArcSize, (getMeasuredHeight() + bigArcSize) / 2));
                mHandler.sendEmptyMessageDelayed(0, 100);
            }
            return true;
        }
    });

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
    }

    public void setDuration(int duration) {
        mDuration = duration * 10;
    }

    public void startAnimation() {
        mCurrentProgress = 0;
        mProgressRectWidth = 0;
        int strokeWidth = DensityUtil.dip2px(getContext(), 2);
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
        int strokeWidth = DensityUtil.dip2px(getContext(), 2);
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
            int strokeWidth = DensityUtil.dip2px(getContext(), 2);
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
        mPaint.setStrokeWidth(DensityUtil.dip2px(getContext(), 2));
        canvas.drawRoundRect(new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight()),
                getMeasuredHeight() / 6, getMeasuredHeight() / 6,
                mPaint);
    }

    private void drawArc(Canvas canvas) {
        Bitmap bitmap = null;
        int left = DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP);
        int height = getMeasuredHeight() * 3 / 4;
        int width = (int) (height * 0.66);
        RectF rectF = new RectF(left, getMeasuredHeight() / 8, getMeasuredHeight() / 8 + width, height + getMeasuredHeight() / 8);
        if (STATE_PLAYING == mState) {
            switch (mDrawState) {
                case DRAW_STATE_SMALL:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_audio1);
                    break;
                case DRAW_STATE_MIDDLE:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_audio2);
                    break;
                case DRAW_STATE_BIG:
                    bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_audio3);
                    break;
            }
            if (mCurrentProgress % 5 == 0) {
                mDrawState = (++mDrawState) % 3;
            }
        } else {
            bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_audio3);
        }
        canvas.drawBitmap(bitmap, null, rectF, mPaint);

//        mPaint.setColor(0xff828282);
//        mPaint.setStrokeWidth(DensityUtil.dip2px(getContext(), 6));
//        int smallArcSize = getMeasuredHeight() / 4;
//        int middleArcSize = getMeasuredHeight() / 2;
//        int bigArcSize = getMeasuredHeight() * 3 / 4;
//        if (STATE_PLAYING == mState) {
//            switch (mDrawState) {
//                case DRAW_STATE_SMALL:
//                    canvas.drawArc(new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - smallArcSize) / 2,
//                                    DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + smallArcSize, (getMeasuredHeight() + smallArcSize) / 2),
//                            -60, 120, false, mPaint);
//                    break;
//                case DRAW_STATE_MIDDLE:
//                    canvas.drawArc(new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - smallArcSize) / 2,
//                                    DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + smallArcSize, (getMeasuredHeight() + smallArcSize) / 2),
//                            -60, 120, false, mPaint);
//                    canvas.drawArc(new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - middleArcSize) / 2,
//                                    DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + middleArcSize, (getMeasuredHeight() + middleArcSize) / 2),
//                            -60, 120, false, mPaint);
//                    break;
//                case DRAW_STATE_BIG:
//                    canvas.drawArc(new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - smallArcSize) / 2,
//                                    DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + smallArcSize, (getMeasuredHeight() + smallArcSize) / 2),
//                            -60, 120, false, mPaint);
//                    canvas.drawArc(new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - middleArcSize) / 2,
//                                    DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + middleArcSize, (getMeasuredHeight() + middleArcSize) / 2),
//                            -60, 120, false, mPaint);
//                    canvas.drawArc(new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - bigArcSize) / 2,
//                                    DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + bigArcSize, (getMeasuredHeight() + bigArcSize) / 2),
//                            -60, 120, false, mPaint);
//                    break;
//            }
//            mDrawState = (++mDrawState) % 3;
//        } else {
//            canvas.drawArc(new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - smallArcSize) / 2,
//                            DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + smallArcSize, (getMeasuredHeight() + smallArcSize) / 2),
//                    -60, 120, false, mPaint);
//            canvas.drawArc(new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - middleArcSize) / 2,
//                            DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + middleArcSize, (getMeasuredHeight() + middleArcSize) / 2),
//                    -60, 120, false, mPaint);
//            canvas.drawArc(new RectF(DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP), (getMeasuredHeight() - bigArcSize) / 2,
//                            DensityUtil.dip2px(getContext(), ARC_LEFT_MARGIN_IN_DP) + bigArcSize, (getMeasuredHeight() + bigArcSize) / 2),
//                    -60, 120, false, mPaint);
//        }
    }

    private void drawProgress(Canvas canvas) {

        int strokeWidth = DensityUtil.dip2px(getContext(), 2);
        mProgressRectWidth += mMinAdd;
        mProgressRectWidth = mProgressRectWidth > getMeasuredWidth() - strokeWidth ? getMeasuredWidth() - strokeWidth : mProgressRectWidth;
        mPaint.setColor(0xffebebeb);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.save();
        Path path = new Path();
        path.addRoundRect(new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight()),
                getMeasuredHeight() / 6, getMeasuredHeight() / 6, Path.Direction.CW);
        canvas.clipPath(path);
        canvas.drawRect(new RectF(strokeWidth / 2f, strokeWidth / 2f, (float) mProgressRectWidth - strokeWidth, getMeasuredHeight() - strokeWidth), mPaint);
        canvas.restore();
    }
}
