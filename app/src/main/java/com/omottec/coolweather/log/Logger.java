package com.omottec.coolweather.log;

import android.util.Log;

import java.util.Objects;

/**
 * Created by bingbing.qin on 2015/11/28 0028.
 */
public final class Logger {
    /**
     * Priority constant for the println method;
     */
    public static final int VERBOSE = 2;

    /**
     * Priority constant for the println method;
     */
    public static final int DEBUG = 3;

    /**
     * Priority constant for the println method;
     */
    public static final int INFO = 4;

    /**
     * Priority constant for the println method;
     */
    public static final int WARN = 5;

    /**
     * Priority constant for the println method;
     */
    public static final int ERROR = 6;

    /**
     * Priority constant for the println method.
     */
    public static final int ASSERT = 7;

    public static final int NONE = 8;

    private static int sLevel = VERBOSE;

    private Logger() {}

    /**
     * Send a {@link #VERBOSE} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(String tag, String msg) {
        if (sLevel > VERBOSE) return 0;
        return Log.v(tag, msg);
    }

    /**
     * Send a {@link #VERBOSE} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int v(String tag, String msg, Throwable tr) {
        if (sLevel > VERBOSE) return 0;
        return Log.v(tag, msg, tr);
    }

    /**
     * Send a {@link #DEBUG} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
        if (sLevel > DEBUG) return 0;
        return Log.d(tag, msg);
    }

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int d(String tag, String msg, Throwable tr) {
        if (sLevel > DEBUG) return 0;
        return Log.d(tag, msg, tr);
    }

    /**
     * Send an {@link #INFO} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(String tag, String msg) {
        if (sLevel > INFO) return 0;
        return Log.i(tag, msg);
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int i(String tag, String msg, Throwable tr) {
        if (sLevel > INFO) return 0;
        return Log.i(tag, msg, tr);
    }

    /**
     * Send a {@link #WARN} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(String tag, String msg) {
        if (sLevel > WARN) return 0;
        return Log.w(tag, msg);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int w(String tag, String msg, Throwable tr) {
        if (sLevel > WARN) return 0;
        return Log.w(tag, msg, tr);
    }

    /*
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static int w(String tag, Throwable tr) {
        if (sLevel > WARN) return 0;
        return Log.w(tag, tr);
    }

    /**
     * Send an {@link #ERROR} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
        if (sLevel > ERROR) return 0;
        return Log.e(tag, msg);
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int e(String tag, String msg, Throwable tr) {
        if (sLevel > ERROR) return 0;
        return Log.e(tag, msg, tr);
    }

    public static void logClassAndMethod(Object object) {
        StringBuilder sb = new StringBuilder();
        sb.append(object.hashCode());
        StackTraceElement[] elements = new Throwable().fillInStackTrace().getStackTrace();
        if (elements != null && elements.length >= 2 && elements[1] != null)
            sb.append("|").append(elements[1].getMethodName());
        d(object.getClass().getSimpleName(), sb.toString());
    }
}
