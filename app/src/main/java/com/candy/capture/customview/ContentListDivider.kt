package com.candy.capture.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.candy.capture.R
import com.candy.commonlibrary.utils.DensityUtil

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:46
 */
class ContentListDivider(context: Context): ItemDecoration() {
    private var mItemSize = 1
    private var mPaint: Paint? = null

    init {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mPaint!!.color = context.resources.getColor(R.color.colorGray)
        mPaint!!.style = Paint.Style.FILL
        mItemSize = DensityUtil.dip2px(context, 0.5f)
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childSize = parent.childCount
        for (i in 0 until childSize - 1) {
            val child = parent.getChildAt(i)
            val layoutParams = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + layoutParams.bottomMargin
            val bottom = top + mItemSize
            canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), mPaint)
        }
    }
}