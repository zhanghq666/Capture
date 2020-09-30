package com.candy.capture.activity

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.candy.capture.R
import com.candy.capture.model.MediaPlayState
import com.candy.capture.util.TimeUtil
import com.candy.commonlibrary.utils.DensityUtil
import com.candy.commonlibrary.utils.LogUtil
import com.candy.commonlibrary.utils.TipsUtil
import java.io.IOException

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 10:57
 */
class VideoPlayerActivity: BaseActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "VideoPlayerActivity"

        const val EXTRA_MEDIA_PATH_KEY = "extra_media_path"


        /**
         * If [.mAutoHide] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }

    /**
     * Whether or not the system UI should be auto-hidden after
     * [.AUTO_HIDE_DELAY_MILLIS] milliseconds.
     */
    private var mAutoHide = true

    private val mHandler = Handler()
    private val mHidePart2Runnable = Runnable { // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        mSurfaceTv!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
    private val mShowPart2Runnable = Runnable { // Delayed display of UI elements
        val actionBar = supportActionBar
        actionBar?.show()
        mProgressView!!.visibility = View.VISIBLE
        mControllerView!!.visibility = View.VISIBLE
        delayedHide(AUTO_HIDE_DELAY_MILLIS)
    }
    private val mHideRunnable = Runnable { hide() }
    private val mUpdateProgressRunnable: Runnable = object : Runnable {
        override fun run() {
            if (mMediaPlayer != null && isPlayable()) {
                mProgressSb!!.progress = mMediaPlayer!!.currentPosition
                mHandler.removeCallbacks(this)
                mHandler.postDelayed(this, 100)
            }
        }
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = OnTouchListener { view, motionEvent ->
        if (mAutoHide) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    private var mSurfaceTv: TextureView? = null
    private var mProgressView: View? = null
    private var mControllerView: View? = null
    private var mControllerVisible = false

    private var mPlayIv: ImageView? = null
    private var mBackwardIv: ImageView? = null
    private var mForwardIv: ImageView? = null

    private var mProgressSb: SeekBar? = null
    private var mCurrentProgressTv: TextView? = null
    private var mDurationTv: TextView? = null

    private var mMediaPlayer: MediaPlayer? = null
    private var mPlayState: MediaPlayState? = null
    private var mMediaFilePath: String? = null

//    private TimeCountThread mTimeCountThread;

    //    private TimeCountThread mTimeCountThread;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 导航栏、状态栏透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.colorTranslucence)
            window.navigationBarColor = resources.getColor(R.color.colorTranslucence)
            if (supportActionBar != null) {
                supportActionBar!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        }
        setContentView(R.layout.activity_video_player)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        mControllerVisible = true
        handleIntent()
        findView()
        setView()
        initMediaPlayer()
    }

    private fun handleIntent() {
        mMediaFilePath = intent.getStringExtra(EXTRA_MEDIA_PATH_KEY)
        if (TextUtils.isEmpty(mMediaFilePath)) {
            TipsUtil.showToast(this, "源文件位置未知")
            finish()
        }
    }

    private fun findView() {
        mProgressView = findViewById(R.id.rl_progress)
        mControllerView = findViewById(R.id.rl_controller)
        mSurfaceTv = findViewById<View>(R.id.tv_surface) as TextureView
        mPlayIv = findViewById<View>(R.id.iv_play) as ImageView
        mBackwardIv = findViewById<View>(R.id.iv_backward) as ImageView
        mForwardIv = findViewById<View>(R.id.iv_forward) as ImageView
        mProgressSb = findViewById<View>(R.id.seek_bar) as SeekBar
        mCurrentProgressTv = findViewById<View>(R.id.tv_current_progress) as TextView
        mDurationTv = findViewById<View>(R.id.tv_duration) as TextView
    }

    private fun setView() {
        mSurfaceTv!!.setOnClickListener { toggle() }
        mSurfaceTv!!.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                LogUtil.d(TAG, "onSurfaceTextureAvailable")
                if (mMediaPlayer == null || mPlayState == MediaPlayState.IDLE) {
                    initMediaPlayer()
                }
                mMediaPlayer!!.setSurface(Surface(surface))
                //                playMedia();
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                LogUtil.d(TAG, "onSurfaceTextureSizeChanged width = $width height = $height")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                LogUtil.d(TAG, "onSurfaceTextureDestroyed")
                releaseMedia()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        mPlayIv!!.setOnClickListener(this)
        mBackwardIv!!.setOnClickListener(this)
        mForwardIv!!.setOnClickListener(this)
        mProgressSb!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mMediaPlayer!!.seekTo(seekBar.progress)
                    delayedHide(AUTO_HIDE_DELAY_MILLIS)
                }
                mCurrentProgressTv!!.text = TimeUtil.formatDuration(mMediaPlayer!!.currentPosition / 1000.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        initMediaPlayer();
//    }

    //    @Override
    //    protected void onResume() {
    //        super.onResume();
    //        initMediaPlayer();
    //    }
    override fun onPause() {
        super.onPause()
        if (MediaPlayState.STARTED == mPlayState) {
            pauseMedia()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMedia()
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.home) {
            // This ID represents the Home or Up button.
//            NavUtils.navigateUpFromSameTask(this);
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toggle() {
        if (mControllerVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        val actionBar = supportActionBar
        actionBar?.hide()
        mProgressView!!.visibility = View.GONE
        mControllerView!!.visibility = View.GONE
        mControllerVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHandler.removeCallbacks(mShowPart2Runnable)
        mHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    @SuppressLint("InlinedApi")
    private fun show() {
        // Show the system bar
        mSurfaceTv!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        mControllerVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHandler.removeCallbacks(mHidePart2Runnable)
        mHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        if (mAutoHide) {
            mHandler.removeCallbacks(mHideRunnable)
            mHandler.postDelayed(mHideRunnable, delayMillis.toLong())
        }
    }

    private fun isPlayable(): Boolean {
        return mPlayState == MediaPlayState.PREPARED || mPlayState == MediaPlayState.STARTED || mPlayState == MediaPlayState.PAUSED
    }

    private fun initMediaPlayer() {
        LogUtil.d(TAG, "initMediaPlayer")
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer()
            mPlayState = MediaPlayState.IDLE
            mMediaPlayer!!.setScreenOnWhilePlaying(true)
            mMediaPlayer!!.setOnCompletionListener {
                mPlayState = MediaPlayState.COMPLETED
                mPlayIv!!.setImageResource(R.mipmap.ic_play)
                val padding = DensityUtil.dip2px(mContext, 15f)
                val paddingDiff = DensityUtil.dip2px(mContext, 2f)
                mPlayIv!!.setPadding(padding + paddingDiff, padding, padding - paddingDiff, padding)
                mAutoHide = false
                mHandler.removeCallbacks(mHideRunnable)
                show()
            }
            mMediaPlayer!!.setOnVideoSizeChangedListener { mp, width, height ->
                LogUtil.d(TAG, "MediaPlayer onVideoSizeChanged width = $width height = $height")
                if (isPlayable()) {
                    mCurrentProgressTv!!.text = TimeUtil.formatDuration(0)
                    mDurationTv!!.text = TimeUtil.formatDuration(mMediaPlayer!!.duration / 1000.toLong())
                    mProgressSb!!.progress = mMediaPlayer!!.currentPosition
                    mProgressSb!!.max = mMediaPlayer!!.duration
                }
                configSurfaceSize(width, height)
            }
            mMediaPlayer!!.setOnSeekCompleteListener { LogUtil.d(TAG, "MediaPlayer onSeekComplete ") }
            mMediaPlayer!!.setOnErrorListener { mp, what, extra ->
                LogUtil.e(TAG, "MediaPlayer onError")
                mPlayState = MediaPlayState.ERROR
                mp.reset()
                mPlayState = MediaPlayState.IDLE
                true
            }
            mMediaPlayer!!.setOnPreparedListener {
                LogUtil.e(TAG, "MediaPlayer onPrepared")
                mPlayState = MediaPlayState.PREPARED
                playMedia()
            }
        }
        setMediaPlayerSource()
        prepareMediaPlayer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 这个方法很重要
        val point = DensityUtil.getScreenRealSize(this)
        LogUtil.d(TAG, "onConfigurationChanged size = $point")
        if (mMediaPlayer!!.videoWidth > 0 && mMediaPlayer!!.videoHeight > 0) {
            configSurfaceSize(mMediaPlayer!!.videoWidth, mMediaPlayer!!.videoHeight)
        }
    }

    private fun configSurfaceSize(width: Int, height: Int) {
        val layoutParams = mSurfaceTv!!.layoutParams
        val point = DensityUtil.getScreenRealSize(this)
        val screenWidth = point.x
        val screenHeight = point.y
        if (screenWidth.toDouble() / width * height > screenHeight) {
            layoutParams.width = (screenHeight.toDouble() / height * width).toInt()
            layoutParams.height = screenHeight
        } else {
            layoutParams.width = screenWidth
            layoutParams.height = (screenWidth.toDouble() / width * height).toInt()
            LogUtil.d(TAG, "onConfigurationChanged width = " + layoutParams.width + " height = " + layoutParams.height)
        }
    }

    private fun setMediaPlayerSource() {
        try {
            mMediaPlayer!!.setDataSource(mMediaFilePath)
            mPlayState = MediaPlayState.INITIALIZED
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun prepareMediaPlayer() {
        mMediaPlayer!!.prepareAsync()
        mPlayState = MediaPlayState.PREPARING
    }

    private fun playMedia() {
        mPlayIv!!.setImageResource(R.mipmap.ic_pause)
        val padding = DensityUtil.dip2px(this, 15f)
        mPlayIv!!.setPadding(padding, padding, padding, padding)
        mMediaPlayer!!.start()
        mPlayState = MediaPlayState.STARTED
        mHandler.removeCallbacks(mUpdateProgressRunnable)
        mHandler.post(mUpdateProgressRunnable)
        mAutoHide = true
    }

    private fun pauseMedia() {
        mPlayIv!!.setImageResource(R.mipmap.ic_play)
        val padding = DensityUtil.dip2px(this, 15f)
        val paddingDiff = DensityUtil.dip2px(this, 2f)
        mPlayIv!!.setPadding(padding + paddingDiff, padding, padding - paddingDiff, padding)
        mMediaPlayer!!.pause()
        mPlayState = MediaPlayState.PAUSED
        mHandler.removeCallbacks(mUpdateProgressRunnable)
    }

    private fun stopMedia() {
        mMediaPlayer!!.stop()
        mPlayState = MediaPlayState.STOPPED
    }

    private fun releaseMedia() {
        if (mMediaPlayer != null) {
            if (MediaPlayState.STARTED == mPlayState || MediaPlayState.PAUSED == mPlayState) {
                stopMedia()
            }
            mMediaPlayer!!.release()
            mPlayState = MediaPlayState.END
            mMediaPlayer = null
        }
        mHandler.removeCallbacks(mUpdateProgressRunnable)
    }

    override fun onClick(v: View) {
        delayedHide(AUTO_HIDE_DELAY_MILLIS)
        when (v.id) {
            R.id.iv_play -> if (MediaPlayState.PAUSED == mPlayState || MediaPlayState.COMPLETED == mPlayState) {
                playMedia()
            } else if (MediaPlayState.STARTED == mPlayState) {
                pauseMedia()
            }
            R.id.iv_backward -> if (MediaPlayState.PAUSED == mPlayState || MediaPlayState.COMPLETED == mPlayState || MediaPlayState.STARTED == mPlayState) {
                val seekTo = mMediaPlayer!!.currentPosition - 10 * 1000
                if (seekTo > 0) {
                    mMediaPlayer!!.seekTo(seekTo)
                    mProgressSb!!.progress = seekTo
                }
            }
            R.id.iv_forward -> if (MediaPlayState.PAUSED == mPlayState || MediaPlayState.COMPLETED == mPlayState || MediaPlayState.STARTED == mPlayState) {
                val seekTo = mMediaPlayer!!.currentPosition + 10 * 1000
                if (seekTo < mMediaPlayer!!.duration) {
                    mMediaPlayer!!.seekTo(seekTo)
                    mProgressSb!!.progress = seekTo
                }
            }
        }
    }
}