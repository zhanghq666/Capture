package com.candy.capture.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import com.candy.commonlibrary.utils.DensityUtil

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:39
 */
class AudioView(context: Context?, attrs: AttributeSet?): View(context, attrs) {
    companion object {
        private const val ARC_LEFT_MARGIN_IN_DP = 10

        private const val ARC_ANGLE = 90

        private const val BORDER_STROKE_WIDTH_IN_DP = 2

        private const val STATE_STOP = 1
        private const val STATE_PLAYING = 2

        private const val DRAW_STATE_SMALL = 0
        private const val DRAW_STATE_MIDDLE = 1
        private const val DRAW_STATE_BIG = 2
    }

    private var mPaint: Paint? = null

    private var mState = 0
    private var mDrawState = 0
    private var mDuration = 0

    private var mCurrentProgress = 0

    private var mMinAdd = 0.0
    private var mProgressRectWidth = 0.0
    private var mProgressPercent = 0.0

    private lateinit var mHandler: Handler
    private var strokeWidth = 0

    constructor(context: Context?): this(context, null)

    init {
        init()
    }

    private fun init() {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        mPaint!!.style = Paint.Style.STROKE

        mState = STATE_STOP
        mDrawState = DRAW_STATE_BIG
        strokeWidth = DensityUtil.dip2px(context, BORDER_STROKE_WIDTH_IN_DP.toFloat())

        mHandler = Handler {
            if (STATE_PLAYING == mState) {
                mCurrentProgress += 1
                invalidate()
                // 0.1秒重绘一次以实现进度流畅变化
                mHandler.sendEmptyMessageDelayed(0, 100)
            }
            true
        }
    }

    fun setDuration(duration: Int) {
        mDuration = duration * 10
    }

    fun startAnimation() {
        mCurrentProgress = 0
        mProgressRectWidth = 0.0
        mMinAdd = (measuredWidth - strokeWidth) / mDuration.toDouble()
        mState = STATE_PLAYING
        mDrawState = DRAW_STATE_SMALL
        mHandler.sendEmptyMessage(0)
    }

    fun startAnimation(progressInPercent: Double) {
        // 注意这里getMeasuredWidth拿到的可能是0，所以在onMeasure里还会设置mProgressRectWidth和mMinAdd
        mCurrentProgress = 0
        mProgressRectWidth = progressInPercent * measuredWidth
        mProgressPercent = progressInPercent
        mMinAdd = (measuredWidth - strokeWidth) / mDuration.toDouble()
        mState = STATE_PLAYING
        mDrawState = DRAW_STATE_SMALL
        mHandler.sendEmptyMessage(0)
    }

    fun stopAnimation() {
        mState = STATE_STOP
        mDrawState = DRAW_STATE_BIG
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measuredWidth > 0) {
            mProgressRectWidth = mProgressPercent * measuredWidth
            mMinAdd = (measuredWidth - strokeWidth) / mDuration.toDouble()
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawRect(canvas)
        if (STATE_PLAYING == mState) {
            drawProgress(canvas)
        }
        drawArc(canvas)
    }

    private fun drawRect(canvas: Canvas) {
        mPaint!!.color = -0x9a9a9b
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.strokeWidth = strokeWidth.toFloat()
        canvas.drawRoundRect(RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat()),
                measuredHeight / 6f, measuredHeight / 6f,
                mPaint)
    }

    private fun drawArc(canvas: Canvas) {
        mPaint!!.color = -0x7d7d7e
        mPaint!!.strokeWidth = strokeWidth.toFloat()
        // 三个环每个环角度相同，小环Rect宽高为控件的四分之一高，往外每环比内环宽八分之一控件高度。至于为什么，把每个Arc的rectF画出来就知道了。
        if (mDrawState >= DRAW_STATE_SMALL) {
            drawSmallArc(canvas)
        }
        if (mDrawState >= DRAW_STATE_MIDDLE) {
            drawMiddleArc(canvas)
        }
        if (mDrawState >= DRAW_STATE_BIG) {
            drawBigArc(canvas)
        }
        if (STATE_PLAYING == mState) {
            if (mCurrentProgress % 5 == 0) {
                mDrawState = ++mDrawState % 3
            }
        }
    }

    private fun drawSmallArc(canvas: Canvas) {
        mPaint!!.color = -0x7d7d7e
        mPaint!!.style = Paint.Style.FILL
        val smallArcSize = measuredHeight / 4
        val rectF = RectF(DensityUtil.dip2px(context, ARC_LEFT_MARGIN_IN_DP.toFloat()).toFloat(), ((measuredHeight - smallArcSize) / 2).toFloat(),
                (DensityUtil.dip2px(context, ARC_LEFT_MARGIN_IN_DP.toFloat()) + smallArcSize).toFloat(), ((measuredHeight + smallArcSize) / 2).toFloat())
        canvas.drawArc(rectF, -ARC_ANGLE / 2f, ARC_ANGLE.toFloat(), true, mPaint)
    }

    private fun drawMiddleArc(canvas: Canvas) {
        mPaint!!.color = -0x7d7d7e
        mPaint!!.style = Paint.Style.STROKE
        val middleArcSize = measuredHeight / 2
        val rectF = RectF(DensityUtil.dip2px(context, ARC_LEFT_MARGIN_IN_DP.toFloat()).toFloat(), ((measuredHeight - middleArcSize) / 2).toFloat(),
                (DensityUtil.dip2px(context, ARC_LEFT_MARGIN_IN_DP.toFloat()) + measuredHeight * 3 / 8).toFloat(), ((measuredHeight + middleArcSize) / 2).toFloat())
        canvas.drawArc(rectF, -ARC_ANGLE / 2f, ARC_ANGLE.toFloat(), false, mPaint)
    }

    private fun drawBigArc(canvas: Canvas) {
        mPaint!!.color = -0x7d7d7e
        mPaint!!.style = Paint.Style.STROKE
        val bigArcSize = measuredHeight * 3 / 4
        val rectF = RectF(DensityUtil.dip2px(context, ARC_LEFT_MARGIN_IN_DP.toFloat()).toFloat(), ((measuredHeight - bigArcSize) / 2).toFloat(),
                (DensityUtil.dip2px(context, ARC_LEFT_MARGIN_IN_DP.toFloat()) + measuredHeight / 2).toFloat(), ((measuredHeight + bigArcSize) / 2).toFloat())
        canvas.drawArc(rectF, -ARC_ANGLE / 2f, ARC_ANGLE.toFloat(), false, mPaint)
    }

    private fun drawProgress(canvas: Canvas) {
        val lineCenter = strokeWidth / 2f
        mProgressRectWidth += mMinAdd
        mProgressRectWidth = if (mProgressRectWidth > measuredWidth - lineCenter) (measuredWidth - lineCenter).toDouble() else mProgressRectWidth
        mPaint!!.color = -0x222223
        mPaint!!.style = Paint.Style.FILL
        canvas.save()
        val path = Path()
        path.addRoundRect(RectF(lineCenter, lineCenter, measuredWidth - lineCenter, measuredHeight - lineCenter),
                measuredHeight / 6f - lineCenter, measuredHeight / 6f - lineCenter, Path.Direction.CW)
        canvas.clipPath(path)
        canvas.drawRect(RectF(0f, 0f, mProgressRectWidth.toFloat(), measuredHeight.toFloat()), mPaint)
        canvas.restore()
    }
}