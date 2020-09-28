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
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.candy.capture.IFloatAidlInterface;
import com.candy.capture.R;
import com.candy.capture.core.ConstantValues;
import com.candy.capture.core.SharedPreferenceManager;
import com.candy.capture.fragment.ContentListFragment;
import com.candy.capture.service.FloatingWindowService;
import com.candy.capture.service.LocationService;
import com.candy.capture.model.Content;
import com.candy.capture.util.DensityUtil;
import com.candy.capture.util.FileUtil;
import com.candy.capture.util.LogUtil;
import com.candy.capture.util.TipsUtil;
import com.jaeger.library.StatusBarUtil;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends BaseActivity implements ContentListFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";

    private static final int WHOLE_PERMISSION_REQUEST = 1;
    private static final int AUDIO_PERMISSION_REQUEST = 2;
    private static final int VIDEO_PERMISSION_REQUEST = 3;

    private static final int REQ_CODE_ADD_CONTENT = 1;
    private static final int REQ_CODE_TAKE_PICTURE = 2;
    private static final int REQ_CODE_VIDEO_CAPTURE = 3;
    private static final int REQ_CODE_OVERLAY_PERMISSION = 4;

    private CoordinatorLayout mRootCl;
    private FloatingActionButton mAddContentFab;
    private FloatingActionButton mAddTextFab;
    private FloatingActionButton mAddAudioFab;
    private FloatingActionButton mAddPhotoFab;
    private FloatingActionButton mAddVideoFab;

    private ContentListFragment mContentListFragment;
    /**
     * 控制FloatingActionButton动画收回的透明View
     */
    private View mMaskView;

    /**
     * 标识添加内容是否处于展开状态
     */
    private boolean mIsAddButtonPressed;
    /**
     * 标识是否处于编辑模式
     */
    private boolean mIsInEditMode;

    private String mCameraFilePath;
    private AlertDialog mFloatingAlertDialog;

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

        startLocation();
        getWholePermissions();

        if (savedInstanceState == null) {
            SharedPreferenceManager.getInstance(this).setFirstRun(false);

            mContentListFragment = ContentListFragment.newInstance(false);
            getSupportFragmentManager().beginTransaction().add(R.id.fl_fragment, mContentListFragment, "content_list").commit();
        } else {
            mContentListFragment = (ContentListFragment) getSupportFragmentManager().findFragmentByTag("content_list");
        }
    }

    private void findView() {
        mRootCl = (CoordinatorLayout) findViewById(R.id.cl_root);
        mAddContentFab = (FloatingActionButton) findViewById(R.id.fab_add);
        mAddTextFab = (FloatingActionButton) findViewById(R.id.fab_add_text);
        mAddAudioFab = (FloatingActionButton) findViewById(R.id.fab_add_audio);
        mAddPhotoFab = (FloatingActionButton) findViewById(R.id.fab_add_photo);
        mAddVideoFab = (FloatingActionButton) findViewById(R.id.fab_add_video);
        mMaskView = findViewById(R.id.mask_view);
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
                    showInAnimation();
                } else {
                    showOutAnimation();
                }
            }
        });
        mAddTextFab.setOnClickListener(mAddContentClickListener);
        mAddAudioFab.setOnClickListener(mAddContentClickListener);
        mAddPhotoFab.setOnClickListener(mAddContentClickListener);
        mAddVideoFab.setOnClickListener(mAddContentClickListener);
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

//    private boolean mLocationBound;
//    private ServiceConnection connLocation = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            LogUtil.d(TAG, "Location service connected");
//            mLocationBound = true;
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            LogUtil.d(TAG, "Location service disconnected");
//            mLocationBound = false;
//        }
//    };

    private void startLocationService() {
//        if (!mLocationBound) {
            Intent intent = new Intent(mContext, LocationService.class);
//            bindService(intent, connLocation, BIND_AUTO_CREATE);
            startService(intent);
//        }
    }

    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy");
//        if (mLocationBound) {
//            LogUtil.d(TAG, "will unbind location service");
//            unbindService(connLocation);
//        }
        if (mFloatBound) {
            LogUtil.d(TAG, "will unbind floating service");
            unbindService(connFloat);
        }
        super.onDestroy();
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
            addPermission(permissions, Manifest.permission.READ_PHONE_STATE);
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
    private static final int SINGLE_ANIMATION_DURATION = 200;

    private void showOutAnimation() {
        mIsAddButtonPressed = false;

        mMaskView.setVisibility(View.GONE);
        mAddContentFab.animate().rotation(0).scaleX(1).scaleY(1).setDuration(SINGLE_ANIMATION_DURATION).setListener(new Animator.AnimatorListener() {
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
        mAddTextFab.animate().translationX(0).alpha(0).setStartDelay(300).setDuration(SINGLE_ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        mAddAudioFab.animate().translationX(0).translationY(0).alpha(0).setStartDelay(200).setDuration(SINGLE_ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        mAddPhotoFab.animate().translationX(0).translationY(0).alpha(0).setStartDelay(100).setDuration(SINGLE_ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
        mAddVideoFab.animate().translationY(0).alpha(0).setStartDelay(0).setDuration(SINGLE_ANIMATION_DURATION).setInterpolator(new AccelerateInterpolator()).start();
    }

    private void showInAnimation() {
        mIsAddButtonPressed = true;

        mMaskView.setVisibility(View.VISIBLE);
        int radius = DensityUtil.dip2px(mContext, 150);
        mAddContentFab.animate().rotation(45).scaleX((float) 0.8).scaleY((float) 0.8).setDuration(SINGLE_ANIMATION_DURATION).setListener(new Animator.AnimatorListener() {
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
        mAddTextFab.animate().translationX(-radius).alphaBy(1).setStartDelay(0).setDuration(SINGLE_ANIMATION_DURATION).setInterpolator(new OvershootInterpolator()).start();
        mAddAudioFab.animate().translationX((float) -(Math.sin(Math.PI / 3) * radius))
                .translationY((float) -(Math.cos(Math.PI / 3) * radius)).alphaBy(1).setStartDelay(100).setDuration(SINGLE_ANIMATION_DURATION).setInterpolator(new OvershootInterpolator()).start();
        mAddPhotoFab.animate().translationX((float) -(Math.sin(Math.PI / 6) * radius))
                .translationY((float) -(Math.cos(Math.PI / 6) * radius)).alphaBy(1).setStartDelay(200).setDuration(SINGLE_ANIMATION_DURATION).setInterpolator(new OvershootInterpolator()).start();
        mAddVideoFab.animate().translationY(-radius).alphaBy(1).setStartDelay(300).setDuration(SINGLE_ANIMATION_DURATION).setInterpolator(new OvershootInterpolator()).start();
    }

    private void hideAddButton() {
        mAddContentFab.animate().scaleX(0).scaleY(0).setDuration(SINGLE_ANIMATION_DURATION).start();
    }

    private void showAddButton() {
        mAddContentFab.animate().scaleX(1).scaleY(1).setDuration(SINGLE_ANIMATION_DURATION).start();
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
        menu.findItem(R.id.action_fast_capture).setChecked(SharedPreferenceManager.getInstance(this).isAllowFastCapture());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = false;
        int id = item.getItemId();

        if (id == R.id.action_search) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);

            handled = true;
        } else if (id == R.id.action_delete) {
            if (mContentListFragment != null) {
                mContentListFragment.checkDeleteContent();
            }

            handled = true;
        } else if (id == R.id.action_fast_capture) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(mContext)) {
                    if (mFloatingAlertDialog == null) {
                        mFloatingAlertDialog = new AlertDialog.Builder(mContext)
                                .setTitle("无悬浮窗权限")
                                .setMessage("请前往设置-应用管理中为Capture开启显示悬浮窗权限")
                                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:" + getPackageName()));
                                        startActivityForResult(intent, REQ_CODE_OVERLAY_PERMISSION);
                                    }
                                })
                                .setNegativeButton("先等会", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .create();
                    }
                    mFloatingAlertDialog.show();
                } else {
                    menuCheck(item);
                }
            } else {
                menuCheck(item);
            }
            handled = true;
        }

        return handled || super.onOptionsItemSelected(item);
    }

    private void menuCheck(MenuItem item) {
        boolean showFloating = !item.isChecked();
        item.setChecked(showFloating);

        SharedPreferenceManager.getInstance(this).setAllowFastCapture(showFloating);
        toggleFloatWindow(showFloating);
    }


    private boolean mFloatBound;
    ServiceConnection connFloat = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtil.d(TAG, "Floating service connected");
            mFloatBound = true;
            myAidlInterface = IFloatAidlInterface.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtil.d(TAG, "Floating service disconnected");
            mFloatBound = false;
        }
    };
    IFloatAidlInterface myAidlInterface;

    private void toggleFloatWindow(boolean toggle) {
        if (myAidlInterface == null) {
            Intent intent1 = new Intent(this, FloatingWindowService.class);
            this.startService(intent1);

            Intent intent = new Intent(this, FloatingWindowService.class);
            intent.putExtra(FloatingWindowService.EXTRA_TOGGLE, toggle);
            bindService(intent, connFloat, BIND_AUTO_CREATE | BIND_ABOVE_CLIENT);
        } else {
            try {
                myAidlInterface.toggleFloatWindow(toggle);
            } catch (RemoteException e) {
                e.printStackTrace();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQ_CODE_ADD_CONTENT == requestCode) {
            if (RESULT_OK == resultCode) {
                if (mContentListFragment != null) {
                    mContentListFragment.getNewerContents();
                }
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
            showOutAnimation();
        } else if (mIsInEditMode) {
            exitEditMode();
            if (mContentListFragment != null) {
                mContentListFragment.exitEditMode(true);
            }
        } else {
            super.onBackPressed();
//            finish();
//            System.exit(0);
        }
    }

    @Override
    public void gotoEditMode() {
        mIsInEditMode = true;
        // 改变菜单项
        invalidateOptionsMenu();
        // 隐藏添加按钮
        hideAddButton();
    }

    @Override
    public void exitEditMode() {
        mIsInEditMode = false;
        // 改变菜单项
        invalidateOptionsMenu();
        // 显示添加按钮
        showAddButton();
    }
}
