package com.candy.capture.viewmodel

import android.media.MediaPlayer
import android.text.TextUtils
import androidx.lifecycle.*
import androidx.paging.PagedList
import androidx.paging.toLiveData
import com.candy.capture.adapter.ContentListAdapter
import com.candy.capture.core.AppDatabase
import com.candy.capture.core.CaptureApplication
import com.candy.capture.dao.ContentDao
import com.candy.capture.model.Content
import com.candy.capture.model.MediaPlayState
import com.candy.commonlibrary.utils.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/10/14 14:39
 */
class ContentListViewModel: ViewModel(), ContentListAdapter.AudioPlayCallBack {

    companion object {
        const val TAG = "ContentListViewModel"
    }

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


    private val contentDao: ContentDao = AppDatabase.get(CaptureApplication.getInstance()).contentDao()

    var contentList: LiveData<PagedList<Content>> = MutableLiveData()

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

    fun initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer()
            mPlayState = MediaPlayState.IDLE
            mMediaPlayer!!.setOnCompletionListener {
                mPlayState = MediaPlayState.COMPLETED
                contentList.value!![mCurrentPlayingItemIndex]!!.playingAudio = false
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
            mMediaPlayer!!.setDataSource(contentList.value!![mCurrentPlayingItemIndex]!!.mediaFilePath)
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
        contentList.value!![mCurrentPlayingItemIndex]!!.playingAudio = true
    }

    fun stopMedia() {
        if (MediaPlayState.STARTED == mPlayState) {
            mMediaPlayer!!.stop()
            mPlayState = MediaPlayState.STOPPED
            contentList.value!![mCurrentPlayingItemIndex]!!.playingAudio = false
        }
    }

    fun releaseMedia() {
        mCurrentPlayingItemIndex = -1
        if (mMediaPlayer != null) {
            if (MediaPlayState.STARTED == mPlayState) {
                stopMedia()
            }
            mMediaPlayer!!.release()
            mPlayState = MediaPlayState.END
            mMediaPlayer = null
        }
    }

    fun deleteContent() {
        viewModelScope.launch {
            if (contentList.value != null) {
                val selectedValue = contentList.value!!.filter { ct ->
                    ct!!.select
                }

                if (selectedValue.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        contentDao.deleteContentAndUpdateCity(selectedValue)
                        for (content in selectedValue) {
                            if (!TextUtils.isEmpty(content.mediaFilePath)) {
                                File(content.mediaFilePath).delete()
                            }
                        }
                    }
                }
            }
        }
    }

    fun onAudioViewClick(content: Content) {
        var position = contentList.value?.indexOf(content)
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
            mCurrentPlayingItemIndex = position!!
            setMediaPlayerSource()
            prepareMediaPlayer()
            playMedia()
        }
    }

    fun exitEditMode() {
        contentList.value?.let {
            it.forEach { item -> item.select = false }
        }
    }

    fun getAllContent() {
        contentList = contentDao.contentsByDate().toLiveData(pageSize = 10)
    }


    fun searchByType(type: Int) {
        contentList = contentDao.searchContentByType(type).toLiveData(pageSize = 10)
    }

    fun searchByCity(city: String) {
        contentList = contentDao.searchContentByCity(city).toLiveData(pageSize = 10)
    }

    fun searchByDesc(keyword: String) {
        contentList = contentDao.searchContentByDesc(keyword).toLiveData(pageSize = 10)
    }
}