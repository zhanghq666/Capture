package com.candy.commonlibrary.utils

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory


/**
 * @Description
 * * Glide图片加载工具类
 * 1、调用者须确保在主线程调用load***相关方法
 * 2、应用于CircleImageView等圆形ImageView时须使用禁用动画效果
 * <p>
 * @Author zhanghaiqiang
 * @Date 2020/9/30 15:45
 */
class GlideImageLoader private constructor(){
    companion object {
        private var globalPlaceholderResId = 0
        private var globalFailedResId = 0

        private var instance: GlideImageLoader? = null

        fun getInstance(): GlideImageLoader {
            if (instance == null) {
                synchronized(GlideImageLoader::class.java) { if (instance == null) instance = GlideImageLoader() }
            }
            return instance!!
        }
    }

    fun setGlobalPlaceholderResId(globalPlaceholderResId: Int) {
        Companion.globalPlaceholderResId = globalPlaceholderResId
    }

    fun setGlobalFailedResId(globalFailedResId: Int) {
        Companion.globalFailedResId = globalFailedResId
    }

    fun loadImage(context: Context, imgPath: String, imageView: ImageView) {
        val appContext = context.applicationContext
        loadImage(appContext, imgPath, imageView, globalPlaceholderResId, globalFailedResId)
    }

    fun loadVideoThumbnail(context: Context, videoPath: String, imageView: ImageView) {
        val appContext = context.applicationContext
        val options = RequestOptions()
                .frame(1)
                .centerCrop()
        Glide.with(appContext)
                .asBitmap()
                .load(videoPath)
                .apply(options)
                .into(imageView)
    }

    fun loadRoundImage(context: Context, imgPath: String?, imageView: ImageView, defaultId: Int) {
        val appContext = context.applicationContext
        loadRoundImage(appContext, imgPath, imageView, defaultId, defaultId)
    }

    fun loadImage(context: Context, imgPath: String?, imageView: ImageView, placeholderResId: Int, failedResId: Int) {
        val appContext = context.applicationContext
        val options = RequestOptions()
                .placeholder(placeholderResId)
                .error(failedResId)
                .centerCrop()
        val drawableCrossFadeFactory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        Glide.with(appContext)
                .load(imgPath)
                .apply(options)
                .transition(DrawableTransitionOptions.with(drawableCrossFadeFactory))
                .into(imageView)
    }

    fun loadRoundImage(context: Context, imgPath: String?, imageView: ImageView, placeholderResId: Int, failedResId: Int) {
        val appContext = context.applicationContext
        val options = RequestOptions()
                .placeholder(placeholderResId)
                .error(failedResId)
                .centerCrop()
        Glide.with(appContext).asBitmap().load(imgPath)
                .apply(options)
                .into(object : BitmapImageViewTarget(imageView) {
                    override fun setResource(resource: Bitmap?) {
                        val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.resources, resource)
                        circularBitmapDrawable.isCircular = true
                        imageView.setImageDrawable(circularBitmapDrawable)
                    }
                })
    }

    fun loadImageWithNoAnimate(context: Context, imgPath: String?, imageView: ImageView, placeholderResId: Int, failedResId: Int) {
        val appContext = context.applicationContext
        val drawableCrossFadeFactory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        val options = RequestOptions()
                .placeholder(placeholderResId)
                .error(failedResId)
                .centerCrop()
                .dontAnimate()
        Glide.with(appContext)
                .load(imgPath)
                .apply(options)
                .transition(DrawableTransitionOptions.with(drawableCrossFadeFactory))
                .into(imageView)
    }
}