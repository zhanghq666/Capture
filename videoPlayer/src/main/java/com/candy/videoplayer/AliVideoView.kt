package com.candy.videoplayer

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.RelativeLayout
import com.aliyun.player.AliPlayer
import com.aliyun.player.AliPlayerFactory
import com.aliyun.player.IPlayer.*
import com.aliyun.player.bean.ErrorInfo
import com.aliyun.player.bean.InfoCode
import com.aliyun.player.nativeclass.CacheConfig
import com.aliyun.player.nativeclass.TrackInfo
import com.aliyun.player.source.UrlSource
import com.candy.commonlibrary.utils.LogUtil.d

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 16:21
 */
class AliVideoView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : RelativeLayout(context, attrs, defStyleAttr) {
    companion object {
        private const val TAG = "AliVideoView"
    }

    private lateinit var mSurfaceTv: TextureView
    private lateinit var mAliPlayer: AliPlayer
    private var mCurrentMediaPath: String? = null
    private var mCurrentPositionMs: Long = 0
    private var mBufferedPositionMs: Long = 0
    private var mCurrentState = 0

    private var onCompletionListener: OnCompletionListener? = null
    private var onVideoSizeChangedListener: OnVideoSizeChangedListener? = null
    private var onErrorListener: OnErrorListener? = null

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        init()
    }

    private fun init() {
        initPlayer()
        LayoutInflater.from(context).inflate(R.layout.layout_video_view, this)
        mSurfaceTv = findViewById<View>(R.id.tv_surface) as TextureView
        mSurfaceTv.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                d(TAG, "onSurfaceTextureAvailable")
                mAliPlayer.setSurface(Surface(surface))
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                d(TAG, "onSurfaceTextureSizeChanged width = $width height = $height")
                mAliPlayer.redraw()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                d(TAG, "onSurfaceTextureDestroyed")
                mAliPlayer.setDisplay(null)
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun initPlayer() {
        mAliPlayer = AliPlayerFactory.createAliPlayer(context.applicationContext)
        mAliPlayer.isAutoPlay = true
        // 缓存设置
        val cacheConfig = CacheConfig()
        cacheConfig.mEnable = true
        cacheConfig.mMaxDurationS = 10
        cacheConfig.mMaxSizeMB = 100
        mAliPlayer.setCacheConfig(cacheConfig)
        // 缓冲相关、网络请求相关设置
//        PlayerConfig playerConfig = mAliPlayer.getConfig();
//        mAliPlayer.setConfig(playerConfig);
        mAliPlayer.setOnCompletionListener { //播放完成事件
            d(TAG, "onCompletion")
            if (onCompletionListener != null) {
                onCompletionListener!!.onCompletion()
            }
        }
        mAliPlayer.setOnErrorListener { errorInfo -> //出错事件
            d(TAG, "onError " + errorInfo.msg)
            if (onErrorListener != null) {
                onErrorListener!!.onError(errorInfo.code.value, errorInfo.msg)
            }
        }
        mAliPlayer.setOnPreparedListener { //准备成功事件
            d(TAG, "onPrepared")
        }
        mAliPlayer.setOnVideoSizeChangedListener { width, height -> //视频分辨率变化回调
            d(TAG, "onVideoSizeChanged " + width + "x" + height)
            if (onVideoSizeChangedListener != null) {
                onVideoSizeChangedListener!!.onVideoSizeChanged(width, height)
            }
        }
        mAliPlayer.setOnRenderingStartListener { //首帧渲染显示事件
            d(TAG, "onRenderingStart")
        }
        mAliPlayer.setOnInfoListener { infoBean -> //其他信息的事件，type包括了：循环播放开始，缓冲位置，当前播放位置，自动播放开始等
            d(TAG, "onInfo " + infoBean.code + " " + infoBean.extraMsg + " " + infoBean.extraValue)
            if (InfoCode.CurrentPosition == infoBean.code) {
                mCurrentPositionMs = infoBean.extraValue
            } else if (InfoCode.BufferedPosition == infoBean.code) {
                mBufferedPositionMs = infoBean.extraValue
            }
        }
        mAliPlayer.setOnLoadingStatusListener(object : OnLoadingStatusListener {
            override fun onLoadingBegin() {
                //缓冲开始。
                d(TAG, "onLoadingBegin")
            }

            override fun onLoadingProgress(percent: Int, kbps: Float) {
                //缓冲进度
                d(TAG, "onLoadingProgress percent:$percent kbps:$kbps")
            }

            override fun onLoadingEnd() {
                //缓冲结束
                d(TAG, "onLoadingEnd")
            }
        })
        mAliPlayer.setOnSeekCompleteListener { //拖动结束
            d(TAG, "onSeekComplete")
        }
        mAliPlayer.setOnSubtitleDisplayListener(object : OnSubtitleDisplayListener {
            override fun onSubtitleShow(id: Long, data: String) {
                //显示字幕
                d(TAG, "onSubtitleShow")
            }

            override fun onSubtitleHide(id: Long) {
                //隐藏字幕
                d(TAG, "onSubtitleHide")
            }
        })
        mAliPlayer.setOnTrackChangedListener(object : OnTrackChangedListener {
            override fun onChangedSuccess(trackInfo: TrackInfo) {
                //切换音视频流或者清晰度成功
                d(TAG, "onChangedSuccess")
            }

            override fun onChangedFail(trackInfo: TrackInfo, errorInfo: ErrorInfo) {
                //切换音视频流或者清晰度失败
                d(TAG, "onChangedFail")
            }
        })
        mAliPlayer.setOnStateChangedListener { newState -> //播放器状态改变事件
            d(TAG, "onStateChanged newState:$newState")
            mCurrentState = newState
        }
        mAliPlayer.setOnSnapShotListener{ bm, with, height -> //截图事件
            d(TAG, "onSnapShot")
        }
    }

    fun setMediaSource(path: String?) {
        mCurrentMediaPath = path
        val urlSource = UrlSource()
        urlSource.uri = path
        mAliPlayer.setDataSource(urlSource)
        mAliPlayer.prepare()
    }

    fun seekTo(progress: Long) {
        mAliPlayer.seekTo(progress)
    }

    fun getCurrentPosition(): Long {
        return mCurrentPositionMs
    }

    fun getBufferedPosition(): Long {
        return mBufferedPositionMs
    }

    fun getVideoWidth(): Int {
        return mAliPlayer.videoWidth
    }

    fun getVideoHeight(): Int {
        return mAliPlayer.videoHeight
    }

    fun start() {
        mAliPlayer.start()
    }

    fun reload() {
        mAliPlayer.reload()
    }

    fun pause() {
        mAliPlayer.pause()
    }

    fun stop() {
        mAliPlayer.stop()
    }

    fun release() {
        mAliPlayer.release()
    }

    fun getDuration(): Long {
        return mAliPlayer.duration
    }

    fun isPlayable(): Boolean {
        return mCurrentState == PlayerState.PREPARED.value || mCurrentState == PlayerState.STARTED.value || mCurrentState == PlayerState.PAUSED.value
    }

    fun isPlaying(): Boolean {
        return mCurrentState == PlayerState.STARTED.value
    }

    fun isPaused(): Boolean {
        return mCurrentState == PlayerState.PAUSED.value
    }

    fun isCompleted(): Boolean {
        return mCurrentState == PlayerState.COMPLETED.value
    }

    fun setOnCompletionListener(listener: OnCompletionListener?) {
        onCompletionListener = listener
    }

    fun setOnVideoSizeChangedListener(listener: OnVideoSizeChangedListener?) {
        onVideoSizeChangedListener = listener
    }

    fun setOnErrorListener(listener: OnErrorListener?) {
        onErrorListener = listener
    }

    interface OnCompletionListener {
        fun onCompletion()
    }

    interface OnVideoSizeChangedListener {
        fun onVideoSizeChanged(width: Int, height: Int)
    }

    interface OnErrorListener {
        fun onError(code: Int, msg: String?)
    }

    /**
     * 这是通过OnStateChangedListener现象推测出的状态含义枚举，SDK上报的状态只有一个int值，也没找到关于这个int值的说明文档
     */
    internal enum class PlayerState(val value: Int) {
        IDLE(0), INITIALIZED(1), PREPARED(2), STARTED(3), PAUSED(4), COMPLETED(6);
    }
}