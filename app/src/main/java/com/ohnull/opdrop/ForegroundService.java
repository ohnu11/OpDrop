package com.ohnull.opdrop;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.ohnull.opdrop.Helpers.CpuInfo;
import com.ohnull.opdrop.Requests.IRequestThread;
import com.ohnull.opdrop.Requests.RequestThreadFactory;
import com.ohnull.opdrop.Models.Config;
import com.ohnull.opdrop.Models.ExUrl;
import com.ohnull.opdrop.Utils.CommonUtils;
import com.ohnull.opdrop.Utils.EDebug;
import com.ohnull.opdrop.Helpers.SpeedTestHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ForegroundService extends Service {

    public static String ACTION_START = "com.ohnull.opdrop.service.ACTION_START";
    public static String ACTION_STOP = "com.ohnull.opdrop.service.ACTION_STOP";
    private static final int ID_SERVICE = 101;
    private SpeedTestHelper speedTestHelper = null;
    Thread JWorker = null;
    Config config = null;
    long lastSendToUIStatusTimestamp = System.currentTimeMillis();

    @Override
    public void onCreate() {
        updateForegroundNotification(
                getString(R.string.notificationMsg_initialTitle),
                getString(R.string.notificationMsg_preparingTask)
        );
        FirebaseApp.initializeApp(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        EDebug.l("ForegroundService::onStartCommand()");
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopGetMethodWithCookieWorker();
            return START_NOT_STICKY;
        } else if (intent != null && ACTION_START.equals(intent.getAction())) {
            prepareConfig();
            return START_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void prepareConfig() {
        try {
            sendMsgToUI("Fetching the config...");
            StorageReference islandRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://ohnull-opdrop.appspot.com/config.json");
            File configFile = File.createTempFile("config", "json");
            islandRef.getFile(configFile).addOnSuccessListener(taskSnapshot -> {
                EDebug.l("ForegroundService::getConfigFromTheServer::onSuccess()");
                File newConfigFile = new File(getApplicationContext().getExternalFilesDir(null), "/config.json");
                try {
                    CommonUtils.copyFileUsingStream(configFile, newConfigFile);
                } catch (IOException e) {
                    EDebug.l(e);
                }
                config = CommonUtils.readConfigFromFile(getApplicationContext());
                sendMsgToUI("Fresh config is here!");
                checkUpdates();
                startGetMethodWithCookieWorker();
            }).addOnFailureListener(exception -> {
                EDebug.l("ForegroundService::getConfigFromTheServer::onFailed()");
                EDebug.l(exception);
                config = CommonUtils.readConfigFromFile(getApplicationContext());
                sendMsgToUI("Cannot get fresh config... not a problem! I have previous one");
                startGetMethodWithCookieWorker();
            });
        }catch (Exception e){
            EDebug.l(e);
            sendMsgToUI("Cannot get fresh config... Maybe I have prev one?");
            config = CommonUtils.readConfigFromFile(getApplicationContext());
            if(config == null){
                sendMsgToUI("Hm... something wrong with the config. Cannot start a work :(");
            }
        }
    }

    private void checkUpdates() {
        Config config = CommonUtils.readConfigFromFile(getApplicationContext());
        if(config == null
                || config.getSettings().getLatestCodeVersion() == null
                || (config.getSettings().getLatestCodeVersion() <= BuildConfig.VERSION_CODE)) return;
        Bundle bundle = new Bundle();

        bundle.putString("state", "UPDATE_APP");
        CommonUtils.sendStateBroadcast(getApplicationContext(), bundle);
    }

    private void stopGetMethodWithCookieWorker() {
        if(JWorker != null) JWorker.interrupt();
        if(speedTestHelper != null) speedTestHelper.interrupt();
        stopForeground(true);
        stopSelf();
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    private synchronized void startGetMethodWithCookieWorker() {
        EDebug.l("ForegroundService::startJBrowserWorker()");
        if(config == null){
            EDebug.l("@ForegroundService::prepareConfig:: config == null");
            return;
        }
        if (speedTestHelper != null) speedTestHelper.interrupt();
        speedTestHelper = new SpeedTestHelper((dlMbps, ulMbps) -> {
            int cpuInfoFromFreq = CpuInfo.getCpuUsageFromFreq();
            boolean isConnectedWifi = CommonUtils.isConnectedWifi(getApplicationContext());
            String wifiStr = (isConnectedWifi) ? "✅ - " + getString(R.string.notificationMsg_wifi) + " | " : "⚠️ - " + getString(R.string.notificationMsg_cellularNetwork) + " | " ;
            String deviceStr = "⬇ " + dlMbps + " Mb/s | ⬆ " + ulMbps + " Mb/s | " + cpuInfoFromFreq + "%";
            String finalStr = wifiStr + deviceStr;
            if(System.currentTimeMillis() - lastSendToUIStatusTimestamp > 5000) {
                sendMsgToUI(finalStr);
                lastSendToUIStatusTimestamp = System.currentTimeMillis();
            }
            EDebug.l(finalStr);
            updateForegroundNotification("OpDrop - working... \uD83C\uDDFA\uD83C\uDDE6", finalStr);
        });
        speedTestHelper.start();

        if(JWorker != null) JWorker.interrupt();
        JWorker = new Thread(new ThreadGroup("WorkerGroup"), () -> {
            String rndUUID = UUID.randomUUID().toString();
            EDebug.l("ForegroundService::startJBrowserWorker(): START JWorker #" + rndUUID);
            sendMsgToUI("Started worker #" + rndUUID);
            List<Thread> workingThreads = new ArrayList<>();
            try {
                List<ExUrl> exUrlList = new ArrayList<>(config.getUrls());
                int threadsPerUrl = config.getMaxThreads() / exUrlList.size();

                while(!Thread.interrupted()) {
                    for (ExUrl exUrl : exUrlList) {
                        sendMsgToUI("Preparing " + exUrl.getMethod() + ": " + exUrl.getUrl());
                        if(exUrl.isMethod(ExUrl.METHOD_GET) || exUrl.isMethod(ExUrl.METHOD_POST)) {
                            runWebViewOnceToGetCookies(exUrl.getUrl());
                        }
                    }
                    EDebug.l("ForegroundService:startJBrowserWorker():webview launched...");
                    Thread.sleep(10000);
                    CookieManager cookieManager = CookieManager.getInstance();
                    for (ExUrl exUrl : exUrlList) {
                        String cookies = cookieManager.getCookie(exUrl.getUrl());
                        EDebug.l("ForegroundService:startJBrowserWorker():cookies: " + exUrl.getUrl() + " | " + cookies);
                        sendMsgToUI("Starting " + threadsPerUrl + " threads for " + exUrl.getUrl());
                        Thread t = startRequests(exUrl, threadsPerUrl, cookies);
                        t.start();
                        workingThreads.add(t);
                        Thread.sleep(1000);
                    }

                    sendMsgToUI("===== Let's goooooo! =====");

                    long finishTimestamp = System.currentTimeMillis() + (2 * 60 * 1000);
                    long lastTimestamp;
                    while(finishTimestamp > System.currentTimeMillis()){
                        lastTimestamp = System.currentTimeMillis();
                        Thread.sleep(100);
                        Config.IS_HIGH_CPU_USAGE.set(System.currentTimeMillis() - lastTimestamp > 110);
                    }

                    for(Thread thread : workingThreads) thread.interrupt();
                    workingThreads.clear();
                    sendMsgToUI("Finished all threads. Let's create a new wave!");
                }
            }catch (Exception e){
                EDebug.l(e);
            }
            for(Thread thread : workingThreads) thread.interrupt();
            sendMsgToUI("Finished worker #" + rndUUID);
            EDebug.l("ForegroundService::startJBrowserWorker(): FINISH JWorker #" + rndUUID);
        }, "JWorker", 256000);
        JWorker.start();
    }

    private void sendMsgToUI(String str){
        Bundle bundle = new Bundle();
        bundle.putString("state", "SERVICE_MSG");
        bundle.putString("MSG", str);
        CommonUtils.sendStateBroadcast(getApplicationContext(), bundle);
    }

    private void runWebViewOnceToGetCookies(String urlStr) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                WebView tempWebView = new WebView(getApplicationContext());
                tempWebView.getSettings().setSupportZoom(true);
                tempWebView.getSettings().setJavaScriptEnabled(true);
                tempWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        EDebug.l("ForegroundService::runWebViewOnceToGetCookies():onPageFinished():" + url);
                        view.destroy();
                    }
                });
                tempWebView.loadUrl(urlStr);
            }
        });
    }

    private void updateForegroundNotification(String title, String contentText) {
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        int priority = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? NotificationManager.IMPORTANCE_HIGH : Notification.PRIORITY_HIGH;
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_baseline_opacity_24)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(contentText))
                .setPriority(priority)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        notificationManager.notify(ID_SERVICE, notification);
        startForeground(ID_SERVICE, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager) {
        String channelId = "OpDrop Main";
        String channelName = "Main foreground service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Thread startRequests(ExUrl exUrl, int threads, String cookie) {
        return new Thread(new ThreadGroup("RequestsGroup"), () -> {
            String rndUUID = UUID.randomUUID().toString();
            EDebug.l("ForegroundService::startRequests(): START JWorkerThread #" + rndUUID);

            List<IRequestThread> threadList = new ArrayList<>();
            Random rnd = new Random();
            try{
                for(int i = 0; i < threads; i++){
                    IRequestThread t = RequestThreadFactory.create(exUrl, i, cookie);
                    if(t == null) continue;
                    t.startThread();
                    threadList.add(t);
                    Thread.sleep(rnd.nextInt(100));
                }

                while (!Thread.interrupted()) {
                    Thread.sleep(100);
                }
            }catch (Exception e){
                //EDebug.l(e);
            }finally {
                for(IRequestThread t : threadList){
                    t.stopThread();
                }
            }

            EDebug.l("ForegroundService::startRequests(): FINISHED JWorkerThread #" + rndUUID);
        }, "t_"+exUrl.getUrl(), 256000);
    }

    public interface IUICallback {
        void onMsg(String str);
    }
}
