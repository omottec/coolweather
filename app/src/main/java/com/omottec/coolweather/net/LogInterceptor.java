package com.omottec.coolweather.net;

import android.view.animation.Interpolator;

import com.omottec.coolweather.log.Logger;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by qinbingbing on 7/13/16.
 */
public final class LogInterceptor implements Interceptor {
    public static final String TAG = "OkHttp";
    @Override
    public Response intercept(Chain chain) throws IOException {
        long t = System.currentTimeMillis();
        Request request = chain.request();
        Logger.d(TAG, String.format("------>%s on %s%n%s",
                request,
                chain.connection(),
                request.headers()));
        Response response = chain.proceed(request);
        long t1 = System.currentTimeMillis();
        Logger.d(TAG, String.format("<------%s in %dms%n%s",
                response,
                t1 - t,
                response.headers()));
        return response;
    }
}
