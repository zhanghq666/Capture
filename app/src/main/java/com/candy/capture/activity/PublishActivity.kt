package com.candy.capture.activity

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.candy.capture.BR
import com.candy.capture.R
import com.candy.capture.core.BackStackManager
import com.candy.capture.core.ConstantValues
import com.candy.capture.core.SharedPreferenceManager
import com.candy.capture.customview.AudioView
import com.candy.capture.customview.ImageViewerDialog
import com.candy.capture.databinding.ActivityPublishBinding
import com.candy.capture.model.Content
import com.candy.capture.model.MediaPlayState
import com.candy.commonlibrary.utils.GlideImageLoader
import com.candy.capture.util.TimeUtils
import com.candy.capture.viewmodel.PublishViewModel
import com.candy.commonlibrary.utils.LogUtil
import com.candy.commonlibrary.utils.TipsUtil
import org.jetbrains.annotations.NotNull
import java.io.IOException
import java.util.*

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 10:53
 */
class PublishActivity: BaseActivity(), View.OnClickListener {
    companion object {
        private const val TAG = "PublishActivity"

        const val INTENT_KEY_TYPE = "type"
        const val INTENT_KEY_CONTENT = "content"
    }

    private var mDescEt: EditText? = null

    private var mAudioStub: ViewStub? = null
    private var mAudioView: AudioView? = null

    private var mPhotoStub: ViewStub? = null
    private var mImageView: ImageView? = null

    private var mVideoStub: ViewStub? = null
    private var mVideoCoverView: ImageView? = null
    private var mVideoPlaybackMask: ImageView? = null

    private var mType = 0
    private var mContent: Content? = null
    private val vm: PublishViewModel by lazy {
        PublishViewModel()
    }

    private var mMediaPlayer: MediaPlayer? = null
    private var mPlayState: MediaPlayState? = null

    private lateinit var binding: ActivityPublishBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        handleIntent()
        findView()
        setView()
    }

    private fun handleIntent() {
        mType = intent.getIntExtra(INTENT_KEY_TYPE, ConstantValues.CONTENT_TYPE_TEXT)
        mContent = intent.getSerializableExtra(INTENT_KEY_CONTENT) as Content?
        if (mContent == null) {
            mContent = Content()
            mContent!!.type = mType
        }

        binding.setVariable(BR.content, mContent)
    }

    private fun findView() {
        mDescEt = findViewById<View>(R.id.et_desc) as EditText
        mAudioStub = findViewById<View>(R.id.stub_audio) as ViewStub
        mPhotoStub = findViewById<View>(R.id.stub_photo) as ViewStub
        mVideoStub = findViewById<View>(R.id.stub_video) as ViewStub
    }

    private fun setView() {
        when (mType) {
            ConstantValues.CONTENT_TYPE_AUDIO -> {
                mAudioStub!!.inflate()
                mAudioView = findViewById<View>(R.id.audio_view) as AudioView
//                mAudioDurationTv = findViewById<View>(R.id.tv_duration) as TextView
                mAudioView!!.setDuration(mContent!!.mediaDuration)
                mAudioView!!.setOnClickListener(this)
//                mAudioDurationTv!!.text = TimeUtils.formatDuration(mContent!!.mediaDuration!!.toLong())
            }
            ConstantValues.CONTENT_TYPE_PHOTO -> {
                mPhotoStub!!.inflate()
                mImageView = findViewById<View>(R.id.image_view) as ImageView
                mImageView!!.setOnClickListener(this)
                GlideImageLoader.getInstance().loadImage(mContext, mContent!!.mediaFilePath, mImageView!!)
            }
            ConstantValues.CONTENT_TYPE_VIDEO -> {
                mVideoStub!!.inflate()
                mVideoCoverView = findViewById<View>(R.id.image_view) as ImageView
                mVideoPlaybackMask = findViewById<View>(R.id.iv_playback_mask) as ImageView
                mVideoPlaybackMask!!.setOnClickListener(this)
                GlideImageLoader.getInstance().loadVideoThumbnail(this, mContent!!.mediaFilePath, mVideoCoverView!!)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ConstantValues.CONTENT_TYPE_AUDIO == mType || ConstantValues.CONTENT_TYPE_VIDEO == mType) {
            initMediaPlayer()
        }
    }

    private fun initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer()
            mPlayState = MediaPlayState.IDLE
            mMediaPlayer!!.setOnCompletionListener {
                mPlayState = MediaPlayState.COMPLETED
                if (mAudioView != null) {
                    mAudioView!!.stopAnimation()
                }
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
        setMediaPlayerSource()
        prepareMediaPlayer()
    }

    private fun setMediaPlayerSource() {
        try {
            mMediaPlayer!!.setDataSource(mContent!!.mediaFilePath)
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

    override fun onPause() {
        super.onPause()
        releaseMedia()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_check, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_ok) {
            if (TextUtils.isEmpty(mDescEt!!.text.toString().trim { it <= ' ' })) {
                TipsUtil.showToast(mContext, "说点什么再保存吧")
            } else {
                if (insertToDB()) {
                    if (ConstantValues.CONTENT_TYPE_AUDIO == mType) {
                        BackStackManager.getInstance().removeSpecificActivityByClass(AudioRecordActivity::class.java)
                    }
                    setResult(RESULT_OK)
                    finish()
                } else {
                    TipsUtil.showToast(mContext, "保存失败")
                }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun insertToDB(): Boolean {
        mContent?.let {
            it.cityName = SharedPreferenceManager.getInstance(mContext).getLocationCity() ?: ""
            it.releaseTime = Date().time / 1000
            vm.saveContent(it)
        }
        return true
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.audio_view -> if (MediaPlayState.STARTED == mPlayState) {
                stopMedia()
            } else if (MediaPlayState.COMPLETED == mPlayState || MediaPlayState.PREPARED == mPlayState) {
                playMedia()
            } else if (MediaPlayState.STOPPED == mPlayState) {
                prepareMediaPlayer()
                playMedia()
            }
            R.id.image_view -> {
                val list = ArrayList<String>()
                list.add(mContent!!.mediaFilePath!!)
                ImageViewerDialog.showDialog(this, list, 1)
            }
            R.id.iv_playback_mask -> {
                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.EXTRA_MEDIA_PATH_KEY, mContent!!.mediaFilePath)
                startActivity(intent)
            }
        }
    }

    private fun playMedia() {
        mMediaPlayer!!.start()
        mPlayState = MediaPlayState.STARTED
        if (mAudioView != null) {
            mAudioView!!.startAnimation()
        }
    }

    private fun stopMedia() {
        mMediaPlayer!!.stop()
        mPlayState = MediaPlayState.STOPPED
        if (mAudioView != null) {
            mAudioView!!.stopAnimation()
        }
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
}