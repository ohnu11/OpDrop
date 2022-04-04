package com.ohnull.opdrop.Utils;

import android.util.Log;

import com.ohnull.opdrop.BuildConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;


final public class EDebug {
    private final static String TAG = "APP_LOGS";
    private static final int chunkSize = 2048;

//    public synchronized static void logCrashlytics(Exception e) {
//        try {
//            FirebaseHelper.crashlyticsRecordException(e);
//        }catch (Throwable ex){
//            EDebug.l("@ logCrashlytics::Exception:" + ex.getMessage());
//        }
//    }

    public synchronized static void l(String message, Object... args) {
        if (args != null && args.length > 0) {
            message = String.format(Locale.US, message, args);
        }
        l(message);
    }

    public synchronized static void l(Throwable t, String message, Object... args) {
        if (t != null)
            l(getStackTraceString(t));
        l(message, args);
    }

    public synchronized static void l(Throwable t, String message) {
        if (t != null)
            l(getStackTraceString(t));
        l(message);
    }

    public synchronized static void l(Throwable t) {
        l(getStackTraceString(t));
    }

    public synchronized static void l(String message) {
        if(!BuildConfig.DEBUG) return;
        if(message == null || message.isEmpty()) return;
        for (int i = 0; i < message.length(); i += chunkSize) {
            Log.d(TAG, message.substring(i, Math.min(message.length(), i + chunkSize)));
        }
    }

    public static String getStackTraceString(Throwable t) {
        StringWriter sw = new StringWriter(256);
        PrintWriter pw = new PrintWriter(sw, false);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}