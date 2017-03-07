package com.candy.capture.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by zhanghq on 2016/11/11.
 */

public class SharedPreferenceManager {

    private static SharedPreferenceManager instance;
    private SharedPreferenceManager(Context context) {
        mContext = context;
        init();
    }

    public static SharedPreferenceManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SharedPreferenceManager.class) {
                if (instance == null) {
                    instance = new SharedPreferenceManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private Context mContext;
    private SharedPreferences mSharedPreferences;

    private String mLocationCity;
    private boolean mAllowFastCapture;
    private boolean mIsFirstRun;

    private static final String KEY_LOCATION_CITY = "key_location_city";
    private static final String KEY_FAST_CAPTURE = "key_fast_capture";
    private static final String KEY_IS_FIRST_RUN = "key_is_first_run";

    private void init() {
        mSharedPreferences = mContext.getSharedPreferences("capture", 0);

        mLocationCity = mSharedPreferences.getString(KEY_LOCATION_CITY, ConstantValues.DEFAULT_CITY_NAME);
        mAllowFastCapture = mSharedPreferences.getBoolean(KEY_FAST_CAPTURE, false);
        mIsFirstRun = mSharedPreferences.getBoolean(KEY_IS_FIRST_RUN, true);
    }

    public String getLocationCity() {
        return mLocationCity;
    }

    public void setLocationCity(String locationCity) {
        synchronized (SharedPreferenceManager.class) {
            if (!mLocationCity.equals(locationCity)) {
                mLocationCity = locationCity;
                mSharedPreferences.edit().putString(KEY_LOCATION_CITY, mLocationCity).apply();
            }
        }
    }

    public boolean isAllowFastCapture() {
        return mAllowFastCapture;
    }

    public void setAllowFastCapture(boolean allowFastCapture) {
        synchronized (SharedPreferenceManager.class) {
            mAllowFastCapture = allowFastCapture;
            mSharedPreferences.edit().putBoolean(KEY_FAST_CAPTURE, mAllowFastCapture).apply();
        }
    }

    public boolean isFirstRun() {
        return mIsFirstRun;
    }

    public void setFirstRun(boolean firstRun) {
        synchronized (SharedPreferenceManager.class) {
            mIsFirstRun = firstRun;
            mSharedPreferences.edit().putBoolean(KEY_IS_FIRST_RUN, mIsFirstRun).apply();
        }
    }
}
