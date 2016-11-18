package com.candy.capture.activity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.candy.capture.R;
import com.candy.capture.core.BackStackManager;
import com.candy.capture.core.ConstantValues;
import com.candy.capture.core.DBHelper;
import com.candy.capture.core.SharedReferenceManager;
import com.candy.capture.customview.AudioView;
import com.candy.capture.model.Content;
import com.candy.capture.model.MediaPlayState;
import com.candy.capture.util.TimeUtil;
import com.candy.capture.util.TipsUtil;

import java.io.IOException;
import java.util.Date;

public class PublishActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "PublishActivity";

    public static final String INTENT_KEY_TYPE = "type";
    public static final String INTENT_KEY_CONTENT = "content";

    private EditText mDescEt;

    private ViewStub mAudioStub;
    private AudioView mAudioView;
    private TextView mAudioDurationTv;

    private ViewStub mPhotoStub;
    private ImageView mImageView;

    private int mType;
    private Content mContent;

    private MediaPlayer mMediaPlayer;
    private MediaPlayState mPlayState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        handleIntent();
        findView();
        setView();
    }

    private void handleIntent() {
        mType = getIntent().getIntExtra(INTENT_KEY_TYPE, ConstantValues.CONTENT_TYPE_TEXT);
        mContent = (Content) getIntent().getSerializableExtra(INTENT_KEY_CONTENT);
    }

    private void findView() {
        mDescEt = (EditText) findViewById(R.id.et_desc);
        mAudioStub = (ViewStub) findViewById(R.id.stub_audio);
    }

    private void setView() {
        switch (mType) {
            case ConstantValues.CONTENT_TYPE_AUDIO:
                mAudioStub.inflate();
                mAudioView = (AudioView) findViewById(R.id.audio_view);
                mAudioDurationTv = (TextView) findViewById(R.id.tv_duration);
                mAudioView.setDuration(mContent.getMediaDuration());
                mAudioView.setOnClickListener(this);
                mAudioDurationTv.setText(TimeUtil.formatDuration(mContent.getMediaDuration()));
                break;
            case ConstantValues.CONTENT_TYPE_PHOTO:
                break;
            case ConstantValues.CONTENT_TYPE_VIDEO:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ConstantValues.CONTENT_TYPE_AUDIO == mType || ConstantValues.CONTENT_TYPE_VIDEO == mType) {
            initMediaPlayer();
        }
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mPlayState = MediaPlayState.IDLE;
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mPlayState = MediaPlayState.COMPLETED;
                    if (mAudioView != null) {
                        mAudioView.stopAnimation();
                    }
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e(TAG, "MediaPlayer onError");
                    mPlayState = MediaPlayState.ERROR;

                    mp.reset();
                    mPlayState = MediaPlayState.IDLE;

                    initMediaPlayer();
                    return true;
                }
            });
        }
        setMediaPlayerSource();
        prepareMediaPlayer();
    }

    private void setMediaPlayerSource() {
        try {
            mMediaPlayer.setDataSource(mContent.getMediaFilePath());
            mPlayState = MediaPlayState.INITIALIZED;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareMediaPlayer() {
        try {
            mMediaPlayer.prepare();
            mPlayState = MediaPlayState.PREPARED;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMedia();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_check, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_ok) {
            if (TextUtils.isEmpty(mDescEt.getText().toString().trim())) {
                TipsUtil.showToast(mContext, "说点什么再保存吧");
            } else {
                if (insertToDB()) {
                    if (ConstantValues.CONTENT_TYPE_AUDIO == mType) {
                        BackStackManager.getInstance().removeSpecificActivityByClass(AudioRecordActivity.class);
                    }
                    setResult(RESULT_OK);
                    finish();
                } else {
                    TipsUtil.showToast(mContext, "保存失败");
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean insertToDB() {
        DBHelper dbHelper = new DBHelper(mContext);

        if (mContent == null) {
            mContent = new Content();
            mContent.setType(ConstantValues.CONTENT_TYPE_TEXT);
        }
        mContent.setDesc(mDescEt.getText().toString().trim());
        mContent.setCityName(SharedReferenceManager.getInstance(mContext).getLocationCity());
        mContent.setReleaseTime((int) (new Date().getTime() / 1000));
        return dbHelper.insertContent(mContent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.audio_view:
                if (MediaPlayState.STARTED == mPlayState) {
                    stopMedia();
                } else if (MediaPlayState.COMPLETED == mPlayState || MediaPlayState.PREPARED == mPlayState) {
                    playMedia();
                } else if (MediaPlayState.STOPPED == mPlayState) {
                    prepareMediaPlayer();
                    playMedia();
                }
                break;
        }
    }

    private void playMedia() {
        mMediaPlayer.start();
        mPlayState = MediaPlayState.STARTED;

        if (mAudioView != null) {
            mAudioView.startAnimation();
        }
    }

    private void stopMedia() {
        mMediaPlayer.stop();
        mPlayState = MediaPlayState.STOPPED;

        if (mAudioView != null) {
            mAudioView.stopAnimation();
        }
    }

    private void releaseMedia() {
        if (mMediaPlayer != null) {
            if (MediaPlayState.STARTED == mPlayState) {
                stopMedia();
            }
            mMediaPlayer.release();
            mPlayState = MediaPlayState.END;
            mMediaPlayer = null;
        }
    }
}
