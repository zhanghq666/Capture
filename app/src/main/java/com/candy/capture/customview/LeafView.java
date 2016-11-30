//package com.candy.capture.customview;
//
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Paint;
//import android.graphics.Path;
//import android.graphics.Point;
//import android.util.AttributeSet;
//import android.view.View;
//
//import com.candy.capture.util.DensityUtil;
//
//import java.util.ArrayList;
//import java.util.Random;
//
///**
// * Created by zhanghq on 2016/11/27.
// */
//
//public class LeafView extends View {
//    public LeafView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//    }
//
//    private LeafInfo[] mLeaf;
//    private Paint mPaint;
//    private int outOfViewBaseDistance;
//
//    private void init() {
//        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
//        outOfViewBaseDistance = DensityUtil.dip2px(getContext(), 40);
//
//        int leafCount = 1 + new Random().nextInt(3);
//        mLeaf = new LeafInfo[leafCount];
//        for (int i = 0; i < leafCount; i ++) {
//            mLeaf[i] = new LeafInfo();
//
//        }
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        for (LeafInfo leaf : mLeaf) {
//            leaf.path.cubicTo();
//        }
//    }
//
//    class LeafInfo {
//        private int leafType;
//        private Point position;
//        private boolean isOutOfView;
//        private Path path;
//    }
//}
