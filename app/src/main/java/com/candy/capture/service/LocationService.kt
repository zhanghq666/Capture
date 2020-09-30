package com.candy.capture.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import com.baidu.location.BDLocation
import com.baidu.location.BDLocationListener
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.location.LocationClientOption.LocationMode
import com.candy.capture.core.SharedPreferenceManager
import com.candy.commonlibrary.utils.LogUtil

/**
 * @Description
 * @Author zhanghaiqiang
 * @Date 2020/9/30 15:41
 */
class LocationService: Service(), BDLocationListener {
    private val TAG = "LocationService"

    private val client: LocationClient = LocationClient(this)
    private var mOption: LocationClientOption? = null

    init {
        client.locOption = getDefaultLocationClientOption()
    }

    /***
     * @return DefaultLocationClientOption
     */
    fun getDefaultLocationClientOption(): LocationClientOption? {
        if (mOption == null) {
            mOption = LocationClientOption()
            mOption!!.locationMode = LocationMode.Hight_Accuracy //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            mOption!!.isOpenGps = true
            //			mOption.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
//			mOption.setScanSpan(3000);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
            mOption!!.setIsNeedAddress(true) //可选，设置是否需要地址信息，默认不需要
            mOption!!.setIsNeedLocationDescribe(true) //可选，设置是否需要地址描述
            //		    mOption.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
//		    mOption.setLocationNotify(false);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            mOption!!.setIgnoreKillProcess(false) //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
            //		    mOption.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
//		    mOption.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
//		    mOption.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集

//			mOption.setIsNeedAltitude(false);//可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        }
        return mOption
    }

    override fun onBind(intent: Intent?): IBinder? {
        LogUtil.d(TAG, "onBind")
        if (!client.isStarted) {
            client.registerLocationListener(this)
            client.start()
        }
        return null
    }

    override fun onDestroy() {
        LogUtil.d(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtil.d(TAG, "onStartCommand")
        if (!client.isStarted) {
            client.registerLocationListener(this)
            client.start()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onReceiveLocation(bdLocation: BDLocation) {
        LogUtil.d(TAG, "onReceiveLocation " + bdLocation.addrStr)
        val city = bdLocation.city
        if (!TextUtils.isEmpty(city)) {
            SharedPreferenceManager.getInstance(this).setLocationCity(city)
            if (client.isStarted) {
                LogUtil.d(TAG, "onReceiveLocation client.stop()")
                client.stop()
            }
            client.unRegisterLocationListener(this)
            stopSelf()
        } else {
            Log.w(TAG, bdLocation.locTypeDescription)
        }
    }
}