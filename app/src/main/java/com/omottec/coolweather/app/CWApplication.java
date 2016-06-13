package com.omottec.coolweather.app;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.activeandroid.ActiveAndroid;
import com.omottec.coolweather.BuildConfig;

/**
 * Created by bingbing.qin on 2015/10/22 0022.
 */
public class CWApplication extends Application {
    private static Application mAppContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = this;
        ActiveAndroid.initialize(this, BuildConfig.DEBUG);
    }

    public static Application getAppContext() {
        return mAppContext;
    }
}
