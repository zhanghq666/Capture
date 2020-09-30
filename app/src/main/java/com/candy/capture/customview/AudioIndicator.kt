package com.candy.capture.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.candy.commonlibrary.utils.DensityUtil

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:29
 */
class AudioIndicator(context: Context): View(context) {
    private var mPaint: Paint

    private var mWidth = 0
    private var mScale = 1f

    init {
        mWidth = DensityUtil.dip2px(context, 5f)
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeCap = Paint.Cap.ROUND
        mPaint.strokeWidth = mWidth.toFloat()
    }

    fun setColor(color: Int) {
        mPaint.color = color
    }

    fun setScale(scale: Float) {
        mScale = scale
        requestLayout()
        invalidate()
    }

    fun getScale(): Float {
        return mScale
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY)
        val height = MeasureSpec.makeMeasureSpec((mWidth * mScale).toInt(), MeasureSpec.EXACTLY)
        super.onMeasure(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (mScale <= 1) {
            canvas.drawPoint(width / 2.toFloat(), height / 2.toFloat(), mPaint)
        } else {
            canvas.drawLine(width / 2.toFloat(), mWidth / 2.toFloat(), width / 2.toFloat(), height - mWidth / 2.toFloat(), mPaint)
        }
    }
}