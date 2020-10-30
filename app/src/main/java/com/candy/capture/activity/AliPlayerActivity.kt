package com.candy.capture.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.candy.capture.R
import com.candy.capture.util.TimeUtils
import com.candy.commonlibrary.utils.DensityUtil
import com.candy.commonlibrary.utils.LogUtil
import com.candy.commonlibrary.utils.TipsUtil
import com.candy.videoplayer.AliVideoView

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:05
 */
class AliPlayerActivity: BaseActivity(), View.OnClickListener {

    companion object {
        private const val TAG = "AliPlayerActivity"

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
        mAliVideoView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
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
            if (mAliVideoView != null /*&& isPlayable()*/) {
                mProgressSb!!.progress = mAliVideoView!!.getCurrentPosition().toInt()
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

    private var mAliVideoView: AliVideoView? = null
    private var mProgressView: View? = null
    private var mControllerView: View? = null
    private var mControllerVisible = false

    private var mPlayIv: ImageView? = null
    private var mBackwardIv: ImageView? = null
    private var mForwardIv: ImageView? = null

    private var mProgressSb: SeekBar? = null
    private var mCurrentProgressTv: TextView? = null
    private var mDurationTv: TextView? = null

    private var mMediaFilePath: String? = null

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
        setContentView(R.layout.activity_ali_player)
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
        mAliVideoView = findViewById(R.id.aliVideoView)
        mPlayIv = findViewById<View>(R.id.iv_play) as ImageView
        mBackwardIv = findViewById<View>(R.id.iv_backward) as ImageView
        mForwardIv = findViewById<View>(R.id.iv_forward) as ImageView
        mProgressSb = findViewById<View>(R.id.seek_bar) as SeekBar
        mCurrentProgressTv = findViewById<View>(R.id.tv_current_progress) as TextView
        mDurationTv = findViewById<View>(R.id.tv_duration) as TextView
    }

    private fun setView() {
        mAliVideoView!!.setOnClickListener { toggle() }
        mPlayIv!!.setOnClickListener(this)
        mBackwardIv!!.setOnClickListener(this)
        mForwardIv!!.setOnClickListener(this)
        mProgressSb!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mAliVideoView!!.seekTo(seekBar.progress.toLong())
                    delayedHide(AUTO_HIDE_DELAY_MILLIS)
                }
                mCurrentProgressTv!!.text = TimeUtils.formatDuration(mAliVideoView!!.getCurrentPosition() / 1000)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    override fun onPause() {
        super.onPause()

        pauseMedia()
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
        mAliVideoView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
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

    private fun initMediaPlayer() {
        LogUtil.d(TAG, "initMediaPlayer")
        mAliVideoView!!.setOnCompletionListener(object: AliVideoView.OnCompletionListener{
            override fun onCompletion() {
                mPlayIv!!.setImageResource(R.mipmap.ic_play)
                val padding = DensityUtil.dip2px(mContext, 15f)
                val paddingDiff = DensityUtil.dip2px(mContext, 2f)
                mPlayIv!!.setPadding(padding + paddingDiff, padding, padding - paddingDiff, padding)
                mAutoHide = false
                mHandler.removeCallbacks(mHideRunnable)
                show()
            }
        })
        mAliVideoView!!.setOnVideoSizeChangedListener(object: AliVideoView.OnVideoSizeChangedListener {
            override fun onVideoSizeChanged(width: Int, height: Int) {
                mCurrentProgressTv!!.text = TimeUtils.formatDuration(0)
                mDurationTv!!.text = TimeUtils.formatDuration(mAliVideoView!!.getDuration() / 1000)
                mProgressSb!!.progress = mAliVideoView!!.getCurrentPosition().toInt()
                mProgressSb!!.max = mAliVideoView!!.getDuration().toInt()
            }
        })
        mAliVideoView!!.setOnErrorListener(object: AliVideoView.OnErrorListener {
            override fun onError(code: Int, msg: String?) {
            }
        })

        setMediaPlayerSource()
        prepareMediaPlayer()
    }

    private fun setMediaPlayerSource() {
//        mMediaFilePath = "http://oss.littlehotspot.com/media/resource/h8YcE7debZ.mp4"
        mMediaFilePath = "http://player.alicdn.com/video/aliyunmedia.mp4"
        mAliVideoView!!.setMediaSource(mMediaFilePath)
        mHandler.post(mUpdateProgressRunnable)
    }

    private fun prepareMediaPlayer() {
    }

    private fun playMedia() {
        mPlayIv!!.setImageResource(R.mipmap.ic_pause)
        val padding = DensityUtil.dip2px(this, 15f)
        mPlayIv!!.setPadding(padding, padding, padding, padding)
        if (mAliVideoView!!.isPaused()) {
            mAliVideoView!!.start()
        } else if (mAliVideoView!!.isCompleted()) {
            setMediaPlayerSource()
        }
        mHandler.removeCallbacks(mUpdateProgressRunnable)
        mHandler.post(mUpdateProgressRunnable)
        mAutoHide = true
    }

    private fun pauseMedia() {
        mPlayIv!!.setImageResource(R.mipmap.ic_play)
        val padding = DensityUtil.dip2px(this, 15f)
        val paddingDiff = DensityUtil.dip2px(this, 2f)
        mPlayIv!!.setPadding(padding + paddingDiff, padding, padding - paddingDiff, padding)
        mAliVideoView!!.pause()
        mHandler.removeCallbacks(mUpdateProgressRunnable)
    }

    private fun stopMedia() {
        mAliVideoView!!.stop()
    }

    private fun releaseMedia() {
        mAliVideoView!!.release()
        mHandler.removeCallbacks(mUpdateProgressRunnable)
    }

    override fun onClick(v: View) {
        delayedHide(AUTO_HIDE_DELAY_MILLIS)
        when (v.id) {
            R.id.iv_play -> if (mAliVideoView!!.isPaused() || mAliVideoView!!.isCompleted()) {
                playMedia()
            } else if (mAliVideoView!!.isPlaying()) {
                pauseMedia()
            }
            R.id.iv_backward -> {
                val seekTo = mAliVideoView!!.getCurrentPosition() - 10 * 1000
                if (seekTo > 0) {
                    mAliVideoView!!.seekTo(seekTo)
                    mProgressSb!!.progress = seekTo.toInt()
                }
            }
            R.id.iv_forward -> {
                val seekTo1 = mAliVideoView!!.getCurrentPosition() + 10 * 1000
                if (seekTo1 < mAliVideoView!!.getDuration()) {
                    mAliVideoView!!.seekTo(seekTo1)
                    mProgressSb!!.progress = seekTo1.toInt()
                }
            }
        }
    }
}