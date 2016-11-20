package com.candy.capture.activity;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.candy.capture.R;
import com.candy.capture.model.MediaPlayState;
import com.candy.capture.util.DensityUtil;
import com.candy.capture.util.TimeUtil;
import com.candy.capture.util.TipsUtil;

import java.io.IOException;

public class VideoPlayerActivity extends BaseActivity implements View.OnClickListener {
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

    private final Handler mHideHandler = new Handler();
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mSurfaceTv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
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
            if (mMediaPlayer != null) {
                mProgressSb.setProgress(mMediaPlayer.getCurrentPosition());
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

    private TextureView mSurfaceTv;
    private View mProgressView;
    private View mControllerView;
    private boolean mVisible;

    private ImageView mPlayIv;
    private ImageView mBackwardIv;
    private ImageView mForwardIv;

    private SeekBar mProgressSb;
    private TextView mCurrentProgressTv;
    private TextView mDurationTv;

    private MediaPlayer mMediaPlayer;
    private MediaPlayState mPlayState;
    private String mMediaFilePath;

    private TimeCountThread mTimeCountThread;

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

        setContentView(R.layout.activity_video_player);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mVisible = true;


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
        mSurfaceTv = (TextureView) findViewById(R.id.tv_surface);
        mPlayIv = (ImageView) findViewById(R.id.iv_play);
        mBackwardIv = (ImageView) findViewById(R.id.iv_backward);
        mForwardIv = (ImageView) findViewById(R.id.iv_forward);
        mProgressSb = (SeekBar) findViewById(R.id.seek_bar);
        mCurrentProgressTv = (TextView) findViewById(R.id.tv_current_progress);
        mDurationTv = (TextView) findViewById(R.id.tv_duration);
    }

    private void setView() {
        mSurfaceTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        mSurfaceTv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureAvailable");
                if (mMediaPlayer == null || mPlayState == MediaPlayState.IDLE) {
                    initMediaPlayer();
                }
                mMediaPlayer.setSurface(new Surface(surface));
                playMedia();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.d(TAG, "onSurfaceTextureSizeChanged width = " + width + " height = " + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG, "onSurfaceTextureDestroyed");
                releaseMedia();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        mPlayIv.setOnClickListener(this);
        mBackwardIv.setOnClickListener(this);
        mForwardIv.setOnClickListener(this);

        mProgressSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mMediaPlayer.seekTo(seekBar.getProgress());

                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
                mCurrentProgressTv.setText(TimeUtil.formatDuration(mMediaPlayer.getCurrentPosition() / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        initMediaPlayer();
//    }

    @Override
    protected void onPause() {
        super.onPause();

        if (MediaPlayState.STARTED == mPlayState) {
            pauseMedia();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        releaseMedia();
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
        if (mVisible) {
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
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mSurfaceTv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        if (mAutoHide) {
            mHideHandler.removeCallbacks(mHideRunnable);
            mHideHandler.postDelayed(mHideRunnable, delayMillis);
        }
    }


    private void initMediaPlayer() {
        Log.d(TAG, "initMediaPlayer");
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mPlayState = MediaPlayState.IDLE;
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mPlayState = MediaPlayState.COMPLETED;

                    mPlayIv.setImageResource(R.mipmap.ic_play);
                    int padding = DensityUtil.dip2px(mContext, 15);
                    int paddingDiff = DensityUtil.dip2px(mContext, 2);
                    mPlayIv.setPadding(padding + paddingDiff, padding, padding - paddingDiff, padding);

                    mAutoHide = false;
                    mHideHandler.removeCallbacks(mHideRunnable);
                    show();
                }
            });
            mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    Log.d(TAG, "MediaPlayer onVideoSizeChanged width = " + width + " height = " + height);

                    mCurrentProgressTv.setText(TimeUtil.formatDuration(0));
                    mDurationTv.setText(TimeUtil.formatDuration(mMediaPlayer.getDuration() / 1000));
                    mProgressSb.setProgress(0);
                    mProgressSb.setMax(mMediaPlayer.getDuration());

                    configSurfaceSize(width, height);
                }
            });
            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    Log.d(TAG, "MediaPlayer onSeekComplete ");
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 这个方法很重要
        Point point = DensityUtil.getScreenRealSize(this);
        Log.d(TAG, "onConfigurationChanged size = " + point);

        if (mMediaPlayer.getVideoWidth() > 0 && mMediaPlayer.getVideoHeight() > 0) {
            configSurfaceSize(mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
        }
    }

    private void configSurfaceSize(int width, int height) {
        ViewGroup.LayoutParams layoutParams = mSurfaceTv.getLayoutParams();
        Point point = DensityUtil.getScreenRealSize(this);
        int screenWidth = point.x;
        int screenHeight = point.y;
//        int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
//        int screenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        if (((double) screenWidth / width) * height > screenHeight) {
            layoutParams.width = (int) (((double) screenHeight / height) * width);
            layoutParams.height = screenHeight;
        } else {
            layoutParams.width = screenWidth;
            layoutParams.height = (int) (((double) screenWidth / width) * height);
            Log.d(TAG, "onConfigurationChanged width = " + layoutParams.width + " height = " + layoutParams.height);
        }
    }

    private void setMediaPlayerSource() {
        try {
            mMediaPlayer.setDataSource(mMediaFilePath);
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

    private void playMedia() {
        mPlayIv.setImageResource(R.mipmap.ic_pause);
        int padding = DensityUtil.dip2px(this, 15);
        mPlayIv.setPadding(padding, padding, padding, padding);

        mMediaPlayer.start();
        mPlayState = MediaPlayState.STARTED;

        if (mTimeCountThread == null || mTimeCountThread.mIsClosed) {
            mTimeCountThread = new TimeCountThread();
        }
        if (mTimeCountThread.mIsPaused) {
            mTimeCountThread.resumeThread();
        } else {
            mTimeCountThread.start();
        }

        mAutoHide = true;
    }

    private void pauseMedia() {
        mPlayIv.setImageResource(R.mipmap.ic_play);
        int padding = DensityUtil.dip2px(this, 15);
        int paddingDiff = DensityUtil.dip2px(this, 2);
        mPlayIv.setPadding(padding + paddingDiff, padding, padding - paddingDiff, padding);

        mMediaPlayer.pause();
        mPlayState = MediaPlayState.PAUSED;

        if (mTimeCountThread != null) {
            mTimeCountThread.pauseThread();
        }
    }

    private void stopMedia() {
        mMediaPlayer.stop();
        mPlayState = MediaPlayState.STOPPED;

    }

    private void releaseMedia() {
        if (mMediaPlayer != null) {
            if (MediaPlayState.STARTED == mPlayState || MediaPlayState.PAUSED == mPlayState) {
                stopMedia();
            }
            mMediaPlayer.release();
            mPlayState = MediaPlayState.END;
            mMediaPlayer = null;
        }

        if (mTimeCountThread != null) {
            mTimeCountThread.closeThread();
        }
    }

    @Override
    public void onClick(View v) {
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
        switch (v.getId()) {
            case R.id.iv_play:
                if (MediaPlayState.PAUSED == mPlayState || MediaPlayState.COMPLETED == mPlayState) {
                    playMedia();
                } else if (MediaPlayState.STARTED == mPlayState) {
                    pauseMedia();
                }
                break;
            case R.id.iv_backward:
                if (MediaPlayState.PAUSED == mPlayState || MediaPlayState.COMPLETED == mPlayState || MediaPlayState.STARTED == mPlayState) {
                    int seekTo = mMediaPlayer.getCurrentPosition() - 10 * 1000;
                    if (seekTo > 0) {
                        mMediaPlayer.seekTo(seekTo);
                        mProgressSb.setProgress(seekTo);
                    }
                }
                break;
            case R.id.iv_forward:
                if (MediaPlayState.PAUSED == mPlayState || MediaPlayState.COMPLETED == mPlayState || MediaPlayState.STARTED == mPlayState) {
                    int seekTo = mMediaPlayer.getCurrentPosition() + 10 * 1000;
                    if (seekTo < mMediaPlayer.getDuration()) {
                        mMediaPlayer.seekTo(seekTo);
                        mProgressSb.setProgress(seekTo);
                    }
                }
                break;
        }
    }

    class TimeCountThread extends Thread {

        private boolean mIsPaused;
        private boolean mIsClosed;

        public synchronized void pauseThread() {
            mIsPaused = true;
        }

        public synchronized void resumeThread() {
            mIsPaused = false;
            this.notify();
        }

        public synchronized void closeThread() {
            try {
                notify();
                mIsClosed = true;
                interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (!mIsClosed && !isInterrupted()) {
                if (!mIsPaused) {
                    mHideHandler.post(mUpdateProgressRunnable);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        synchronized (this) {
                            wait();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
