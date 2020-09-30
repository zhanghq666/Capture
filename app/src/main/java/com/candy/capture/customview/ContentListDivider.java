package com.candy.capture.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.candy.capture.R;
import com.candy.commonlibrary.utils.DensityUtil;

/**
 * Created by zhanghq on 2016/10/30.
 */

public class ContentListDivider extends RecyclerView.ItemDecoration {

    private int mItemSize = 1;
    private Paint mPaint;

    public ContentListDivider(Context context) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(context.getResources().getColor(R.color.colorGray));
        mPaint.setStyle(Paint.Style.FILL);

        mItemSize = DensityUtil.dip2px(context, 0.5f);
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();
        final int childSize = parent.getChildCount();
        for (int i = 0; i < childSize - 1; i++) {
            final View child = parent.getChildAt(i);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int top = child.getBottom() + layoutParams.bottomMargin;
            final int bottom = top + mItemSize;
            canvas.drawRect(left, top, right, bottom, mPaint);
        }
    }
}
