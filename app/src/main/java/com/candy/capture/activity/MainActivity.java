package com.candy.capture.activity;

import android.Manifest;
import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.candy.capture.R;
import com.candy.capture.adapter.ContentListAdapter;
import com.candy.capture.core.ConstantValues;
import com.candy.capture.core.DBHelper;
import com.candy.capture.customview.ImageViewerDialog;
import com.candy.capture.model.MediaPlayState;
import com.candy.capture.service.LocationService;
import com.candy.capture.customview.ContentListDivider;
import com.candy.capture.model.Content;
import com.candy.capture.util.DensityUtil;
import com.candy.capture.util.FileUtil;
import com.candy.capture.util.TipsUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends BaseActivity implements ContentListAdapter.ItemClickCallback {

    private static final String TAG = "MainActivity";
    // 列表获取更多时用到的提前量
    private static final int LIST_BUFFER_THRESHOLD = 3;

    private final int WHOLE_PERMISSION_REQUEST = 1;
    private final int AUDIO_PERMISSION_REQUEST = 2;
    private final int VIDEO_PERMISSION_REQUEST = 3;

    private final int REQ_CODE_ADD_CONTENT = 1;
    private final int REQ_CODE_TAKE_PICTURE = 2;
    private final int REQ_CODE_VIDEO_CAPTURE = 3;

    private DBHelper mDBHelper;
    private static final int COUNT = 20;

    private CoordinatorLayout mRootCl;
    private FloatingActionButton mAddContentFab;
    private FloatingActionButton mAddTextFab;
    private FloatingActionButton mAddAudioFab;
    private FloatingActionButton mAddPhotoFab;
    private FloatingActionButton mAddVideoFab;
    /**
     * 控制FloatingActionButton动画收回的透明View
     */
    private View mMaskView;

    private RecyclerView mContentsRv;

    /**
     * 标识添加内容是否处于展开状态
     */
    private boolean mIsAddButtonPressed;
    /**
     * 标识是否处于编辑模式
     */
    private boolean mIsInEditMode;
    /**
     * 内容集合
     */
    private ArrayList<Content> mContentList;
    /**
     * 编辑状态下选中内容项集合
     */
    private ArrayList<Content> mSelectedContents;
    /**
     * RecyclerView Adapter
     */
    private ContentListAdapter mContentListAdapter;

    /**
     * 音频MediaPlayer实例
     */
    private MediaPlayer mMediaPlayer;
    /**
     * 音频MediaPlayer状态
     */
    private MediaPlayState mPlayState;
    /**
     * 正在播放音频的列表项索引
     */
    private int mCurrentPlayingItemIndex = -1;
    private boolean mIsLoadingData;
    private boolean mHasMore;

    private String mCameraFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsAddButtonPressed) {
                    showOutAnimation();
                }
            }
        });
        setSupportActionBar(toolbar);

        findView();
        setView();
        mDBHelper = new DBHelper(this);

        startLocation();
        getWholePermissions();
        getContents();
    }

    private void findView() {
        mRootCl = (CoordinatorLayout) findViewById(R.id.cl_root);
        mAddContentFab = (FloatingActionButton) findViewById(R.id.fab_add);
        mAddTextFab = (FloatingActionButton) findViewById(R.id.fab_add_text);
        mAddAudioFab = (FloatingActionButton) findViewById(R.id.fab_add_audio);
        mAddPhotoFab = (FloatingActionButton) findViewById(R.id.fab_add_photo);
        mAddVideoFab = (FloatingActionButton) findViewById(R.id.fab_add_video);
        mMaskView = findViewById(R.id.mask_view);

        mContentsRv = (RecyclerView) findViewById(R.id.rv_contents);
    }

    private void setView() {
        mMaskView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOutAnimation();
            }
        });
        mAddContentFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsAddButtonPressed) {
                    mIsAddButtonPressed = true;
                    showInAnimation();
                } else {
                    mIsAddButtonPressed = false;
                    showOutAnimation();
                }
            }
        });
        mAddTextFab.setOnClickListener(mAddContentClickListener);
        mAddAudioFab.setOnClickListener(mAddContentClickListener);
        mAddPhotoFab.setOnClickListener(mAddContentClickListener);
        mAddVideoFab.setOnClickListener(mAddContentClickListener);

        mContentListAdapter = new ContentListAdapter(mContext, this);
        mContentsRv.setAdapter(mContentListAdapter);
        mContentsRv.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        mContentsRv.addItemDecoration(new ContentListDivider(mContext));
        mContentsRv.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                if (mHasMore && !mIsLoadingData && totalItemCount < (lastVisibleItem + LIST_BUFFER_THRESHOLD)) {
                    getContents();
                }
            }
        });
    }


    //region 定位
    @TargetApi(Build.VERSION_CODES.M)
    private void startLocation() {
        // 权限未拿到时从Main去请求权限、启动定位Service，拿到以后从Splash启动
        // 低版本直接定位，高版本请求权限通过后定位
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            }
        } else {
            startLocationService();
        }
    }

    private void startLocationService() {
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        Intent intent = new Intent(mContext, LocationService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);
    }
    //endregion

    //region 权限相关
    @TargetApi(Build.VERSION_CODES.M)
    private void getWholePermissions() {
        // 权限未拿到时从Main去请求权限、启动定位Service，拿到以后从Splash启动
        // 低版本直接定位，高版本请求权限通过后定位

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            /***
             * 定位权限为必须权限，用户如果禁止，则每次进入都会申请
             */
            // 定位精确位置
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
			 */
            // 读写权限
            addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            // 读取电话状态权限
//            addPermission(permissions, Manifest.permission.READ_PHONE_STATE);
            // 摄像头权限
            addPermission(permissions, Manifest.permission.CAMERA);
            // 录音权限
            addPermission(permissions, Manifest.permission.RECORD_AUDIO);

            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), WHOLE_PERMISSION_REQUEST);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean getAudioPermissions() {
        boolean result = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
			 */
            // 读写权限
            addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            // 录音权限
            addPermission(permissions, Manifest.permission.RECORD_AUDIO);

            if (permissions.size() > 0) {
                result = false;
                requestPermissions(permissions.toArray(new String[permissions.size()]), AUDIO_PERMISSION_REQUEST);
            }
        }

        return result;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean getVideoPermissions() {
        boolean result = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
			 */
            // 读写权限
            addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            // 摄像头权限
            addPermission(permissions, Manifest.permission.CAMERA);

            if (permissions.size() > 0) {
                result = false;
                requestPermissions(permissions.toArray(new String[permissions.size()]), VIDEO_PERMISSION_REQUEST);
            }
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
//            if (shouldShowRequestPermissionRationale(permission)) {
//                return true;
//            } else {
            permissionsList.add(permission);
            return false;
//            }

        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (WHOLE_PERMISSION_REQUEST == requestCode) {
            if (permissions.length > 0) {
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED ||
                            permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        startLocationService();
                        break;
                    }
                }
            }
        } else {
            boolean isDenied = false;
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        isDenied = true;
                        break;
                    }
                }
            } else {
                isDenied = true;

            }
            switch (requestCode) {
                case AUDIO_PERMISSION_REQUEST:
                    if (isDenied) {
                        TipsUtil.showSnackbar(mRootCl, "权限不足，无法继续进行", "重新授权", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                getAudioPermissions();
                            }
                        });
                    } else {
                        gotoAudioRecord();
                    }
                    break;
                case VIDEO_PERMISSION_REQUEST:
                    if (isDenied) {
                        TipsUtil.showSnackbar(mRootCl, "权限不足，无法继续进行", "重新授权", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                getVideoPermissions();
                            }
                        });
                    } else {
                        gotoVideoRecord();
                    }
                    break;
            }
        }
    }
    //endregion

    //region FloatingActionButton动画
    private void showOutAnimation() {
        mMaskView.setVisibility(View.GONE);
        mAddContentFab.animate().rotation(0).scaleX(1).scaleY(1).setDuration(200).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mAddContentFab.setBackgroundTintList(ColorStateList.valueOf(0xff081fcc));
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).setInterpolator(new AccelerateInterpolator()).start();
        mAddTextFab.animate().translationX(0).alpha(0).setStartDelay(300).setDuration(200).setInterpolator(new AccelerateInterpolator()).start();
        mAddAudioFab.animate().translationX(0).translationY(0).alpha(0).setStartDelay(200).setDuration(200).setInterpolator(new AccelerateInterpolator()).start();
        mAddPhotoFab.animate().translationX(0).translationY(0).alpha(0).setStartDelay(100).setDuration(200).setInterpolator(new AccelerateInterpolator()).start();
        mAddVideoFab.animate().translationY(0).alpha(0).setStartDelay(0).setDuration(200).setInterpolator(new AccelerateInterpolator()).start();
    }

    private void showInAnimation() {
        mMaskView.setVisibility(View.VISIBLE);
        int radius = DensityUtil.dip2px(mContext, 150);
        mAddContentFab.animate().rotation(45).scaleX((float) 0.8).scaleY((float) 0.8).setDuration(200).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mAddContentFab.setBackgroundTintList(ColorStateList.valueOf(0xff858585));
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        }).setInterpolator(new OvershootInterpolator()).start();
        mAddTextFab.animate().translationXBy(-radius).alphaBy(1).setStartDelay(0).setDuration(200).setInterpolator(new OvershootInterpolator()).start();
        mAddAudioFab.animate().translationXBy((float) -(Math.sin(Math.PI / 3) * radius))
                .translationYBy((float) -(Math.cos(Math.PI / 3) * radius)).alphaBy(1).setStartDelay(100).setDuration(200).setInterpolator(new OvershootInterpolator()).start();
        mAddPhotoFab.animate().translationXBy((float) -(Math.sin(Math.PI / 6) * radius))
                .translationYBy((float) -(Math.cos(Math.PI / 6) * radius)).alphaBy(1).setStartDelay(200).setDuration(200).setInterpolator(new OvershootInterpolator()).start();
        mAddVideoFab.animate().translationYBy(-radius).alphaBy(1).setStartDelay(300).setDuration(200).setInterpolator(new OvershootInterpolator()).start();
    }

    private void hideAddButton() {
        mAddContentFab.animate().scaleX(0).scaleY(0).setDuration(200).start();
    }

    private void showAddButton() {
        mAddContentFab.animate().scaleX(1).scaleY(1).setDuration(200).start();
    }
    //endregion

    //region 获取内容
    private void getNewerContents() {
        mIsLoadingData = true;
        if (mContentList == null || mContentList.isEmpty()) {
            mContentList = mDBHelper.getContentNewer(-1, COUNT);
            mContentListAdapter.setContents(mContentList);
        } else {
            ArrayList contents = mDBHelper.getContentNewer(mContentList.get(0).getId(), COUNT);
            mContentList.addAll(0, contents);
            int count = contents.size();
            mContentListAdapter.notifyItemRangeInserted(0, count);
        }
        mIsLoadingData = false;
    }

    private void getContents() {
        mIsLoadingData = true;
        if (mContentList == null || mContentList.isEmpty()) {
            mContentList = mDBHelper.getContentOlder(-1, COUNT);
            mContentListAdapter.setContents(mContentList);
        } else {
            ArrayList contents = mDBHelper.getContentOlder(mContentList.get(mContentList.size() - 1).getId(), COUNT);
            if (contents.size() > 0) {
                mContentList.addAll(contents);
                int startPosition = mContentList.size() - 1;
                int count = contents.size();
                mContentList.addAll(contents);
                mContentListAdapter.notifyItemRangeInserted(startPosition, count);
            } else {
                mHasMore = false;
            }
        }
        mIsLoadingData = false;
    }
    //endregion

    //region 菜单控制
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (mIsInEditMode) {
            menu.findItem(R.id.action_search).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(true);
        } else {
            menu.findItem(R.id.action_search).setVisible(true);
            menu.findItem(R.id.action_delete).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            return true;
        } else if (id == R.id.action_delete) {
            if (mSelectedContents != null && !mSelectedContents.isEmpty()) {
                AlertDialog dialog = new AlertDialog.Builder(mContext)
                        .setMessage("要删除此内容吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteContent();
                            }
                        })
                        .create();
                dialog.show();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteContent() {
        if (mSelectedContents != null && !mSelectedContents.isEmpty()) {
            try {
                if (!mDBHelper.deleteContent(mSelectedContents)) {
                    TipsUtil.showToast(mContext, "删除所选内容失败");
                    return;
                }

                mContentList.removeAll(mSelectedContents);
                for (Content content : mSelectedContents) {
                    if (!TextUtils.isEmpty(content.getMediaFilePath())) {
                        new File(content.getMediaFilePath()).delete();
                    }
                }

                exitEditMode();
            } catch (Exception e) {
                e.printStackTrace();
                TipsUtil.showToast(mContext, "删除所选内容失败");
            }
        }
    }

    //endregion

    //region FloatingActionButton点击处理
    private View.OnClickListener mAddContentClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showOutAnimation();
            switch (v.getId()) {
                case R.id.fab_add_text:
                    gotoTextPublish();
                    break;
                case R.id.fab_add_audio:
                    if (getAudioPermissions()) {
                        gotoAudioRecord();
                    }
                    break;
                case R.id.fab_add_photo:
                    gotoTakePicture();
                    break;
                case R.id.fab_add_video:
                    if (getVideoPermissions()) {
                        gotoVideoRecord();
                    }
                    break;
            }
        }
    };

    private void gotoTextPublish() {
        Intent intent = new Intent(mContext, PublishActivity.class);
        startActivityForResult(intent, REQ_CODE_ADD_CONTENT);
    }

    private void gotoAudioRecord() {
        Intent intent = new Intent(mContext, AudioRecordActivity.class);
        startActivityForResult(intent, REQ_CODE_ADD_CONTENT);
    }

    private void gotoTakePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        mCameraFilePath = FileUtil.getMediaFilePath(this, FileUtil.MEDIA_TYPE_PHOTO);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mCameraFilePath)));
        } else {
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(MediaStore.Images.Media.DATA, mCameraFilePath);
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }

        startActivityForResult(intent, REQ_CODE_TAKE_PICTURE);
    }

    private void gotoVideoRecord() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        mCameraFilePath = FileUtil.getMediaFilePath(this, FileUtil.MEDIA_TYPE_VIDEO);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mCameraFilePath)));
        } else {
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(MediaStore.Images.Media.DATA, mCameraFilePath);
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }
        startActivityForResult(intent, REQ_CODE_VIDEO_CAPTURE);
    }
    //endregion

    //region 列表项事件回调
    @Override
    public void onItemClick(int position) {
        if (mIsInEditMode) {
            mContentList.get(position).setSelect(!mContentList.get(position).isSelect());
            if (mSelectedContents == null)
                mSelectedContents = new ArrayList<>();
            if (mContentList.get(position).isSelect()) {
                mSelectedContents.add(mContentList.get(position));
            } else {
                mSelectedContents.remove(mContentList.get(position));
            }
            if (mSelectedContents.isEmpty()) {
                exitEditMode();
            } else {
                mContentListAdapter.notifyItemChanged(position);
            }
        }
    }

    @Override
    public void onItemLongPressed(int position) {
        if (!mIsInEditMode) {
            mContentList.get(position).setSelect(true);
            if (mSelectedContents == null)
                mSelectedContents = new ArrayList<>();
            mSelectedContents.add(mContentList.get(position));
            gotoEditMode();
        }
    }

    @Override
    public void onAudioViewClick(int position) {
        if (mIsInEditMode) {
            onItemClick(position);
            return;
        }
        if (mCurrentPlayingItemIndex == position) {
            if (MediaPlayState.STARTED == mPlayState) {
                stopMedia();
            } else if (MediaPlayState.COMPLETED == mPlayState || MediaPlayState.PREPARED == mPlayState) {
                playMedia();
            } else if (MediaPlayState.STOPPED == mPlayState) {
                prepareMediaPlayer();
                playMedia();
            }
        } else {
            if (mCurrentPlayingItemIndex >= 0) {
                if (MediaPlayState.STARTED == mPlayState) {
                    stopMedia();
                }

                mMediaPlayer.reset();
                mPlayState = MediaPlayState.IDLE;
            }

            mCurrentPlayingItemIndex = position;

            setMediaPlayerSource();
            prepareMediaPlayer();
            playMedia();
        }
    }

    @Override
    public void onImageViewClick(int position) {
        if (mIsInEditMode) {
            onItemClick(position);
            return;
        }
        ArrayList<String> list = new ArrayList<>();
        list.add(mContentList.get(position).getMediaFilePath());
        ImageViewerDialog.showDialog(this, list, 1);
    }

    @Override
    public void onVideoViewClick(int position) {
        if (mIsInEditMode) {
            onItemClick(position);
            return;
        }

        if (MediaPlayState.STARTED == mPlayState) {
            stopMedia();
        }
        //TODO: 视频播放
    }

    private void gotoEditMode() {
        mIsInEditMode = true;
        // 改变菜单项
        invalidateOptionsMenu();
        // 隐藏添加按钮
        hideAddButton();
        // 停止播放音频、释放资源
        releaseMedia();
        mCurrentPlayingItemIndex = -1;
        // 列表显示变化
        mContentListAdapter.setEditMode(true);
        mContentListAdapter.notifyDataSetChanged();
    }

    private void exitEditMode() {
        mIsInEditMode = false;
        // 改变菜单项
        invalidateOptionsMenu();
        // 显示添加按钮
        showAddButton();
        // 重新组织MediaPlayer
        initMediaPlayer();
        // 清空已选项集合
        if (mSelectedContents != null && !mSelectedContents.isEmpty()) {
            for (Content content : mSelectedContents) {
                content.setSelect(false);
            }
            mSelectedContents.clear();
        }
        // 列表显示变化
        mContentListAdapter.setEditMode(false);
        mContentListAdapter.notifyDataSetChanged();
    }
    //endregion

    //region 语音播放相关
    @Override
    protected void onResume() {
        super.onResume();

        initMediaPlayer();
    }

    /**
     * 供Adapter调用获取当前音频播放进度
     *
     * @return
     */
    public double getPlayPercent() {
        double percent = 0.0;
        if (MediaPlayState.STARTED == mPlayState) {
            percent = (double) mMediaPlayer.getCurrentPosition() / mMediaPlayer.getDuration();
        }
        Log.d(TAG, "getPlayPercent percent = " + percent);
        return percent;
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mPlayState = MediaPlayState.IDLE;
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mPlayState = MediaPlayState.COMPLETED;

                    mContentList.get(mCurrentPlayingItemIndex).setPlayingAudio(false);
                    mContentListAdapter.notifyItemChanged(mCurrentPlayingItemIndex);
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
        if (mCurrentPlayingItemIndex >= 0) {
            setMediaPlayerSource();
            prepareMediaPlayer();
        }
    }

    private void setMediaPlayerSource() {
        try {
            mMediaPlayer.setDataSource(mContentList.get(mCurrentPlayingItemIndex).getMediaFilePath());
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
        mMediaPlayer.start();
        mPlayState = MediaPlayState.STARTED;

        mContentList.get(mCurrentPlayingItemIndex).setPlayingAudio(true);
        mContentListAdapter.notifyItemChanged(mCurrentPlayingItemIndex);
    }

    private void stopMedia() {
        mMediaPlayer.stop();
        mPlayState = MediaPlayState.STOPPED;

        mContentList.get(mCurrentPlayingItemIndex).setPlayingAudio(false);
        mContentListAdapter.notifyItemChanged(mCurrentPlayingItemIndex);
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

    @Override
    protected void onPause() {
        super.onPause();
        releaseMedia();
    }
    //endregion

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQ_CODE_ADD_CONTENT == requestCode) {
            if (RESULT_OK == resultCode) {
                getNewerContents();
            }
        } else if (REQ_CODE_TAKE_PICTURE == requestCode) {
            if (RESULT_OK == resultCode) {
                if (!TextUtils.isEmpty(mCameraFilePath)) {
                    Content content = new Content();
                    content.setType(ConstantValues.CONTENT_TYPE_PHOTO);
                    content.setMediaFilePath(mCameraFilePath);
                    Intent intent = new Intent(this, PublishActivity.class);
                    intent.putExtra(PublishActivity.INTENT_KEY_TYPE, ConstantValues.CONTENT_TYPE_PHOTO);
                    intent.putExtra(PublishActivity.INTENT_KEY_CONTENT, content);
                    startActivityForResult(intent, REQ_CODE_ADD_CONTENT);
                }
            }
        } else if (REQ_CODE_VIDEO_CAPTURE == requestCode) {
            if (RESULT_OK == resultCode) {
                Content content = new Content();
                content.setType(ConstantValues.CONTENT_TYPE_VIDEO);
                content.setMediaFilePath(mCameraFilePath);
                Intent intent = new Intent(this, PublishActivity.class);
                intent.putExtra(PublishActivity.INTENT_KEY_TYPE, ConstantValues.CONTENT_TYPE_VIDEO);
                intent.putExtra(PublishActivity.INTENT_KEY_CONTENT, content);
                startActivityForResult(intent, REQ_CODE_ADD_CONTENT);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsAddButtonPressed) {
            mIsAddButtonPressed = false;
            showOutAnimation();
        } else if (mIsInEditMode) {
            exitEditMode();
        } else {
            super.onBackPressed();
//            finish();
//            System.exit(0);
        }
    }
}
