package com.candy.capture.util

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.FileDescriptorBitmapDecoder
import com.bumptech.glide.load.resource.bitmap.VideoBitmapDecoder
import com.bumptech.glide.request.target.BitmapImageViewTarget

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
        GlideImageLoader.globalPlaceholderResId = globalPlaceholderResId
    }

    fun setGlobalFailedResId(globalFailedResId: Int) {
        GlideImageLoader.globalFailedResId = globalFailedResId
    }

    fun loadImage(context: Context?, imgPath: String?, imageView: ImageView?) {
        if (context == null) {
            return
        }
        val appContext = context.applicationContext
        loadImage(appContext, imgPath, imageView, globalPlaceholderResId, globalFailedResId)
    }

    fun loadVideoThumbnail(context: Context?, videoPath: String?, imageView: ImageView?) {
        if (context == null) {
            return
        }
        val appContext = context.applicationContext
        val bitmapPool = Glide.get(appContext).bitmapPool
        val microSecond = 1 // 6th second as an example
        val videoBitmapDecoder = VideoBitmapDecoder(microSecond)
        val fileDescriptorBitmapDecoder = FileDescriptorBitmapDecoder(videoBitmapDecoder, bitmapPool, DecodeFormat.PREFER_ARGB_8888)
        Glide.with(appContext)
                .load(videoPath)
                .asBitmap() //                        .override(50,50)// Example
                .videoDecoder(fileDescriptorBitmapDecoder)
                .into(imageView)
    }

    fun loadRoundImage(context: Context?, imgPath: String?, imageView: ImageView, defaultId: Int) {
        if (context == null) {
            return
        }
        val appContext = context.applicationContext
        loadRoundImage(appContext, imgPath, imageView, defaultId, defaultId)
    }

    fun loadImage(context: Context?, imgPath: String?, imageView: ImageView?, placeholderResId: Int, failedResId: Int) {
        if (context == null) {
            return
        }
        val appContext = context.applicationContext
        Glide.with(appContext)
                .load(imgPath)
                .placeholder(placeholderResId)
                .error(failedResId)
                .crossFade()
                .into(imageView)
    }

    fun loadRoundImage(context: Context?, imgPath: String?, imageView: ImageView, placeholderResId: Int, failedResId: Int) {
        if (context == null) {
            return
        }
        val appContext = context.applicationContext
        Glide.with(appContext).load(imgPath)
                .asBitmap()
                .placeholder(placeholderResId)
                .error(failedResId)
                .centerCrop()
                .into(object : BitmapImageViewTarget(imageView) {
                    override fun setResource(resource: Bitmap) {
                        val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(context.resources, resource)
                        circularBitmapDrawable.isCircular = true
                        imageView.setImageDrawable(circularBitmapDrawable)
                    }
                })
    }

    fun loadImageWithNoAnimate(context: Context?, imgPath: String?, imageView: ImageView?, placeholderResId: Int, failedResId: Int) {
        Glide.with(context)
                .load(imgPath)
                .placeholder(placeholderResId)
                .error(failedResId)
                .dontAnimate()
                .crossFade()
                .into(imageView)
    }
}