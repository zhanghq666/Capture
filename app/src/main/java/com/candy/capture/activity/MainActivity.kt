package com.candy.capture.activity

import android.Manifest
import android.animation.Animator
import android.annotation.TargetApi
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.databinding.DataBindingUtil
import com.candy.capture.IFloatAidlInterface
import com.candy.capture.R
import com.candy.capture.core.ConstantValues
import com.candy.capture.core.SharedPreferenceManager
import com.candy.capture.fragment.ContentListFragment
import com.candy.capture.fragment.ContentListFragment.OnFragmentInteractionListener
import com.candy.capture.model.Content
import com.candy.capture.service.FloatingWindowService
import com.candy.capture.service.LocationService
import com.candy.capture.util.FileUtil
import com.candy.commonlibrary.utils.DensityUtil
import com.candy.commonlibrary.utils.LogUtil
import com.candy.commonlibrary.utils.TipsUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.*

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 10:48
 */
class MainActivity: BaseActivity(), OnFragmentInteractionListener {
    companion object {
        private const val TAG = "MainActivity"

        private const val WHOLE_PERMISSION_REQUEST = 1
        private const val AUDIO_PERMISSION_REQUEST = 2
        private const val VIDEO_PERMISSION_REQUEST = 3

        private const val REQ_CODE_ADD_CONTENT = 1
        private const val REQ_CODE_TAKE_PICTURE = 2
        private const val REQ_CODE_VIDEO_CAPTURE = 3
        private const val REQ_CODE_OVERLAY_PERMISSION = 4
    }

    private var mRootCl: CoordinatorLayout? = null
    private var mAddContentFab: FloatingActionButton? = null
    private var mAddTextFab: FloatingActionButton? = null
    private var mAddAudioFab: FloatingActionButton? = null
    private var mAddPhotoFab: FloatingActionButton? = null
    private var mAddVideoFab: FloatingActionButton? = null

    private lateinit var mContentListFragment: ContentListFragment

    /**
     * 控制FloatingActionButton动画收回的透明View
     */
    private var mMaskView: View? = null

    /**
     * 标识添加内容是否处于展开状态
     */
    private var mIsAddButtonPressed = false

    /**
     * 标识是否处于编辑模式
     */
    private var mIsInEditMode = false

    private var mCameraFilePath: String? = null
    private var mFloatingAlertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.setOnClickListener {
            if (mIsAddButtonPressed) {
                showOutAnimation()
            }
        }
        setSupportActionBar(toolbar)
        findView()
        setView()
        startLocation()
        getWholePermissions()
        if (savedInstanceState == null) {
            SharedPreferenceManager.getInstance(this).setFirstRun(false)
            mContentListFragment = ContentListFragment.newInstance(false)
            supportFragmentManager.beginTransaction().add(R.id.fl_fragment, mContentListFragment, "content_list").commit()
        } else {
            mContentListFragment = supportFragmentManager.findFragmentByTag("content_list") as ContentListFragment
        }
    }

    private fun findView() {
        mRootCl = findViewById<View>(R.id.cl_root) as CoordinatorLayout
        mAddContentFab = findViewById<View>(R.id.fab_add) as FloatingActionButton
        mAddTextFab = findViewById<View>(R.id.fab_add_text) as FloatingActionButton
        mAddAudioFab = findViewById<View>(R.id.fab_add_audio) as FloatingActionButton
        mAddPhotoFab = findViewById<View>(R.id.fab_add_photo) as FloatingActionButton
        mAddVideoFab = findViewById<View>(R.id.fab_add_video) as FloatingActionButton
        mMaskView = findViewById(R.id.mask_view)
    }

    private fun setView() {
        mMaskView!!.setOnClickListener { showOutAnimation() }
        mAddContentFab!!.setOnClickListener {
            if (!mIsAddButtonPressed) {
                showInAnimation()
            } else {
                showOutAnimation()
            }
        }
        mAddTextFab!!.setOnClickListener(mAddContentClickListener)
        mAddAudioFab!!.setOnClickListener(mAddContentClickListener)
        mAddPhotoFab!!.setOnClickListener(mAddContentClickListener)
        mAddVideoFab!!.setOnClickListener(mAddContentClickListener)
    }


    //region 定位
    @TargetApi(Build.VERSION_CODES.M)
    private fun startLocation() {
        // 权限未拿到时从Main去请求权限、启动定位Service，拿到以后从Splash启动
        // 低版本直接定位，高版本请求权限通过后定位
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                startLocationService()
            }
        } else {
            startLocationService()
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
    private fun startLocationService() {
//        if (!mLocationBound) {
        val intent = Intent(mContext, LocationService::class.java)
        //            bindService(intent, connLocation, BIND_AUTO_CREATE);
        startService(intent)
//        }
    }

    override fun onDestroy() {
        LogUtil.d(TAG, "onDestroy")
        //        if (mLocationBound) {
//            LogUtil.d(TAG, "will unbind location service");
//            unbindService(connLocation);
//        }
        if (mFloatBound) {
            LogUtil.d(TAG, "will unbind floating service")
            unbindService(connFloat)
        }
        super.onDestroy()
    }

    //endregion

    //region 权限相关
    @TargetApi(Build.VERSION_CODES.M)
    private fun getWholePermissions() {
        // 低版本直接定位，高版本请求权限通过后定位
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = ArrayList<String>()
            // 定位精确位置
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
			 */
            // 读写权限
            addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // 读取电话状态权限
            addPermission(permissions, Manifest.permission.READ_PHONE_STATE)
            // 摄像头权限
            addPermission(permissions, Manifest.permission.CAMERA)
            // 录音权限
            addPermission(permissions, Manifest.permission.RECORD_AUDIO)

            addPermission(permissions, Manifest.permission.FOREGROUND_SERVICE)
            if (permissions.size > 0) {
                requestPermissions(permissions.toTypedArray(), WHOLE_PERMISSION_REQUEST)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun getAudioPermissions(): Boolean {
        var result = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = ArrayList<String>()
            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
			 */
            // 读写权限
            addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // 录音权限
            addPermission(permissions, Manifest.permission.RECORD_AUDIO)
            if (permissions.size > 0) {
                result = false
                requestPermissions(permissions.toTypedArray(), AUDIO_PERMISSION_REQUEST)
            }
        }
        return result
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun getVideoPermissions(): Boolean {
        var result = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = ArrayList<String>()
            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
			 */
            // 读写权限
            addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // 摄像头权限
            addPermission(permissions, Manifest.permission.CAMERA)
            if (permissions.size > 0) {
                result = false
                requestPermissions(permissions.toTypedArray(), VIDEO_PERMISSION_REQUEST)
            }
        }
        return result
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun addPermission(permissionsList: ArrayList<String>, permission: String): Boolean {
        return if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
//            if (shouldShowRequestPermissionRationale(permission)) {
//                return true;
//            } else {
            permissionsList.add(permission)
            false
            //            }
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (WHOLE_PERMISSION_REQUEST == requestCode) {
            if (permissions.isNotEmpty()) {
                for (i in permissions.indices) {
                    if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION && grantResults[i] == PackageManager.PERMISSION_GRANTED ||
                            permissions[i] == Manifest.permission.ACCESS_COARSE_LOCATION && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        startLocationService()
                        break
                    }
                }
            }
        } else {
            var isDenied = false
            if (grantResults.isNotEmpty()) {
                for (i in grantResults.indices) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        isDenied = true
                        break
                    }
                }
            } else {
                isDenied = true
            }
            when (requestCode) {
                AUDIO_PERMISSION_REQUEST -> if (isDenied) {
                    TipsUtil.showSnackbar(mRootCl, "权限不足，无法继续进行", "重新授权") { getAudioPermissions() }
                } else {
                    gotoAudioRecord()
                }
                VIDEO_PERMISSION_REQUEST -> if (isDenied) {
                    TipsUtil.showSnackbar(mRootCl, "权限不足，无法继续进行", "重新授权") { getVideoPermissions() }
                } else {
                    gotoVideoRecord()
                }
            }
        }
    }
    //endregion

    //region FloatingActionButton动画
    private val SINGLE_ANIMATION_DURATION = 200

    private fun showOutAnimation() {
        mIsAddButtonPressed = false
        mMaskView!!.visibility = View.GONE
        mAddContentFab!!.animate().rotation(0f).scaleX(1f).scaleY(1f).setDuration(SINGLE_ANIMATION_DURATION.toLong()).setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                mAddContentFab!!.backgroundTintList = ColorStateList.valueOf(-0xf7e034)
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        }).setInterpolator(AccelerateInterpolator()).start()
        mAddTextFab!!.animate().translationX(0f).alpha(0f).setStartDelay(300).setDuration(SINGLE_ANIMATION_DURATION.toLong()).setInterpolator(AccelerateInterpolator()).start()
        mAddAudioFab!!.animate().translationX(0f).translationY(0f).alpha(0f).setStartDelay(200).setDuration(SINGLE_ANIMATION_DURATION.toLong()).setInterpolator(AccelerateInterpolator()).start()
        mAddPhotoFab!!.animate().translationX(0f).translationY(0f).alpha(0f).setStartDelay(100).setDuration(SINGLE_ANIMATION_DURATION.toLong()).setInterpolator(AccelerateInterpolator()).start()
        mAddVideoFab!!.animate().translationY(0f).alpha(0f).setStartDelay(0).setDuration(SINGLE_ANIMATION_DURATION.toLong()).setInterpolator(AccelerateInterpolator()).start()
    }

    private fun showInAnimation() {
        mIsAddButtonPressed = true
        mMaskView!!.visibility = View.VISIBLE
        val radius = DensityUtil.dip2px(mContext, 150f)
        mAddContentFab!!.animate().rotation(45f).scaleX(0.8.toFloat()).scaleY(0.8.toFloat()).setDuration(SINGLE_ANIMATION_DURATION.toLong()).setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                mAddContentFab!!.backgroundTintList = ColorStateList.valueOf(-0x7a7a7b)
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        }).setInterpolator(OvershootInterpolator()).start()
        mAddTextFab!!.animate().translationX(-radius.toFloat()).alphaBy(1f).setStartDelay(0).setDuration(SINGLE_ANIMATION_DURATION.toLong()).setInterpolator(OvershootInterpolator()).start()
        mAddAudioFab!!.animate().translationX((-(Math.sin(Math.PI / 3) * radius)).toFloat())
                .translationY((-(Math.cos(Math.PI / 3) * radius)).toFloat()).alphaBy(1f).setStartDelay(100).setDuration(SINGLE_ANIMATION_DURATION.toLong()).setInterpolator(OvershootInterpolator()).start()
        mAddPhotoFab!!.animate().translationX((-(Math.sin(Math.PI / 6) * radius)).toFloat())
                .translationY((-(Math.cos(Math.PI / 6) * radius)).toFloat()).alphaBy(1f).setStartDelay(200).setDuration(SINGLE_ANIMATION_DURATION.toLong()).setInterpolator(OvershootInterpolator()).start()
        mAddVideoFab!!.animate().translationY(-radius.toFloat()).alphaBy(1f).setStartDelay(300).setDuration(SINGLE_ANIMATION_DURATION.toLong()).setInterpolator(OvershootInterpolator()).start()
    }

    private fun hideAddButton() {
        mAddContentFab!!.animate().scaleX(0f).scaleY(0f).setDuration(SINGLE_ANIMATION_DURATION.toLong()).start()
    }

    private fun showAddButton() {
        mAddContentFab!!.animate().scaleX(1f).scaleY(1f).setDuration(SINGLE_ANIMATION_DURATION.toLong()).start()
    }
    //endregion

    //region 菜单控制
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        if (mIsInEditMode) {
            menu.findItem(R.id.action_search).isVisible = false
            menu.findItem(R.id.action_delete).isVisible = true
        } else {
            menu.findItem(R.id.action_search).isVisible = true
            menu.findItem(R.id.action_delete).isVisible = false
        }
        menu.findItem(R.id.action_fast_capture).isChecked = SharedPreferenceManager.getInstance(this).isAllowFastCapture()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var handled = false
        val id = item.itemId
        if (id == R.id.action_search) {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            handled = true
        } else if (id == R.id.action_delete) {
            mContentListFragment.checkDeleteContent()
            handled = true
        } else if (id == R.id.action_fast_capture) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(mContext)) {
                    if (mFloatingAlertDialog == null) {
                        mFloatingAlertDialog = AlertDialog.Builder(mContext!!)
                                .setTitle("无悬浮窗权限")
                                .setMessage("请前往设置-应用管理中为Capture开启显示悬浮窗权限")
                                .setPositiveButton("去设置") { dialog, which ->
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:$packageName"))
                                    startActivityForResult(intent, REQ_CODE_OVERLAY_PERMISSION)
                                }
                                .setNegativeButton("先等会") { dialog, which -> }
                                .create()
                    }
                    mFloatingAlertDialog!!.show()
                } else {
                    menuCheck(item)
                }
            } else {
                menuCheck(item)
            }
            handled = true
        }
        return handled || super.onOptionsItemSelected(item)
    }

    private fun menuCheck(item: MenuItem) {
        val showFloating = !item.isChecked
        item.isChecked = showFloating
        SharedPreferenceManager.getInstance(this).setAllowFastCapture(showFloating)
        toggleFloatWindow(showFloating)
    }


    private var mFloatBound = false
    var connFloat: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            LogUtil.d(TAG, "Floating service connected")
            mFloatBound = true
            myAidlInterface = IFloatAidlInterface.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            LogUtil.d(TAG, "Floating service disconnected")
            mFloatBound = false
        }
    }
    var myAidlInterface: IFloatAidlInterface? = null

    private fun toggleFloatWindow(toggle: Boolean) {
        if (myAidlInterface == null) {
            val intent1 = Intent(this, FloatingWindowService::class.java)
            startService(intent1)
            val intent = Intent(this, FloatingWindowService::class.java)
            intent.putExtra(FloatingWindowService.EXTRA_TOGGLE, toggle)
            bindService(intent, connFloat, BIND_AUTO_CREATE or BIND_ABOVE_CLIENT)
        } else {
            try {
                myAidlInterface!!.toggleFloatWindow(toggle)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    //endregion

    //region FloatingActionButton点击处理
    private val mAddContentClickListener = View.OnClickListener { v ->
        showOutAnimation()
        when (v.id) {
            R.id.fab_add_text -> gotoTextPublish()
            R.id.fab_add_audio -> if (getAudioPermissions()) {
                gotoAudioRecord()
            }
            R.id.fab_add_photo -> gotoTakePicture()
            R.id.fab_add_video -> if (getVideoPermissions()) {
                gotoVideoRecord()
            }
        }
    }

    private fun gotoTextPublish() {
        val intent = Intent(mContext, PublishActivity::class.java)
        startActivityForResult(intent, REQ_CODE_ADD_CONTENT)
    }

    private fun gotoAudioRecord() {
        val intent = Intent(mContext, AudioRecordActivity::class.java)
        startActivityForResult(intent, REQ_CODE_ADD_CONTENT)
    }

    private fun gotoTakePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        mCameraFilePath = FileUtil.getMediaFilePath(this, FileUtil.MEDIA_TYPE_PHOTO)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(File(mCameraFilePath)))
        } else {
            val contentValues = ContentValues(1)
            contentValues.put(MediaStore.Images.Media.DATA, mCameraFilePath)
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/*")
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        startActivityForResult(intent, REQ_CODE_TAKE_PICTURE)
    }

    private fun gotoVideoRecord() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        mCameraFilePath = FileUtil.getMediaFilePath(this, FileUtil.MEDIA_TYPE_VIDEO)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(File(mCameraFilePath)))
        } else {
            val contentValues = ContentValues(1)
            contentValues.put(MediaStore.Images.Media.DATA, mCameraFilePath)
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/*")
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        startActivityForResult(intent, REQ_CODE_VIDEO_CAPTURE)
    }
    //endregion

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (REQ_CODE_ADD_CONTENT == requestCode) {
            if (RESULT_OK == resultCode) {
//                mContentListFragment.getNewerContents()
            }
        } else if (REQ_CODE_TAKE_PICTURE == requestCode) {
            if (RESULT_OK == resultCode) {
                if (!TextUtils.isEmpty(mCameraFilePath)) {
                    val content = Content()
                    content.type = ConstantValues.CONTENT_TYPE_PHOTO
                    content.mediaFilePath = mCameraFilePath ?: ""
                    val intent = Intent(this, PublishActivity::class.java)
                    intent.putExtra(PublishActivity.INTENT_KEY_TYPE, ConstantValues.CONTENT_TYPE_PHOTO)
                    intent.putExtra(PublishActivity.INTENT_KEY_CONTENT, content)
                    startActivityForResult(intent, REQ_CODE_ADD_CONTENT)
                }
            }
        } else if (REQ_CODE_VIDEO_CAPTURE == requestCode) {
            if (RESULT_OK == resultCode) {
                val content = Content()
                content.type = ConstantValues.CONTENT_TYPE_VIDEO
                content.mediaFilePath = mCameraFilePath ?: ""
                val intent = Intent(this, PublishActivity::class.java)
                intent.putExtra(PublishActivity.INTENT_KEY_TYPE, ConstantValues.CONTENT_TYPE_VIDEO)
                intent.putExtra(PublishActivity.INTENT_KEY_CONTENT, content)
                startActivityForResult(intent, REQ_CODE_ADD_CONTENT)
            }
        }
    }

    override fun onBackPressed() {
        if (mIsAddButtonPressed) {
            showOutAnimation()
        } else if (mIsInEditMode) {
            exitEditMode()
            mContentListFragment.exitEditMode(true)
        } else {
            super.onBackPressed()
        }
    }

    override fun gotoEditMode() {
        mIsInEditMode = true
        // 改变菜单项
        invalidateOptionsMenu()
        // 隐藏添加按钮
        hideAddButton()
    }

    override fun exitEditMode() {
        mIsInEditMode = false
        // 改变菜单项
        invalidateOptionsMenu()
        // 显示添加按钮
        showAddButton()
    }
}