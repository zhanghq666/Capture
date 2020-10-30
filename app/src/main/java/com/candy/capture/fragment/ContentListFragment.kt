package com.candy.capture.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.candy.capture.activity.AliPlayerActivity
import com.candy.capture.adapter.ContentListAdapter
import com.candy.capture.customview.ContentListDivider
import com.candy.capture.customview.ImageViewerDialog
import com.candy.capture.databinding.FragmentContentListBinding
import com.candy.capture.model.Content
import com.candy.capture.viewmodel.ContentListViewModel
import kotlinx.android.synthetic.main.fragment_content_list.*
import java.util.*

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:57
 */
class ContentListFragment : Fragment() {


    private var mIsSearch = false
    private var mKerWord: String? = null
    private var mSearchType = 0

    private var mListener: OnFragmentInteractionListener? = null

    /**
     * RecyclerView Adapter
     */
    private var mContentListAdapter: ContentListAdapter? = null
    private var mHasMore = false

    /**
     * 标识是否处于编辑模式
     */
    private var mIsInEditMode = false

    private val vm: ContentListViewModel by lazy {
        ContentListViewModel()
    }

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
        var binding = FragmentContentListBinding.inflate(layoutInflater, container, false)
        return binding.root
//        return inflater.inflate(R.layout.fragment_content_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        mDBHelper = DBHelper(context)
//        mContentList = ArrayList()
        setView()
    }

    private fun setView() {
        mContentListAdapter = ContentListAdapter(context, vm)
        mContentListAdapter!!.callbackContent = object : ContentListAdapter.ItemClickContentCallback {
            override fun onItemClick(content: Content) {
                if (mIsInEditMode) {
                    content.select = !content.select
                    /*mContentList!![position].isSelect = !mContentList!![position].isSelect
                    if (mSelectedContents == null) mSelectedContents = ArrayList()
                    if (mContentList!![position].isSelect) {
                        mSelectedContents!!.add(mContentList!![position])
                    } else {
                        mSelectedContents!!.remove(mContentList!![position])
                    }*/
                    if (vm.contentList.value!!.count { c -> c.select } <= 0) {
                        exitEditMode(false)
                    } else {
                        mContentListAdapter!!.notifyItemChanged(vm.contentList.value!!.indexOf(content))
                    }
                }
            }

            override fun onItemLongPressed(content: Content): Boolean {
//                val position = vm.contentList.value?.indexOf(content)
                if (mIsSearch) return true
                if (!mIsInEditMode) {
                    content.select = true
//                    if (mSelectedContents == null) mSelectedContents = ArrayList()
//                    mSelectedContents!!.add(mContentList!![position])
                    gotoEditMode()
                }
                return true
            }

            override fun onAudioViewClick(content: Content) {
//                val position = mContentList?.indexOf(content)!!
                if (mIsInEditMode) {
                    onItemClick(content)
                    return
                }
                vm.onAudioViewClick(content)
            }

            override fun onImageViewClick(content: Content) {
                if (mIsInEditMode) {
                    onItemClick(content)
                    return
                }
                val list = ArrayList<String>()
                list.add(content.mediaFilePath)
                ImageViewerDialog.showDialog(context!!, list, 1)
            }

            override fun onVideoViewClick(content: Content) {
                if (mIsInEditMode) {
                    onItemClick(content)
                    return
                }

                vm.stopMedia()

                val intent = Intent(context, AliPlayerActivity::class.java)
                intent.putExtra(AliPlayerActivity.EXTRA_MEDIA_PATH_KEY, content.mediaFilePath)
                startActivity(intent)
            }

        }
        contentRV.adapter = mContentListAdapter
        contentRV.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        contentRV.addItemDecoration(ContentListDivider(context!!))
        contentRV.setHasFixedSize(true)
//        contentRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
//                val totalItemCount = layoutManager!!.itemCount
//                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
//                if (mHasMore && !mIsLoadingData && totalItemCount < lastVisibleItem + LIST_BUFFER_THRESHOLD) {
//                    getContents()
//                }
//            }
//        })


        if (!mIsSearch) {
            vm.getAllContent()
            vm.contentList.observe(this, { mContentListAdapter!!.submitList(it) })
        }
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
//    fun getNewerContents() {
//        mIsLoadingData = true
//        var contents: ArrayList<Content>? = null
//        contents = if (mContentList!!.isEmpty()) {
//            mDBHelper!!.getContentNewer(-1, COUNT)
//        } else {
//            mDBHelper!!.getContentNewer(mContentList!![0].id!!, COUNT)
//        }
//        mContentList!!.addAll(0, contents!!)
//        val count = contents.size
//        mContentListAdapter!!.notifyItemRangeInserted(0, count)
//        mIsLoadingData = false
//    }

    fun search(keyWord: String?, searchType: Int) {
        mKerWord = keyWord
        mSearchType = searchType

//        if (!mContentList!!.isEmpty()) {
//            mContentList!!.clear()
//            mContentListAdapter!!.notifyDataSetChanged()
//        }
        getContents()
    }

    private fun getContents() {
        when (mSearchType) {
//            SEARCH_TYPE_NO_SEARCH -> vm.getAllContent()
            SEARCH_TYPE_SEARCH_CATEGORY -> try {
                val type = mKerWord!!.toInt()
                vm.searchByType(type)
                vm.contentList.observe(this, { mContentListAdapter!!.submitList(it) })
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
            SEARCH_TYPE_SEARCH_CITY -> {
                vm.searchByCity(mKerWord!!)
                vm.contentList.observe(this, { mContentListAdapter!!.submitList(it) })
            }
            SEARCH_TYPE_SEARCH_DESC -> {
                vm.searchByDesc(mKerWord!!)
                vm.contentList.observe(this, { mContentListAdapter!!.submitList(it) })
            }
        }
    }
    //endregion


    //region 语音播放相关
    override fun onResume() {
        super.onResume()
        vm.initMediaPlayer()
    }

    private fun gotoEditMode() {
        mIsInEditMode = true
        if (mListener != null) {
            mListener!!.gotoEditMode()
        }
        // 停止播放音频、释放资源
        vm.releaseMedia()
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
        vm.initMediaPlayer()
        // 清空已选项集合
        vm.exitEditMode()
        // 列表显示变化
        mContentListAdapter!!.setEditMode(false)
        mContentListAdapter!!.notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        vm.releaseMedia()
    }

    //endregion

    fun checkDeleteContent() {
        if (vm.contentList.value != null) {
            val selectedValue = vm.contentList.value!!.filter { ct ->
                ct!!.select
            }

            if (selectedValue.isNotEmpty()) {
                val dialog = AlertDialog.Builder(context!!)
                        .setMessage("要删除此内容吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除") { _, _ -> deleteContent() }
                        .create()
                dialog.show()
            }
        }
    }

    private fun deleteContent() {
        vm.deleteContent()
    }

    interface OnFragmentInteractionListener {
        fun gotoEditMode()
        fun exitEditMode()
    }
}