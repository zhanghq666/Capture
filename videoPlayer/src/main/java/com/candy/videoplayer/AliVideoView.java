package com.candy.videoplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.widget.RelativeLayout;

import com.aliyun.player.AliPlayer;
import com.aliyun.player.AliPlayerFactory;
import com.aliyun.player.IPlayer;
import com.aliyun.player.bean.ErrorInfo;
import com.aliyun.player.bean.InfoBean;
import com.aliyun.player.bean.InfoCode;
import com.aliyun.player.nativeclass.CacheConfig;
import com.aliyun.player.nativeclass.PlayerConfig;
import com.aliyun.player.nativeclass.TrackInfo;
import com.aliyun.player.source.UrlSource;
import com.candy.commonlibrary.utils.LogUtil;

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/29 9:11
 */
public class AliVideoView extends RelativeLayout {

    private static final String TAG = "AliVideoView";

    private TextureView mSurfaceTv;
    private AliPlayer mAliPlayer;
    private String mCurrentMediaPath;
    private long mCurrentPositionMs, mBufferedPositionMs;
    private int mCurrentState;

    private OnCompletionListener onCompletionListener;
    private OnVideoSizeChangedListener onVideoSizeChangedListener;
    private OnErrorListener onErrorListener;

    public AliVideoView(Context context) {
        this(context, null);
    }

    public AliVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AliVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        initPlayer();
        LayoutInflater.from(getContext()).inflate(R.layout.layout_video_view, this);
        mSurfaceTv = (TextureView) findViewById(R.id.tv_surface);
        mSurfaceTv.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                LogUtil.d(TAG, "onSurfaceTextureAvailable");
                mAliPlayer.setSurface(new Surface(surface));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                LogUtil.d(TAG, "onSurfaceTextureSizeChanged width = " + width + " height = " + height);
                mAliPlayer.redraw();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                LogUtil.d(TAG, "onSurfaceTextureDestroyed");
                mAliPlayer.setDisplay(null);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    private void initPlayer() {
        mAliPlayer = AliPlayerFactory.createAliPlayer(getContext().getApplicationContext());
        mAliPlayer.setAutoPlay(true);
        // 缓存设置
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.mEnable = true;
        cacheConfig.mMaxDurationS = 10;
        cacheConfig.mMaxSizeMB = 100;
        mAliPlayer.setCacheConfig(cacheConfig);
        // 缓冲相关、网络请求相关设置
//        PlayerConfig playerConfig = mAliPlayer.getConfig();
//        mAliPlayer.setConfig(playerConfig);
        mAliPlayer.setOnCompletionListener(new IPlayer.OnCompletionListener() {
            @Override
            public void onCompletion() {
                //播放完成事件
                LogUtil.d(TAG, "onCompletion");

                if (onCompletionListener != null) {
                    onCompletionListener.onCompletion();
                }
            }
        });
        mAliPlayer.setOnErrorListener(new IPlayer.OnErrorListener() {
            @Override
            public void onError(ErrorInfo errorInfo) {
                //出错事件
                LogUtil.d(TAG, "onError " + errorInfo.getMsg());

                if (onErrorListener != null) {
                    onErrorListener.onError(errorInfo.getCode().getValue(), errorInfo.getMsg());
                }
            }
        });
        mAliPlayer.setOnPreparedListener(new IPlayer.OnPreparedListener() {
            @Override
            public void onPrepared() {
                //准备成功事件
                LogUtil.d(TAG, "onPrepared");
            }
        });
        mAliPlayer.setOnVideoSizeChangedListener(new IPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(int width, int height) {
                //视频分辨率变化回调
                LogUtil.d(TAG, "onVideoSizeChanged " + width + "x" + height);
                if (onVideoSizeChangedListener != null) {
                    onVideoSizeChangedListener.onVideoSizeChanged(width, height);
                }
            }
        });
        mAliPlayer.setOnRenderingStartListener(new IPlayer.OnRenderingStartListener() {
            @Override
            public void onRenderingStart() {
                //首帧渲染显示事件
                LogUtil.d(TAG, "onRenderingStart");
            }
        });
        mAliPlayer.setOnInfoListener(new IPlayer.OnInfoListener() {
            @Override
            public void onInfo(InfoBean infoBean) {
                //其他信息的事件，type包括了：循环播放开始，缓冲位置，当前播放位置，自动播放开始等
                LogUtil.d(TAG, "onInfo " + infoBean.getCode() + " " + infoBean.getExtraMsg() + " " + infoBean.getExtraValue());
                if (InfoCode.CurrentPosition == infoBean.getCode()) {
                    mCurrentPositionMs = infoBean.getExtraValue();
                } else if (InfoCode.BufferedPosition == infoBean.getCode()) {
                    mBufferedPositionMs = infoBean.getExtraValue();
                }
            }
        });
        mAliPlayer.setOnLoadingStatusListener(new IPlayer.OnLoadingStatusListener() {
            @Override
            public void onLoadingBegin() {
                //缓冲开始。
                LogUtil.d(TAG, "onLoadingBegin");
            }
            @Override
            public void onLoadingProgress(int percent, float kbps) {
                //缓冲进度
                LogUtil.d(TAG, "onLoadingProgress percent:" + percent + " kbps:" + kbps);
            }
            @Override
            public void onLoadingEnd() {
                //缓冲结束
                LogUtil.d(TAG, "onLoadingEnd");
            }
        });
        mAliPlayer.setOnSeekCompleteListener(new IPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete() {
                //拖动结束
                LogUtil.d(TAG, "onSeekComplete");
            }
        });
        mAliPlayer.setOnSubtitleDisplayListener(new IPlayer.OnSubtitleDisplayListener() {
            @Override
            public void onSubtitleShow(long id, String data) {
                //显示字幕
                LogUtil.d(TAG, "onSubtitleShow");
            }
            @Override
            public void onSubtitleHide(long id) {
                //隐藏字幕
                LogUtil.d(TAG, "onSubtitleHide");
            }
        });
        mAliPlayer.setOnTrackChangedListener(new IPlayer.OnTrackChangedListener() {
            @Override
            public void onChangedSuccess(TrackInfo trackInfo) {
                //切换音视频流或者清晰度成功
                LogUtil.d(TAG, "onChangedSuccess");
            }
            @Override
            public void onChangedFail(TrackInfo trackInfo, ErrorInfo errorInfo) {
                //切换音视频流或者清晰度失败
                LogUtil.d(TAG, "onChangedFail");
            }
        });
        mAliPlayer.setOnStateChangedListener(new IPlayer.OnStateChangedListener() {
            @Override
            public void onStateChanged(int newState) {
                //播放器状态改变事件
                LogUtil.d(TAG, "onStateChanged newState:" + newState);
                mCurrentState = newState;
            }
        });
        mAliPlayer.setOnSnapShotListener(new IPlayer.OnSnapShotListener() {
            @Override
            public void onSnapShot(Bitmap bm, int with, int height) {
                //截图事件
                LogUtil.d(TAG, "onSnapShot");
            }
        });
    }

    public void setMediaSource(String path) {
        mCurrentMediaPath = path;
        UrlSource urlSource = new UrlSource();
        urlSource.setUri(path);
        mAliPlayer.setDataSource(urlSource);
        mAliPlayer.prepare();
    }

    public void seekTo(long progress) {
        mAliPlayer.seekTo(progress);
    }

    public long getCurrentPosition() {
        return mCurrentPositionMs;
    }

    public long getBufferedPosition() {
        return mBufferedPositionMs;
    }

    public int getVideoWidth() {
        return mAliPlayer.getVideoWidth();
    }

    public int getVideoHeight() {
        return mAliPlayer.getVideoHeight();
    }

    public void start() {
        mAliPlayer.start();
    }

    public void reload() {
        mAliPlayer.reload();
    }

    public void pause() {
        mAliPlayer.pause();
    }

    public void stop() {
        mAliPlayer.stop();
    }

    public void release() {
        mAliPlayer.release();
    }

    public long getDuration() {
        return mAliPlayer.getDuration();
    }

    public boolean isPlayable() {
        return mCurrentState == PlayerState.PREPARED.mValue ||
                mCurrentState == PlayerState.STARTED.mValue ||
                mCurrentState == PlayerState.PAUSED.mValue;
    }

    public boolean isPlaying() {
        return mCurrentState == PlayerState.STARTED.mValue;
    }

    public boolean isPaused() {
        return mCurrentState == PlayerState.PAUSED.mValue;
    }

    public boolean isCompleted() {
        return mCurrentState == PlayerState.COMPLETED.mValue;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        onCompletionListener = listener;
    }

    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        onVideoSizeChangedListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        onErrorListener = listener;
    }

    public interface OnCompletionListener {
        void onCompletion();
    }

    public interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(int width, int height);
    }

    public interface OnErrorListener {
        void onError(int code, String msg);
    }

    /**
     * 这是通过OnStateChangedListener现象推测出的状态含义枚举，SDK上报的状态只有一个int值，也没找到关于这个int值的说明文档
     */
    enum PlayerState {
        IDLE(0),
        INITIALIZED(1),
        PREPARED(2),
        STARTED(3),
        PAUSED(4),
        COMPLETED(6),
        ;


        private int mValue;
        PlayerState(int value) {
            mValue = value;
        }
    }
}
