package com.candy.capture.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.cardview.widget.CardView
import com.candy.capture.R
import com.candy.capture.core.ConstantValues
import com.candy.capture.core.DBHelper
import com.candy.capture.fragment.ContentListFragment
import com.candy.capture.fragment.ContentListFragment.OnFragmentInteractionListener
import com.candy.commonlibrary.utils.CommonTools
import com.candy.commonlibrary.utils.DensityUtil
import com.jaeger.library.StatusBarUtil
import java.util.*

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 10:54
 */
class SearchActivity: BaseActivity(), View.OnClickListener, OnFragmentInteractionListener {
    private var mBackIv: ImageView? = null
    private var mKeyWordTv: EditText? = null
    private var mCleanIv: ImageView? = null

    private var mScrollView: ScrollView? = null
    private var mSearchHistoryLl: LinearLayout? = null

    private var mCategoryLl: LinearLayout? = null
    private var mCategoryTextLl: LinearLayout? = null
    private var mCategoryAudioLl: LinearLayout? = null
    private var mCategoryPhotoLl: LinearLayout? = null
    private var mCategoryVideoLl: LinearLayout? = null

    private var mLocationCv: CardView? = null
    private var mLocationLl: LinearLayout? = null

    private var mDBHelper: DBHelper? = null

    private var mCityList: ArrayList<String>? = null
    private var mHistoryList: ArrayList<String>? = null

    private lateinit var mContentListFragment: ContentListFragment

    /**
     * 标识是否处于编辑模式
     */
    private var mIsInEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        findView()
        setView()
        mDBHelper = DBHelper(mContext)
        getData()
        if (savedInstanceState == null) {
            mContentListFragment = ContentListFragment.newInstance(true)
            supportFragmentManager.beginTransaction().add(R.id.fl_result, mContentListFragment, "content_list").commit()
        } else {
            mContentListFragment = supportFragmentManager.findFragmentByTag("content_list") as ContentListFragment
        }
        StatusBarUtil.setLightMode(this)
        StatusBarUtil.setColor(this, resources.getColor(R.color.colorWhite), 0)
    }

    private fun findView() {
        mBackIv = findViewById<View>(R.id.iv_back) as ImageView
        mKeyWordTv = findViewById<View>(R.id.et_key_word) as EditText
        mCleanIv = findViewById<View>(R.id.iv_clean) as ImageView
        mScrollView = findViewById<View>(R.id.scroll_view) as ScrollView
        mSearchHistoryLl = findViewById<View>(R.id.ll_history) as LinearLayout
        mCategoryLl = findViewById<View>(R.id.ll_category) as LinearLayout
        mCategoryTextLl = findViewById<View>(R.id.ll_text) as LinearLayout
        mCategoryAudioLl = findViewById<View>(R.id.ll_audio) as LinearLayout
        mCategoryPhotoLl = findViewById<View>(R.id.ll_photo) as LinearLayout
        mCategoryVideoLl = findViewById<View>(R.id.ll_video) as LinearLayout
        mLocationCv = findViewById<View>(R.id.cv_location) as CardView
        mLocationLl = findViewById<View>(R.id.ll_location) as LinearLayout
    }

    private fun setView() {
        mBackIv!!.setOnClickListener(this)
        mKeyWordTv!!.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                if (!TextUtils.isEmpty(mKeyWordTv!!.text)) {
                    mCleanIv!!.visibility = View.VISIBLE
                }
            } else {
                mCleanIv!!.visibility = View.GONE
            }
        }
        mKeyWordTv!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (mKeyWordTv!!.hasFocus()) {
                    if (!TextUtils.isEmpty(mKeyWordTv!!.text)) {
                        mCleanIv!!.visibility = View.VISIBLE
                    } else {
                        mCleanIv!!.visibility = View.GONE
                    }
                }
            }
        })
        mKeyWordTv!!.setOnEditorActionListener { v, actionId, event ->
            if (EditorInfo.IME_ACTION_SEARCH == actionId) {
                val keyword = mKeyWordTv!!.text.toString()
                if (mContentListFragment != null && !TextUtils.isEmpty(keyword) && !TextUtils.isEmpty(keyword.trim { it <= ' ' })) {
                    search(keyword)
                }
            }
            true
        }
        mCleanIv!!.setOnClickListener(this)
        mCategoryTextLl!!.setOnClickListener(this)
        mCategoryAudioLl!!.setOnClickListener(this)
        mCategoryPhotoLl!!.setOnClickListener(this)
        mCategoryVideoLl!!.setOnClickListener(this)
    }

    private fun getData() {
        getHistory()
        getCities()
    }

    private fun getHistory() {
        mHistoryList = mDBHelper!!.getSearchHistory()
        if (mHistoryList != null && !mHistoryList!!.isEmpty()) {
            mSearchHistoryLl!!.visibility = View.VISIBLE
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtil.dip2px(mContext, 50f))
            for (history in mHistoryList!!) {
                val view = View.inflate(this, R.layout.item_history, null)
                val keywordTv = view.findViewById<View>(R.id.tv_keyword) as TextView
                keywordTv.text = history
                view.setOnClickListener { search(history) }
                mSearchHistoryLl!!.addView(view, params)
            }
        } else {
            mSearchHistoryLl!!.visibility = View.GONE
        }
    }

    private fun getCities() {
        mCityList = mDBHelper!!.getAllCity()
        if (mCityList != null && !mCityList!!.isEmpty()) {
            mLocationCv!!.visibility = View.VISIBLE
            mLocationLl!!.visibility = View.VISIBLE
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtil.dip2px(mContext, 50f))
            for (city in mCityList!!) {
                val view = View.inflate(this, R.layout.item_city, null)
                val cityTv = view.findViewById<View>(R.id.tv_city) as TextView
                cityTv.text = city
                view.setOnClickListener { filterCity(city) }
                mLocationLl!!.addView(view, params)
            }
        } else {
            mLocationCv!!.visibility = View.GONE
            mLocationLl!!.visibility = View.GONE
        }
    }


    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_back -> finish()
            R.id.iv_clean -> mKeyWordTv?.text = null
            R.id.ll_text -> {
                mKeyWordTv!!.setText(getString(R.string.pure_text))
                filterCategory(ConstantValues.CONTENT_TYPE_TEXT)
            }
            R.id.ll_audio -> {
                mKeyWordTv!!.setText(getString(R.string.audio))
                filterCategory(ConstantValues.CONTENT_TYPE_AUDIO)
            }
            R.id.ll_photo -> {
                mKeyWordTv!!.setText(getString(R.string.photo))
                filterCategory(ConstantValues.CONTENT_TYPE_PHOTO)
            }
            R.id.ll_video -> {
                mKeyWordTv!!.setText(getString(R.string.video))
                filterCategory(ConstantValues.CONTENT_TYPE_VIDEO)
            }
        }
    }

    private fun search(keyword: String) {
        CommonTools.hideSoftKeyboard(this@SearchActivity)
        mKeyWordTv!!.setText(keyword)
        mScrollView!!.visibility = View.GONE
        mSearchHistoryLl!!.visibility = View.GONE
        mCleanIv!!.visibility = View.GONE
        mDBHelper!!.insertSearchHistory(keyword)
        mContentListFragment!!.search(keyword, ContentListFragment.SEARCH_TYPE_SEARCH_DESC)
    }

    private fun filterCategory(type: Int) {
        CommonTools.hideSoftKeyboard(this@SearchActivity)
        mScrollView!!.visibility = View.GONE
        mSearchHistoryLl!!.visibility = View.GONE
        mCleanIv!!.visibility = View.GONE
        mContentListFragment!!.search(type.toString(), ContentListFragment.SEARCH_TYPE_SEARCH_CATEGORY)
    }

    private fun filterCity(city: String) {
        CommonTools.hideSoftKeyboard(this@SearchActivity)
        mKeyWordTv!!.setText(city)
        mScrollView!!.visibility = View.GONE
        mSearchHistoryLl!!.visibility = View.GONE
        mCleanIv!!.visibility = View.GONE
        mContentListFragment!!.search(city, ContentListFragment.SEARCH_TYPE_SEARCH_CITY)
    }

    override fun onBackPressed() {
        if (mIsInEditMode) {
            exitEditMode()
            if (mContentListFragment != null) {
                mContentListFragment!!.exitEditMode(true)
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun gotoEditMode() {
        mIsInEditMode = true
        // 改变菜单项
        invalidateOptionsMenu()
    }

    override fun exitEditMode() {
        mIsInEditMode = false
        // 改变菜单项
        invalidateOptionsMenu()
    }
}