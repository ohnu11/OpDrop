package com.ohnull.opdrop;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;


import com.ohnull.opdrop.Utils.EDebug;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {
    public static final String CUSTOM_INTENT = "com.ohnull.opdrop.action.ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        EDebug.l("AlarmReceiver:" + intent.toString());
        context = context.getApplicationContext();

        if(context.getSharedPreferences("MAIN", Context.MODE_PRIVATE).getBoolean("IS_ATTACK", false)) {
            Intent serviceIntent = new Intent(context, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }

        setAlarm(context);
    }

    public static void cancelAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            EDebug.l("@ AlarmReceiver::cancelAlarm: alarmManager == null");
            return;
        }
        alarmManager.cancel(getPendingIntent(context));
    }

    public static void setAlarm(Context context) {
        cancelAlarm(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(alarmManager == null){
            EDebug.l("@ AlarmReceiver::setAlarm: alarmManager == null");
            return;
        }

        try {
            PendingIntent pendingIntent = getPendingIntent(context);
            long wakeUpInterval = AlarmManager.INTERVAL_HALF_HOUR / 2;

            Calendar updateTime = Calendar.getInstance();
            long nextWakeUpTime = updateTime.getTimeInMillis() + wakeUpInterval;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(nextWakeUpTime, pendingIntent), pendingIntent);
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pendingIntent);
            }else{
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextWakeUpTime, pendingIntent);
            }

        }catch (Exception e){
            EDebug.l(e);
        }
        EDebug.l("AlarmReceiver::setRepeating()");
    }

    private static PendingIntent getPendingIntent(Context context) {
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.setAction(CUSTOM_INTENT);
        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            flag = flag | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, 0, alarmIntent, flag);
    }
}
