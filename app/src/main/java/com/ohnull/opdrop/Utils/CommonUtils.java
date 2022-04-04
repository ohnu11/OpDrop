package com.ohnull.opdrop.Utils;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.ohnull.opdrop.Models.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommonUtils {

    public static final String[] USER_AGENTS = new String[]{
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95",
            "Mozilla/5.0 (Windows NT 5.1; U; en; rv:1.8.1) Gecko/20061208 Firefox/2.0.0 Opera 9.50",
            "Mozilla/5.0 (X11; U; Linux x86_64; zh-CN; rv:1.9.2.10) Gecko/20100922 Ubuntu/10.10 (maverick) Firefox/3.6.10",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/534.57.2 (KHTML, like Gecko) Version/5.1.7",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71",
            "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/534.16 (KHTML, like Gecko)",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.71",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 732; .NET4.0C; .NET4.0E)",
            "Mozilla/5.0 (Linux; U; Android 2.2.1; zh-cn; HTC_Wildfire_A3333 Build/FRG83D) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1",
            "Opera/9.80 (Android 2.3.4; Linux; Opera Mobi/build-1107180945; U; en-GB) Presto/2.8.149 Version/11.10",
            "Mozilla/5.0 (hp-tablet; Linux; hpwOS/3.0.0; U; en-US) AppleWebKit/534.6 (KHTML, like Gecko) wOSBrowser/233.70 Safari/534.6 TouchPad/1.0",
            "Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; HTC; Titan)",
            "Mozilla/5.0 (iPad; U; CPU OS 4_2_1 like Mac OS X; zh-cn) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8C148 Safari/6533.18.5",
            "AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1"
    };

    public static String generateRandomString(int length) {
        String ALLOWED_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(length);
        Random rnd = new Random();
        int allowedCharsLength = ALLOWED_CHARS.length();
        for (int i = 0; i < length; i++)
            sb.append(ALLOWED_CHARS.charAt(rnd.nextInt(allowedCharsLength)));
        return sb.toString();
    }

    public static boolean checkIfWifiSupplicantStateConnecting(SupplicantState state) {
        switch(state) {
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
            case COMPLETED:
                return true;
            case DISCONNECTED:
            case INTERFACE_DISABLED:
            case INACTIVE:
            case SCANNING:
            case DORMANT:
            case UNINITIALIZED:
            case INVALID:
            default:
                return false;
        }
    }

    public static void sendStateBroadcast(Context context, Bundle bundle){
        Intent intent = new Intent("com.ohnull.opdrop.STATE");
        intent.putExtras(bundle);
        context.getApplicationContext().sendBroadcast(intent);
    }

    public static boolean isConnectedWifi(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (cm.getActiveNetwork() != null) {
                    NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                    return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                } else {
                    WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wifiMgr.isWifiEnabled()) {
                        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                        SupplicantState wifiState = wifiInfo.getSupplicantState();
                        return wifiInfo.getNetworkId() != -1 || checkIfWifiSupplicantStateConnecting(wifiState);
                    } else {
                        return false;
                    }
                }
            } else {
                NetworkInfo info = cm.getActiveNetworkInfo();
                if (info == null) {
                    WifiManager wifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wifiMgr.isWifiEnabled()) {
                        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                        SupplicantState wifiState = wifiInfo.getSupplicantState();
                        return wifiInfo.getNetworkId() != -1 || checkIfWifiSupplicantStateConnecting(wifiState);
                    } else {
                        return false;
                    }
                } else {
                    return info.getType() == ConnectivityManager.TYPE_WIFI;
                }
            }
        }catch (Exception e){
            EDebug.l(e);
        }
        return false;
    }

    public static double roundDouble(double value, int places) {
        String pattern = "#.";
        try{
            for(int i = 0; i < places; i++){
                pattern += "#";
            }
            return Double.parseDouble(new DecimalFormat(pattern, new DecimalFormatSymbols(Locale.US)).format(value));
        }catch (Exception e){
            EDebug.l(e);
        }
        return value;
    }

    public static void getIP(Context context, ICallback callback){
        getIP(context, callback, 0);
    }

    private static void getIP(Context context, ICallback callback, int retry){
        EDebug.l("CommonUtils::getIP()");
        String[] URLS = new String[]{
                "http://api.ipify.org/",
                "http://ipinfo.io/ip",
                "https://ipecho.net/plain"
        };

        if(retry >= URLS.length){
            callback.onComplete(null);
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URLS[retry])
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                getIP(context, callback, retry + 1);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try{
                    String ipStr = response.body().string();
                    EDebug.l("CommonUtils::getIP():ipStr -> " + ipStr);
                    callback.onComplete(ipStr);
                }catch (Exception e){
                    EDebug.l(e);
                    getIP(context, callback, retry + 1);
                }
            }
        });
    }

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    public static Config readConfigFromFile(Context context){
        Config config = null;
        try {
            File configLocalFile = new File(context.getExternalFilesDir(null) + "/config.json");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(configLocalFile));
            Gson gson = new Gson();
            config = gson.fromJson(bufferedReader, Config.class);
            EDebug.l("ForegroundService::readConfigFromFile::config -> " + config);
        }catch (Exception e){
            EDebug.l(e);
        }
        return config;
    }

    public interface ICallback{
        void onComplete(Object obj);
    }

}
