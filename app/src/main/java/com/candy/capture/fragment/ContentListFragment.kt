package com.candy.capture.fragment

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.candy.capture.R
import com.candy.capture.activity.AliPlayerActivity
import com.candy.capture.adapter.ContentListAdapter
import com.candy.capture.adapter.ContentListAdapter.AudioPlayCallBack
import com.candy.capture.adapter.ContentListAdapter.ItemClickCallback
import com.candy.capture.core.DBHelper
import com.candy.capture.customview.ContentListDivider
import com.candy.capture.customview.ImageViewerDialog
import com.candy.capture.model.Content
import com.candy.capture.model.MediaPlayState
import com.candy.commonlibrary.utils.LogUtil
import com.candy.commonlibrary.utils.TipsUtil
import java.io.File
import java.io.IOException
import java.util.*

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:57
 */
class ContentListFragment: Fragment(), ItemClickCallback, AudioPlayCallBack {


    private var mIsSearch = false
    private var mKerWord: String? = null
    private var mSearchType = 0

    private var mListener: OnFragmentInteractionListener? = null

    private var mDBHelper: DBHelper? = null

    private var mContentsRv: RecyclerView? = null

    /**
     * 内容集合
     */
    private var mContentList: ArrayList<Content>? = null

    /**
     * 编辑状态下选中内容项集合
     */
    private var mSelectedContents: ArrayList<Content>? = null

    /**
     * RecyclerView Adapter
     */
    private var mContentListAdapter: ContentListAdapter? = null

    /**
     * 音频MediaPlayer实例
     */
    private var mMediaPlayer: MediaPlayer? = null

    /**
     * 音频MediaPlayer状态
     */
    private var mPlayState: MediaPlayState? = null

    /**
     * 正在播放音频的列表项索引
     */
    private var mCurrentPlayingItemIndex = -1
    private var mIsLoadingData = false
    private var mHasMore = false

    /**
     * 标识是否处于编辑模式
     */
    private var mIsInEditMode = false
    private var mProgressDialog: ProgressDialog? = null

    companion object {
        private const val TAG = "ContentListFragment"

        const val SEARCH_TYPE_NO_SEARCH = 0
        const val SEARCH_TYPE_SEARCH_DESC = 1
        const val SEARCH_TYPE_SEARCH_CATEGORY = 2
        const val SEARCH_TYPE_SEARCH_CITY = 3

        private const val ARG_IS_SEARCH = "param_is_search"

        // 列表获取更多时用到的提前量
        private const val LIST_BUFFER_THRESHOLD = 3
        private const val COUNT = 20

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param isSearch 是否搜索
         * @return A new instance of fragment ContentListFragment.
         */
        fun newInstance(isSearch: Boolean): ContentListFragment {
            val fragment = ContentListFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_SEARCH, isSearch)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mIsSearch = arguments!!.getBoolean(ARG_IS_SEARCH)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_content_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mDBHelper = DBHelper(context)
        mContentList = ArrayList()
        findView(view)
        setView()
        if (!mIsSearch) {
            getContents()
        }
    }

    private fun findView(rootView: View) {
        mContentsRv = rootView.findViewById<View>(R.id.rv_contents) as RecyclerView
    }

    private fun setView() {
        mContentListAdapter = ContentListAdapter(context, mContentList, this, this)
        mContentsRv!!.adapter = mContentListAdapter
        mContentsRv!!.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mContentsRv!!.addItemDecoration(ContentListDivider(context!!))
        mContentsRv!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val totalItemCount = layoutManager!!.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (mHasMore && !mIsLoadingData && totalItemCount < lastVisibleItem + LIST_BUFFER_THRESHOLD) {
                    getContents()
                }
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    //region 获取内容
    fun getNewerContents() {
        mIsLoadingData = true
        var contents: ArrayList<Content>? = null
        contents = if (mContentList!!.isEmpty()) {
            mDBHelper!!.getContentNewer(-1, COUNT)
        } else {
            mDBHelper!!.getContentNewer(mContentList!![0].id, COUNT)
        }
        mContentList!!.addAll(0, contents!!)
        val count = contents.size
        mContentListAdapter!!.notifyItemRangeInserted(0, count)
        mIsLoadingData = false
    }

    fun search(keyWord: String?, searchType: Int) {
        mKerWord = keyWord
        mSearchType = searchType
        if (!mContentList!!.isEmpty()) {
            mContentList!!.clear()
            mContentListAdapter!!.notifyDataSetChanged()
        }
        getContents()
    }

    private fun getContents() {
        mIsLoadingData = true
        if (mContentList!!.isEmpty()) {
            if (mProgressDialog == null) {
                mProgressDialog = ProgressDialog.show(context, null, null, true, true)
            } else {
                mProgressDialog!!.show()
            }
        }
        var contents: ArrayList<Content>? = null
        when (mSearchType) {
            SEARCH_TYPE_NO_SEARCH -> {
                val startId = if (mContentList!!.isEmpty()) -1 else mContentList!![mContentList!!.size - 1].id
                contents = mDBHelper!!.getContentOlder(startId, COUNT)
            }
            SEARCH_TYPE_SEARCH_CATEGORY -> try {
                val type = mKerWord!!.toInt()
                contents = mDBHelper!!.searchByType(type, COUNT)
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
            SEARCH_TYPE_SEARCH_CITY -> contents = mDBHelper!!.searchByCity(mKerWord!!, COUNT)
            SEARCH_TYPE_SEARCH_DESC -> contents = mDBHelper!!.searchByDesc(mKerWord!!, COUNT)
        }
        if (contents != null && contents.size > 0) {
            val startPosition = if (mContentList!!.isEmpty()) 0 else mContentList!!.size - 1
            val count = contents.size
            mContentList!!.addAll(contents)
            mContentListAdapter!!.notifyItemRangeInserted(startPosition, count)
        }
        mHasMore = contents != null && contents.size == COUNT
        if (mProgressDialog != null && mProgressDialog!!.isShowing) {
            mProgressDialog!!.dismiss()
        }
        mIsLoadingData = false
    }
    //endregion

    //region 列表项事件回调
    override fun onItemClick(position: Int) {
        if (mIsInEditMode) {
            mContentList!![position].isSelect = !mContentList!![position].isSelect
            if (mSelectedContents == null) mSelectedContents = ArrayList()
            if (mContentList!![position].isSelect) {
                mSelectedContents!!.add(mContentList!![position])
            } else {
                mSelectedContents!!.remove(mContentList!![position])
            }
            if (mSelectedContents!!.isEmpty()) {
                exitEditMode(false)
            } else {
                mContentListAdapter!!.notifyItemChanged(position)
            }
        }
    }

    override fun onItemLongPressed(position: Int) {
        if (mIsSearch) return
        if (!mIsInEditMode) {
            mContentList!![position].isSelect = true
            if (mSelectedContents == null) mSelectedContents = ArrayList()
            mSelectedContents!!.add(mContentList!![position])
            gotoEditMode()
        }
    }

    override fun onAudioViewClick(position: Int) {
        if (mIsInEditMode) {
            onItemClick(position)
            return
        }
        if (mCurrentPlayingItemIndex == position) {
            if (MediaPlayState.STARTED == mPlayState) {
                stopMedia()
            } else if (MediaPlayState.COMPLETED == mPlayState || MediaPlayState.PREPARED == mPlayState) {
                playMedia()
            } else if (MediaPlayState.STOPPED == mPlayState) {
                prepareMediaPlayer()
                playMedia()
            }
        } else {
            if (mCurrentPlayingItemIndex >= 0) {
                if (MediaPlayState.STARTED == mPlayState) {
                    stopMedia()
                }
                mMediaPlayer!!.reset()
                mPlayState = MediaPlayState.IDLE
            }
            mCurrentPlayingItemIndex = position
            setMediaPlayerSource()
            prepareMediaPlayer()
            playMedia()
        }
    }

    override fun onImageViewClick(position: Int) {
        if (mIsInEditMode) {
            onItemClick(position)
            return
        }
        val list = ArrayList<String>()
        list.add(mContentList!![position].mediaFilePath!!)
        ImageViewerDialog.showDialog(context!!, list, 1)
    }

    override fun onVideoViewClick(position: Int) {
        if (mIsInEditMode) {
            onItemClick(position)
            return
        }
        if (MediaPlayState.STARTED == mPlayState) {
            stopMedia()
        }
        val intent = Intent(context, AliPlayerActivity::class.java)
        intent.putExtra(AliPlayerActivity.EXTRA_MEDIA_PATH_KEY, mContentList!![position].mediaFilePath)
        startActivity(intent)
//        Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
//        intent.putExtra(VideoPlayerActivity.EXTRA_MEDIA_PATH_KEY, mContentList.get(position).getMediaFilePath());
//        startActivity(intent);
    }

    private fun gotoEditMode() {
        mIsInEditMode = true
        if (mListener != null) {
            mListener!!.gotoEditMode()
        }
        // 停止播放音频、释放资源
        releaseMedia()
        mCurrentPlayingItemIndex = -1
        // 列表显示变化
        mContentListAdapter!!.setEditMode(true)
        mContentListAdapter!!.notifyDataSetChanged()
    }

    fun exitEditMode(fromParent: Boolean) {
        mIsInEditMode = false
        if (!fromParent) {
            if (mListener != null) {
                mListener!!.exitEditMode()
            }
        }
        // 重新组织MediaPlayer
        initMediaPlayer()
        // 清空已选项集合
        if (mSelectedContents != null && !mSelectedContents!!.isEmpty()) {
            for (content in mSelectedContents!!) {
                content.isSelect = false
            }
            mSelectedContents!!.clear()
        }
        // 列表显示变化
        mContentListAdapter!!.setEditMode(false)
        mContentListAdapter!!.notifyDataSetChanged()
    }
    //endregion

    //region 语音播放相关
    override fun onResume() {
        super.onResume()
        initMediaPlayer()
    }

    /**
     * 供Adapter调用获取当前音频播放进度
     *
     * @return
     */
    override fun getPlayPercent(): Double {
        var percent = 0.0
        if (MediaPlayState.STARTED == mPlayState) {
            percent = mMediaPlayer!!.currentPosition.toDouble() / mMediaPlayer!!.duration
        }
        LogUtil.d(TAG, "getPlayPercent percent = $percent")
        return percent
    }

    private fun initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer()
            mPlayState = MediaPlayState.IDLE
            mMediaPlayer!!.setOnCompletionListener {
                mPlayState = MediaPlayState.COMPLETED
                mContentList!![mCurrentPlayingItemIndex].isPlayingAudio = false
                mContentListAdapter!!.notifyItemChanged(mCurrentPlayingItemIndex)
            }
            mMediaPlayer!!.setOnErrorListener { mp, what, extra ->
                LogUtil.e(TAG, "MediaPlayer onError")
                mPlayState = MediaPlayState.ERROR
                mp.reset()
                mPlayState = MediaPlayState.IDLE
                initMediaPlayer()
                true
            }
        }
        if (mCurrentPlayingItemIndex >= 0) {
            setMediaPlayerSource()
            prepareMediaPlayer()
        }
    }

    private fun setMediaPlayerSource() {
        try {
            mMediaPlayer!!.setDataSource(mContentList!![mCurrentPlayingItemIndex].mediaFilePath)
            mPlayState = MediaPlayState.INITIALIZED
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun prepareMediaPlayer() {
        try {
            mMediaPlayer!!.prepare()
            mPlayState = MediaPlayState.PREPARED
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun playMedia() {
        mMediaPlayer!!.start()
        mPlayState = MediaPlayState.STARTED
        mContentList!![mCurrentPlayingItemIndex].isPlayingAudio = true
        mContentListAdapter!!.notifyItemChanged(mCurrentPlayingItemIndex)
    }

    private fun stopMedia() {
        mMediaPlayer!!.stop()
        mPlayState = MediaPlayState.STOPPED
        mContentList!![mCurrentPlayingItemIndex].isPlayingAudio = false
        mContentListAdapter!!.notifyItemChanged(mCurrentPlayingItemIndex)
    }

    private fun releaseMedia() {
        if (mMediaPlayer != null) {
            if (MediaPlayState.STARTED == mPlayState) {
                stopMedia()
            }
            mMediaPlayer!!.release()
            mPlayState = MediaPlayState.END
            mMediaPlayer = null
        }
    }

    override fun onPause() {
        super.onPause()
        releaseMedia()
    }

    //endregion

    fun checkDeleteContent() {
        if (mSelectedContents != null && !mSelectedContents!!.isEmpty()) {
            val dialog = AlertDialog.Builder(context!!)
                    .setMessage("要删除此内容吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除") { dialog, which -> deleteContent() }
                    .create()
            dialog.show()
        }
    }

    private fun deleteContent() {
        if (mSelectedContents != null && !mSelectedContents!!.isEmpty()) {
            try {
                if (!mDBHelper!!.deleteContent(mSelectedContents)) {
                    TipsUtil.showToast(context, "删除所选内容失败")
                    return
                }
                mContentList!!.removeAll(mSelectedContents!!)
                for (content in mSelectedContents!!) {
                    if (!TextUtils.isEmpty(content.mediaFilePath)) {
                        File(content.mediaFilePath).delete()
                    }
                }
                exitEditMode(false)
            } catch (e: Exception) {
                e.printStackTrace()
                TipsUtil.showToast(context, "删除所选内容失败")
            }
        }
    }

    interface OnFragmentInteractionListener {
        fun gotoEditMode()
        fun exitEditMode()
    }
}