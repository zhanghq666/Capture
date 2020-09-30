package com.candy.capture.activity;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import com.candy.capture.R;
import com.candy.capture.model.MediaPlayState;
import com.candy.capture.util.TimeUtil;
import com.candy.commonlibrary.utils.DensityUtil;
import com.candy.commonlibrary.utils.LogUtil;
import com.candy.commonlibrary.utils.TipsUtil;
import com.candy.videoplayer.AliVideoView;

import java.io.IOException;

public class AliPlayerActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "VideoPlayerActivity";

    public static final String EXTRA_MEDIA_PATH_KEY = "extra_media_path";

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private boolean mAutoHide = true;

    /**
     * If {@link #mAutoHide} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private final Handler mHandler = new Handler();
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mAliVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mProgressView.setVisibility(View.VISIBLE);
            mControllerView.setVisibility(View.VISIBLE);

            delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private final Runnable mUpdateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mAliVideoView != null /*&& isPlayable()*/) {
                mProgressSb.setProgress((int) mAliVideoView.getCurrentPosition());

                mHandler.removeCallbacks(mUpdateProgressRunnable);
                mHandler.postDelayed(mUpdateProgressRunnable, 100);
            }
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (mAutoHide) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private AliVideoView mAliVideoView;
    private View mProgressView;
    private View mControllerView;
    private boolean mControllerVisible;

    private ImageView mPlayIv;
    private ImageView mBackwardIv;
    private ImageView mForwardIv;

    private SeekBar mProgressSb;
    private TextView mCurrentProgressTv;
    private TextView mDurationTv;

//    private MediaPlayer mMediaPlayer;
//    private MediaPlayState mPlayState;
    private String mMediaFilePath;

//    private TimeCountThread mTimeCountThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 导航栏、状态栏透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorTranslucence));
            window.setNavigationBarColor(getResources().getColor(R.color.colorTranslucence));

            if (getSupportActionBar() != null) {
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }

        setContentView(R.layout.activity_ali_player);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mControllerVisible = true;


        handleIntent();
        findView();
        setView();

        initMediaPlayer();
    }

    private void handleIntent() {
        mMediaFilePath = getIntent().getStringExtra(EXTRA_MEDIA_PATH_KEY);
        if (TextUtils.isEmpty(mMediaFilePath)) {
            TipsUtil.showToast(this, "源文件位置未知");
            finish();
        }
    }

    private void findView() {
        mProgressView = findViewById(R.id.rl_progress);
        mControllerView = findViewById(R.id.rl_controller);
        mAliVideoView = findViewById(R.id.aliVideoView);
        mPlayIv = (ImageView) findViewById(R.id.iv_play);
        mBackwardIv = (ImageView) findViewById(R.id.iv_backward);
        mForwardIv = (ImageView) findViewById(R.id.iv_forward);
        mProgressSb = (SeekBar) findViewById(R.id.seek_bar);
        mCurrentProgressTv = (TextView) findViewById(R.id.tv_current_progress);
        mDurationTv = (TextView) findViewById(R.id.tv_duration);
    }

    private void setView() {
        mAliVideoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        mPlayIv.setOnClickListener(this);
        mBackwardIv.setOnClickListener(this);
        mForwardIv.setOnClickListener(this);

        mProgressSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mAliVideoView.seekTo(seekBar.getProgress());

                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
                mCurrentProgressTv.setText(TimeUtil.formatDuration(mAliVideoView.getCurrentPosition() / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

//        if (MediaPlayState.STARTED == mPlayState) {
            pauseMedia();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        releaseMedia();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
//            NavUtils.navigateUpFromSameTask(this);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (mControllerVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mProgressView.setVisibility(View.GONE);
        mControllerView.setVisibility(View.GONE);
        mControllerVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHandler.removeCallbacks(mShowPart2Runnable);
        mHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mAliVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mControllerVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHandler.removeCallbacks(mHidePart2Runnable);
        mHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        if (mAutoHide) {
            mHandler.removeCallbacks(mHideRunnable);
            mHandler.postDelayed(mHideRunnable, delayMillis);
        }
    }

//    private boolean isPlayable() {
//        return mPlayState == MediaPlayState.PREPARED || mPlayState == MediaPlayState.STARTED || mPlayState == MediaPlayState.PAUSED;
//    }

    private void initMediaPlayer() {
        LogUtil.d(TAG, "initMediaPlayer");
//        if (mMediaPlayer == null) {
//            mMediaPlayer = new MediaPlayer();
//            mPlayState = MediaPlayState.IDLE;
//            mMediaPlayer.setScreenOnWhilePlaying(true);
        mAliVideoView.setOnCompletionListener(new AliVideoView.OnCompletionListener() {
                @Override
                public void onCompletion() {
//                    mPlayState = MediaPlayState.COMPLETED;

                    mPlayIv.setImageResource(R.mipmap.ic_play);
                    int padding = DensityUtil.dip2px(mContext, 15);
                    int paddingDiff = DensityUtil.dip2px(mContext, 2);
                    mPlayIv.setPadding(padding + paddingDiff, padding, padding - paddingDiff, padding);

                    mAutoHide = false;
                    mHandler.removeCallbacks(mHideRunnable);
                    show();
                }
            });
        mAliVideoView.setOnVideoSizeChangedListener(new AliVideoView.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(int width, int height) {
//                    if (isPlayable()) {
                        mCurrentProgressTv.setText(TimeUtil.formatDuration(0));
                        mDurationTv.setText(TimeUtil.formatDuration(mAliVideoView.getDuration() / 1000));
                        mProgressSb.setProgress((int) mAliVideoView.getCurrentPosition());
                        mProgressSb.setMax((int) mAliVideoView.getDuration());
//                    }

                }
            });
//            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
//                @Override
//                public void onSeekComplete(MediaPlayer mp) {
//                    LogUtil.d(TAG, "MediaPlayer onSeekComplete ");
//                }
//            });
        mAliVideoView.setOnErrorListener(new AliVideoView.OnErrorListener() {
                @Override
                public void onError(int what, String msg) {
//                    mPlayState = MediaPlayState.ERROR;
//
//                    mp.reset();
//                    mPlayState = MediaPlayState.IDLE;
//
//                    return true;
                }
            });
//            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                @Override
//                public void onPrepared(MediaPlayer mp) {
//                    LogUtil.e(TAG, "MediaPlayer onPrepared");
//                    mPlayState = MediaPlayState.PREPARED;
//                    playMedia();
//                }
//            });
//        }
        setMediaPlayerSource();
        prepareMediaPlayer();
    }

    private void setMediaPlayerSource() {
//        try {
            mMediaFilePath = "http://oss.littlehotspot.com/media/resource/h8YcE7debZ.mp4";
            mAliVideoView.setMediaSource(mMediaFilePath);
        mHandler.post(mUpdateProgressRunnable);
//            mPlayState = MediaPlayState.INITIALIZED;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void prepareMediaPlayer() {
//        mMediaPlayer.prepareAsync();
//        mPlayState = MediaPlayState.PREPARING;
    }

    private void playMedia() {
        mPlayIv.setImageResource(R.mipmap.ic_pause);
        int padding = DensityUtil.dip2px(this, 15);
        mPlayIv.setPadding(padding, padding, padding, padding);

        if (mAliVideoView.isPaused()) {
            mAliVideoView.start();
        } else if (mAliVideoView.isCompleted()) {
            setMediaPlayerSource();
        }
//        mPlayState = MediaPlayState.STARTED;

        mHandler.removeCallbacks(mUpdateProgressRunnable);
        mHandler.post(mUpdateProgressRunnable);
//        if (mTimeCountThread == null || mTimeCountThread.mIsClosed) {
//            mTimeCountThread = new TimeCountThread();
//        }
//        if (mTimeCountThread.mIsPaused) {
//            mTimeCountThread.resumeThread();
//        } else {
//            mTimeCountThread.start();
//        }

        mAutoHide = true;
    }

    private void pauseMedia() {
        mPlayIv.setImageResource(R.mipmap.ic_play);
        int padding = DensityUtil.dip2px(this, 15);
        int paddingDiff = DensityUtil.dip2px(this, 2);
        mPlayIv.setPadding(padding + paddingDiff, padding, padding - paddingDiff, padding);

        mAliVideoView.pause();
//        mPlayState = MediaPlayState.PAUSED;

        mHandler.removeCallbacks(mUpdateProgressRunnable);
//        if (mTimeCountThread != null) {
//            mTimeCountThread.pauseThread();
//        }
    }

    private void stopMedia() {
        mAliVideoView.stop();
//        mPlayState = MediaPlayState.STOPPED;

    }

    private void releaseMedia() {
//        if (mMediaPlayer != null) {
//            if (MediaPlayState.STARTED == mPlayState || MediaPlayState.PAUSED == mPlayState) {
//                stopMedia();
//            }
            mAliVideoView.release();
//            mPlayState = MediaPlayState.END;
//            mMediaPlayer = null;
//        }

        mHandler.removeCallbacks(mUpdateProgressRunnable);
//        if (mTimeCountThread != null) {
//            mTimeCountThread.closeThread();
//        }
    }

    @Override
    public void onClick(View v) {
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
        switch (v.getId()) {
            case R.id.iv_play:
                if (mAliVideoView.isPaused() || mAliVideoView.isCompleted()) {
                    playMedia();
                } else if (mAliVideoView.isPlaying()) {
                    pauseMedia();
                }
                break;
            case R.id.iv_backward:
//                if (MediaPlayState.PAUSED == mPlayState || MediaPlayState.COMPLETED == mPlayState || MediaPlayState.STARTED == mPlayState) {
                    long seekTo = mAliVideoView.getCurrentPosition() - 10 * 1000;
                    if (seekTo > 0) {
                        mAliVideoView.seekTo(seekTo);
                        mProgressSb.setProgress((int) seekTo);
                    }
//                }
                break;
            case R.id.iv_forward:
//                if (MediaPlayState.PAUSED == mPlayState || MediaPlayState.COMPLETED == mPlayState || MediaPlayState.STARTED == mPlayState) {
                    long seekTo1 = mAliVideoView.getCurrentPosition() + 10 * 1000;
                    if (seekTo1 < mAliVideoView.getDuration()) {
                        mAliVideoView.seekTo(seekTo1);
                        mProgressSb.setProgress((int) seekTo1);
                    }
//                }
                break;
        }
    }

//    class TimeCountThread extends Thread {
//
//        private boolean mIsPaused;
//        private boolean mIsClosed;
//
//        public synchronized void pauseThread() {
//            mIsPaused = true;
//        }
//
//        public synchronized void resumeThread() {
//            mIsPaused = false;
//            this.notify();
//        }
//
//        public synchronized void closeThread() {
//            try {
//                notify();
//                mIsClosed = true;
//                interrupt();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        public void run() {
//            while (!mIsClosed && !isInterrupted()) {
//                if (!mIsPaused) {
//                    // 有bug，播放结束后这里仍会不停执行
//                    mHideHandler.post(mUpdateProgressRunnable);
//                    try {
//                        Thread.sleep(100);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                } else {
//                    try {
//                        synchronized (this) {
//                            wait();
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//    }
}
