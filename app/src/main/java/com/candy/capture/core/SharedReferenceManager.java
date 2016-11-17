package com.candy.capture.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by zhanghq on 2016/11/11.
 */

public class SharedReferenceManager {

    private static SharedReferenceManager instance;
    private SharedReferenceManager(Context context) {
        mContext = context;
        init();
    }

    public static SharedReferenceManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SharedReferenceManager.class) {
                if (instance == null) {
                    instance = new SharedReferenceManager(context);
                }
            }
        }
        return instance;
    }

    private Context mContext;
    private SharedPreferences mSharedPreferences;

    private String mLocationCity;

    private static final String KEY_LOCATION_CITY = "key_location_city";

    private void init() {
        mSharedPreferences = mContext.getSharedPreferences("capture", 0);

        mLocationCity = mSharedPreferences.getString(KEY_LOCATION_CITY, ConstantValues.DEFAULT_CITY_NAME);
    }

    public String getLocationCity() {
        return mLocationCity;
    }

    public void setLocationCity(String locationCity) {
        if (!mLocationCity.equals(locationCity)) {
            mLocationCity = locationCity;
            mSharedPreferences.edit().putString(KEY_LOCATION_CITY, mLocationCity).apply();
        }
    }
}
