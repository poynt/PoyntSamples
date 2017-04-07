package com.sampleloyaltyapp;

import android.app.Application;

/**
 * Created by palavilli on 1/25/16.
 */
public class SampleLoyaltyApplication extends Application {
    public static SampleLoyaltyApplication instance;

    public static SampleLoyaltyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

}
