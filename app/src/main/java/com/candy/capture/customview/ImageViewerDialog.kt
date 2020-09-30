package com.candy.capture.customview

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.candy.capture.R
import com.candy.capture.util.GlideImageLoader
import java.util.*

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:49
 */
class ImageViewerDialog(context: Context, val dataSource: List<String>, var currentIndex: Int): Dialog(context, R.style.image_pager_dialog_theme) {
    private var mImagesVp: ViewPager? = null
    private var mIndexerTv: TextView? = null

    companion object {
        fun showDialog(context: Context, dataSource: List<String>?, currentIndex: Int) {
            if (dataSource == null || dataSource.isEmpty()) {
                return
            }
            val dialog = ImageViewerDialog(context, dataSource, if (currentIndex > dataSource.size) 0 else currentIndex)
            dialog.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_image_view_pager)
        val rootLayout = findViewById<View>(R.id.rl_root) as RelativeLayout
        rootLayout.setOnClickListener { dismiss() }
        mImagesVp = findViewById<View>(R.id.vp_images) as ViewPager
        mIndexerTv = findViewById<View>(R.id.tv_indexer) as TextView
        mIndexerTv?.text = "$currentIndex/${dataSource.size}"
        if (dataSource.size <= 1) {
            mIndexerTv?.visibility = View.GONE
        }
        val adapter = StringPagerAdapter(context, dataSource)
        mImagesVp!!.adapter = adapter
        mImagesVp!!.currentItem = currentIndex - 1
        mImagesVp!!.setOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageSelected(arg0: Int) {
                currentIndex = arg0 + 1
                mIndexerTv?.text = "$currentIndex/${dataSource.size}"
            }

            override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {
            }

            override fun onPageScrollStateChanged(arg0: Int) {
            }
        })
        setDialogAttributes()
    }

    private fun setDialogAttributes() {
        window.decorView.setPadding(0, 0, 0, 0)
        val wl = window.attributes
        wl.width = WindowManager.LayoutParams.MATCH_PARENT
        wl.height = WindowManager.LayoutParams.MATCH_PARENT
        window.attributes = wl
    }

    class A{
        fun Aaa(){}

        inner class B {
            fun bb() {
                Aaa()
            }
        }
    }
    fun ddd() {}

    fun dismiss1() {
        super.dismiss()
    }

    internal inner class StringPagerAdapter(private val context: Context?, private val mImages: List<String>?) : PagerAdapter() {
        private val mViewList: HashMap<Int, View> = HashMap()
        override fun getCount(): Int {
            return mImages?.size ?: 0
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(mViewList[position])
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val view = View.inflate(context, R.layout.view_image_item, null)
            val imageView = view.findViewById<View>(R.id.image) as ImageView
            imageView.setOnClickListener { dismiss() }
            GlideImageLoader.getInstance().loadImage(context, mImages!![position], imageView)
            mViewList[position] = view
            container.addView(view)
            return view
        }

        override fun isViewFromObject(arg0: View, arg1: Any): Boolean {
            return arg0 === arg1
        }

    }
}