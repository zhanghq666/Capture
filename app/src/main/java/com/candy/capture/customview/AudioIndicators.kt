package com.candy.capture.customview

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import com.candy.capture.R
import com.candy.commonlibrary.utils.DensityUtil

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:32
 */
class AudioIndicators(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
     companion object {
        private const val INDICATOR_COUNT = 4
        private const val VERTICAL_PADDING_IN_DP = 4
        private const val HORIZONTAL_PADDING_IN_DP = 4

        private const val INDICATOR_SPACE_IN_DP = 12
        private const val MAX_LEVEL = 250
    }

    private var mVerticalPadding = 0
    private var mHorizontalPadding: Int = 0

    private lateinit var mAudioIndicator1: AudioIndicator
    private lateinit var mAudioIndicator2: AudioIndicator
    private lateinit var mAudioIndicator3: AudioIndicator
    private lateinit var mAudioIndicator4: AudioIndicator
    private var mIsAnimating = false

    init {
        mVerticalPadding = DensityUtil.dip2px(context, VERTICAL_PADDING_IN_DP.toFloat())
        mHorizontalPadding = DensityUtil.dip2px(context, HORIZONTAL_PADDING_IN_DP.toFloat())
        orientation = HORIZONTAL
        setPadding(mHorizontalPadding, mVerticalPadding, mHorizontalPadding, mVerticalPadding)
        init()
    }

    constructor(context: Context) : this(context, null)

    private fun init() {
        mAudioIndicator1 = AudioIndicator(context)
        mAudioIndicator1.setColor(resources.getColor(R.color.colorAudioPoint1))
        val layoutParams1 = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams1.gravity = Gravity.CENTER_VERTICAL
        addView(mAudioIndicator1, layoutParams1)
        mAudioIndicator2 = AudioIndicator(context)
        mAudioIndicator2.setColor(resources.getColor(R.color.colorAudioPoint2))
        val layoutParams2 = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams2.gravity = Gravity.CENTER_VERTICAL
        layoutParams2.leftMargin = DensityUtil.dip2px(context, INDICATOR_SPACE_IN_DP.toFloat())
        addView(mAudioIndicator2, layoutParams2)
        mAudioIndicator3 = AudioIndicator(context)
        mAudioIndicator3.setColor(resources.getColor(R.color.colorAudioPoint3))
        addView(mAudioIndicator3, layoutParams2)
        mAudioIndicator4 = AudioIndicator(context)
        mAudioIndicator4.setColor(resources.getColor(R.color.colorAudioPoint4))
        addView(mAudioIndicator4, layoutParams2)
    }

    private val mHandler = Handler { msg ->
        val level = msg.arg1
        var indicator: AudioIndicator? = null
        when (msg.what) {
            1 -> indicator = mAudioIndicator1
            2 -> indicator = mAudioIndicator2
            3 -> indicator = mAudioIndicator3
            4 -> indicator = mAudioIndicator4
        }
        val animator: ObjectAnimator
        if (level > 0) {
            val scale = (level * 8.0 / MAX_LEVEL).toFloat()
            animator = ObjectAnimator.ofFloat(indicator, "abc", indicator!!.getScale(), scale)
            animator.repeatMode = ValueAnimator.REVERSE
            animator.repeatCount = 1
            //                animator.setDuration(400 * MAX_LEVEL / level);
            animator.duration = 400
            animator.interpolator = DecelerateInterpolator()
        } else {
            // 停止
            animator = ObjectAnimator.ofFloat(indicator, "abc", indicator!!.getScale(), 1f)
            animator.duration = 100
            animator.interpolator = AccelerateInterpolator()
        }
        val finalIndicator = indicator
        animator.addUpdateListener { animation ->
            if (mIsAnimating) {
                finalIndicator.setScale((animation.animatedValue as Float))
            }
        }
        animator.start()
        true
    }

    fun setLevel(level: Int) {
        var level = if (level > MAX_LEVEL) MAX_LEVEL else level
        val message1 = mHandler.obtainMessage(1, level, 0)
        mHandler.sendMessage(message1)
        val message2 = mHandler.obtainMessage(2, level, 0)
        mHandler.sendMessageDelayed(message2, 200)
        val message3 = mHandler.obtainMessage(3, level, 0)
        mHandler.sendMessageDelayed(message3, 400)
        val message4 = mHandler.obtainMessage(4, level, 0)
        mHandler.sendMessageDelayed(message4, 600)
    }

    fun turnOn() {
        mIsAnimating = true
    }

    fun stop() {
        mIsAnimating = false
        mHandler.removeCallbacksAndMessages(null)
        mAudioIndicator1.setScale(1f)
        mAudioIndicator2.setScale(1f)
        mAudioIndicator3.setScale(1f)
        mAudioIndicator4.setScale(1f)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHandler.removeCallbacksAndMessages(null)
    }
}