package com.candy.capture.activity;

import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.candy.capture.R;
import com.candy.capture.core.ConstantValues;
import com.candy.capture.customview.AudioIndicators;
import com.candy.capture.model.Content;
import com.candy.capture.util.FileUtil;

import java.io.IOException;

public class AudioRecordActivity extends BaseActivity {

    private static final double REFERENCE = 0.00002;

    private MediaRecorder mMediaRecorder;
    private String mSourceFilePath;
    private boolean mIsRecording;
    private NoiseLevelSubscriber mNoiseLevelSubscriber;

    private NoiseHandler mNoiseHandler = new NoiseHandler();

    private RelativeLayout mRecordLayout;
    private TextView mRecordTipTv;
    private TextView mRecordDurationTv;
    private AudioIndicators mAudioIndicators;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findView();
        setView();
        initRecorder();
    }

    private void findView() {
        mRecordLayout = (RelativeLayout) findViewById(R.id.rl_record);
        mRecordTipTv = (TextView) findViewById(R.id.tv_record_tips);
        mRecordDurationTv = (TextView) findViewById(R.id.tv_record_duration);
        mAudioIndicators = (AudioIndicators) findViewById(R.id.audio_indicators);
    }

    private void setView() {
        mRecordLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsRecording) {
                    // 停止
                    stopRecord();
                    gotoPublish();
                } else {
                    // 开始
                    startRecord();
                }
            }
        });
    }

    private void initRecorder() {
        mMediaRecorder = new MediaRecorder();
    }

    private void startRecord() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mIsRecording = true;
        mRecordTipTv.setText("完成录音");
        mRecordDurationTv.setText("");

        if (!TextUtils.isEmpty(mSourceFilePath)) {
            FileUtil.deleteFile(mSourceFilePath);
        }
        mSourceFilePath = FileUtil.getMediaFilePath(this, FileUtil.MEDIA_TYPE_AUDIO);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setOutputFile(mSourceFilePath);
//        mMediaRecorder.setMaxDuration(1000 * 60 * 60 * 1);//1小时
//        mMediaRecorder.setMaxFileSize(1024 * 1024 * 500);//500MB
        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();   // Recording is now started

            mAudioIndicators.turnOn();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mNoiseHandler.setSeconds(0);
        mNoiseLevelSubscriber = new NoiseLevelSubscriber();
        mNoiseLevelSubscriber.start();
    }

    private void stopRecord() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mIsRecording = false;
        mRecordTipTv.setText("重新录音");

        mNoiseLevelSubscriber.interrupt();
        mNoiseLevelSubscriber.isRunning = false;

        try {
            mMediaRecorder.stop();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMediaRecorder.reset();   // You can reuse the object by going back to setAudioSource() step

        mNoiseHandler.removeCallbacksAndMessages(null);
        mAudioIndicators.stop();
    }

    private void gotoPublish() {
        Content content = new Content();
        content.setType(ConstantValues.CONTENT_TYPE_AUDIO);
        content.setMediaFilePath(mSourceFilePath);
        content.setMediaDuration(mNoiseHandler.getSeconds());
        Intent intent = new Intent(this, PublishActivity.class);
        intent.putExtra(PublishActivity.INTENT_KEY_TYPE, ConstantValues.CONTENT_TYPE_AUDIO);
        intent.putExtra(PublishActivity.INTENT_KEY_CONTENT, content);
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaRecorder != null && mIsRecording) {
            stopRecord();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMediaRecorder != null) {
            mMediaRecorder.release(); // Now the object cannot be reused
        }
    }

    class NoiseHandler extends Handler {
        private int seconds;

        public int getSeconds() {
            return seconds;
        }

        public void setSeconds(int seconds) {
            this.seconds = seconds;
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            double level = bundle.getDouble("level");
            mAudioIndicators.setLevel((int) level);
            seconds++;

            String text = (seconds / 60) + "\'" + (seconds % 60) + "\"";
            mRecordDurationTv.setText(text);
        }
    }

    class NoiseLevelSubscriber extends Thread {
        private double level = 0.0;
        private boolean isRunning = false;
        private int counter;

        @Override
        public void run() {
            super.run();

            isRunning = true;
            while (isRunning) {
                level = level + getNoiseLevel();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                counter++;
                if (counter >= 5 && isRunning) {
                    level = level / counter;
                    counter = 0;
                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putDouble("level", level);
                    message.setData(bundle);
                    mNoiseHandler.sendMessage(message);
                }
            }

            level = 0.0;
            counter = 0;
        }

        private double getNoiseLevel() {
            double db = 0;
            try {
                db = (20 * Math.log10(mMediaRecorder.getMaxAmplitude() / REFERENCE));
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            if (db > 0) {
                return db;
            } else {
                return 0;
            }
        }
    }
}
