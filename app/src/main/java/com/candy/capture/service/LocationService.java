package com.candy.capture.service;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.candy.capture.core.SharedPreferenceManager;
import com.candy.capture.util.LogUtil;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class LocationService extends Service implements BDLocationListener {
    private static final String TAG = "LocationService";

    private LocationClient client = null;
    private LocationClientOption mOption;

    public LocationService() {
        super();
        if (client == null) {
            client = new LocationClient(this);
            client.setLocOption(getDefaultLocationClientOption());
        }
    }

    /***
     * @return DefaultLocationClientOption
     */
    public LocationClientOption getDefaultLocationClientOption() {
        if (mOption == null) {
            mOption = new LocationClientOption();
            mOption.setLocationMode(LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
//			mOption.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
//			mOption.setScanSpan(3000);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
            mOption.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
            mOption.setIsNeedLocationDescribe(true);//可选，设置是否需要地址描述
//		    mOption.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
//		    mOption.setLocationNotify(false);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            mOption.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
//		    mOption.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
//		    mOption.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
//		    mOption.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集

//			mOption.setIsNeedAltitude(false);//可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        }
        return mOption;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.d(TAG, "onBind");
        if (client != null && !client.isStarted()) {
            client.registerLocationListener(this);
            client.start();
        }
        return null;
    }

    @Override
    public void onDestroy() {
        LogUtil.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d(TAG, "onStartCommand");
        if (client != null && !client.isStarted()) {
            client.registerLocationListener(this);
            client.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        LogUtil.d(TAG, "onReceiveLocation");
        String city = bdLocation.getCity();
        SharedPreferenceManager.getInstance(this).setLocationCity(city);
        if (client != null) {
            if (client.isStarted()) {
                LogUtil.d(TAG, "onReceiveLocation client.stop()");
                client.stop();
            }
            client.unRegisterLocationListener(this);
        }
//        stopSelf();
    }
}
