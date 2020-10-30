package com.candy.capture.activity

import android.content.Intent
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.candy.capture.R
import com.candy.capture.core.ConstantValues
import com.candy.capture.customview.AudioIndicators
import com.candy.capture.model.Content
import com.candy.capture.util.FileUtil
import java.io.IOException

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 11:04
 */
class AudioRecordActivity : BaseActivity() {
    companion object {
        private const val REFERENCE = 0.00002
    }

    private lateinit var mMediaRecorder: MediaRecorder
    private var mSourceFilePath: String? = null
    private var mIsRecording = false
    private var mNoiseLevelSubscriber: NoiseLevelSubscriber? = null

    private val mNoiseHandler = NoiseHandler()

    private var mRecordLayout: RelativeLayout? = null
    private var mRecordTipTv: TextView? = null
    private var mRecordDurationTv: TextView? = null
    private var mAudioIndicators: AudioIndicators? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        findView()
        setView()
        initRecorder()
    }

    private fun findView() {
        mRecordLayout = findViewById<View>(R.id.rl_record) as RelativeLayout
        mRecordTipTv = findViewById<View>(R.id.tv_record_tips) as TextView
        mRecordDurationTv = findViewById<View>(R.id.tv_record_duration) as TextView
        mAudioIndicators = findViewById<View>(R.id.audio_indicators) as AudioIndicators
    }

    private fun setView() {
        mRecordLayout!!.setOnClickListener {
            if (mIsRecording) {
                // 停止
                stopRecord()
                gotoPublish()
            } else {
                // 开始
                startRecord()
            }
        }
    }

    private fun initRecorder() {
        mMediaRecorder = MediaRecorder()
    }

    private fun startRecord() {
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mIsRecording = true
        mRecordTipTv!!.text = "完成录音"
        mRecordDurationTv!!.text = ""
        if (!TextUtils.isEmpty(mSourceFilePath)) {
            FileUtil.deleteFile(mSourceFilePath)
        }
        mSourceFilePath = FileUtil.getMediaFilePath(this, FileUtil.MEDIA_TYPE_AUDIO)
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mMediaRecorder.setOutputFile(mSourceFilePath)
        //        mMediaRecorder.setMaxDuration(1000 * 60 * 60 * 1);//1小时
//        mMediaRecorder.setMaxFileSize(1024 * 1024 * 500);//500MB
        try {
            mMediaRecorder.prepare()
            mMediaRecorder.start() // Recording is now started
            mAudioIndicators!!.turnOn()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mNoiseHandler.seconds = 0
        mNoiseLevelSubscriber = NoiseLevelSubscriber()
        mNoiseLevelSubscriber!!.start()
    }

    private fun stopRecord() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mIsRecording = false
        mRecordTipTv!!.text = "重新录音"
        mNoiseLevelSubscriber!!.interrupt()
        mNoiseLevelSubscriber!!.isRunning = false
        try {
            mMediaRecorder.stop()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mMediaRecorder.reset() // You can reuse the object by going back to setAudioSource() step
        mNoiseHandler.removeCallbacksAndMessages(null)
        mAudioIndicators!!.stop()
    }

    private fun gotoPublish() {
        val content = Content()
        content.type = ConstantValues.CONTENT_TYPE_AUDIO
        content.mediaFilePath = mSourceFilePath ?: ""
        content.mediaDuration = mNoiseHandler.seconds
        val intent = Intent(this, PublishActivity::class.java)
        intent.putExtra(PublishActivity.INTENT_KEY_TYPE, ConstantValues.CONTENT_TYPE_AUDIO)
        intent.putExtra(PublishActivity.INTENT_KEY_CONTENT, content)
        intent.flags = Intent.FLAG_ACTIVITY_FORWARD_RESULT
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        if (mIsRecording) {
            stopRecord()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mMediaRecorder.release() // Now the object cannot be reused
    }

    internal inner class NoiseHandler : Handler() {
        var seconds = 0

        override fun handleMessage(msg: Message) {
            val bundle = msg.data
            val level = bundle.getDouble("level")
            mAudioIndicators?.setLevel(level.toInt())
            seconds++
            val text = (seconds / 60).toString() + "\'" + seconds % 60 + "\""
            mRecordDurationTv?.text = text
        }
    }

    internal inner class NoiseLevelSubscriber : Thread() {
        private var level = 0.0
        var isRunning = false
        private var counter = 0
        override fun run() {
            super.run()
            isRunning = true
            while (isRunning) {
                level = level + noiseLevel
                try {
                    sleep(200)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                counter++
                if (counter >= 5 && isRunning) {
                    level = level / counter
                    counter = 0
                    val message = Message()
                    val bundle = Bundle()
                    bundle.putDouble("level", level)
                    message.data = bundle
                    mNoiseHandler.sendMessage(message)
                }
            }
            level = 0.0
            counter = 0
        }

        private val noiseLevel: Double
            private get() {
                var db = 0.0
                try {
                    db = 20 * Math.log10(mMediaRecorder.getMaxAmplitude() / REFERENCE)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
                return if (db > 0) db else 0.0
            }
    }
}