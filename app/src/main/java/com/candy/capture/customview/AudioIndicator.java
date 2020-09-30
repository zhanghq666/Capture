package com.candy.capture.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import com.candy.commonlibrary.utils.DensityUtil;

/**
 * Created by zhanghq on 2016/11/15.
 */

public class AudioIndicator extends View {

    private Paint mPaint;

    private int mWidth;
    private float mScale = 1;

    public AudioIndicator(Context context) {
        super(context);

        mWidth = DensityUtil.dip2px(context, 5);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mWidth);
    }

    public void setColor(int color) {
        mPaint.setColor(color);
    }

    public void setScale(float scale) {
        mScale = scale;
        requestLayout();
        invalidate();
    }

    public float getScale() {
        return mScale;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY);
        int height = MeasureSpec.makeMeasureSpec((int) (mWidth * mScale), MeasureSpec.EXACTLY);
        super.onMeasure(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mScale <= 1) {
            canvas.drawPoint(getWidth() / 2, getHeight() / 2, mPaint);
        } else {
            canvas.drawLine(getWidth() / 2, mWidth / 2, getWidth() / 2, getHeight() - mWidth / 2, mPaint);
        }
    }
}
