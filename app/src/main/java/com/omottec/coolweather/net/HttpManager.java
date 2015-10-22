package com.omottec.coolweather.net;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.omottec.coolweather.app.CWApplication;

/**
 * Created by bingbing.qin on 2015/10/22 0022.
 */
public enum  HttpManager {
    INSTANCE;

    private static final RequestQueue mRequestQueue = Volley.newRequestQueue(CWApplication.getAppContext());

    public static RequestQueue getRequestQueue() {
        return mRequestQueue;
    }
}
