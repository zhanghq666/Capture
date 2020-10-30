package com.candy.commonlibrary.core

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.candy.commonlibrary.utils.GlideImageLoader


/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/10/19 9:40
 */
class ImageBindingAdapter {
    companion object {
        @JvmStatic
        @BindingAdapter("android:src")
        fun setSrc(view: ImageView, bitmap: Bitmap?) {
            view.setImageBitmap(bitmap)
        }

        @JvmStatic
        @BindingAdapter("android:src")
        fun setSrc(view: ImageView, resId: Int) {
            view.setImageResource(resId)
        }

        @JvmStatic
        @BindingAdapter("imageUrl")
        fun setSrc(imageView: ImageView, url: String?) {
            url?.apply {
                GlideImageLoader.getInstance().loadImage(imageView.context, this, imageView)
            }
        }
    }
}