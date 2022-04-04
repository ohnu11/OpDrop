package com.ohnull.opdrop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ohnull.opdrop.Models.Config;
import com.ohnull.opdrop.Utils.CommonUtils;
import com.ohnull.opdrop.Utils.EDebug;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Button startBtn;
    Button stopBtn;
    Button refreshIpBtn;
    TextView textViewIp;
    TextView textViewServiceMessages;
    ImageView imageViewStatus;
    ProgressBar progressBar;
    ConstraintLayout welcomeContainer;
    boolean isUIActive = false;
    SharedPreferences sharedPreferences;
    BroadcastReceiver serviceStatesReceiver = null;
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewServiceMessages = findViewById(R.id.textView_serviceMessages);
        startBtn = findViewById(R.id.button_start);
        stopBtn = findViewById(R.id.button_stop);
        refreshIpBtn = findViewById(R.id.button_refreshIP);
        textViewIp = findViewById(R.id.textView_ip);
        imageViewStatus = findViewById(R.id.imageView_serviceStatus);
        progressBar = findViewById(R.id.progressBar);
        welcomeContainer = findViewById(R.id.constraintLayout_welcomeScreen);
        sharedPreferences = getSharedPreferences("MAIN", Context.MODE_PRIVATE);
        AlarmReceiver.setAlarm(this);
        initClicks();

        boolean isAttack = sharedPreferences.getBoolean("IS_ATTACK", false);
        if(isAttack) startMainService(ForegroundService.ACTION_START);
        changeStatusIndicator(isAttack);
        checkUpdates();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                showBatteryOptimizationDialog();
            }
        }
    }

    private void listenServiceMessages() {
        serviceStatesReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    if (intent != null && intent.getExtras() != null && intent.getExtras().getString("state", null) != null) {
                        String state = intent.getExtras().getString("state", "");
                        if (state.contentEquals("SERVICE_MSG")) {
                            Bundle bundle = intent.getExtras();
                            String msg = bundle.getString("MSG", null);
                            if(msg != null) printMessage(msg);
                        }else if(state.contentEquals("UPDATE_APP")){
                            checkUpdates();
                        }
                    }
                }catch (Exception e){
                    EDebug.l(e);
                }
            }
        };
        registerReceiver(serviceStatesReceiver, new IntentFilter("com.ohnull.opdrop.STATE"));
    }

    private void showBatteryOptimizationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.batteyOptimizationDialog_title);
        builder.setMessage(R.string.batteyOptimizationDialog_msg);
        builder.setPositiveButton("OK", (dialog, which) -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            dialog.dismiss();
        });
        builder.setCancelable(true);
        builder.show();
    }

    private void checkUpdates() {
        new Thread(() -> {
            Config config = CommonUtils.readConfigFromFile(getApplicationContext());
            if(config == null
                    || config.getSettings().getLatestCodeVersion() == null
                    || (config.getSettings().getLatestCodeVersion() <= BuildConfig.VERSION_CODE)) return;
            runOnUiThread(() -> showUpdateDialog(config.getSettings().getUpdateDialogText()));
        }).start();
    }

    private void showUpdateDialog(String strMsg){
        float dip = 16f;
        int paddingInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());

        TextView showText = new TextView(this);
        showText.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);
        showText.setText(Html.fromHtml(strMsg));
        showText.setTextIsSelectable(true);
        showText.setMovementMethod(LinkMovementMethod.getInstance());
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.updateDialog_title);
        builder.setView(showText);
        builder.setCancelable(true);
        builder.show();
    }

    private void changeStatusIndicator(boolean isActive){
        if(isActive){
            imageViewStatus.setImageResource(R.drawable.ic_baseline_dot_green_24);
        }else{
            imageViewStatus.setImageResource(R.drawable.ic_baseline_dot_red_24);
        }
    }

    private void startMainService(String action) {
        Intent serviceIntent = new Intent(MainActivity.this, ForegroundService.class);
        if(action != null) serviceIntent.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void initClicks() {
        startBtn.setOnClickListener(v -> {
            sharedPreferences
                    .edit()
                    .putBoolean("IS_ATTACK", true)
                    .apply();
            startMainService(ForegroundService.ACTION_START);
            hideView(welcomeContainer);
            changeStatusIndicator(true);
        });
        stopBtn.setOnClickListener(v -> {
            sharedPreferences
                    .edit()
                    .putBoolean("IS_ATTACK", false)
                    .apply();

            startMainService(ForegroundService.ACTION_STOP);
            changeStatusIndicator(false);
        });
        refreshIpBtn.setOnClickListener(v -> {
            printIp("---");
            CommonUtils.getIP(MainActivity.this, obj -> {
                if(obj != null) printIp(obj.toString());
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenServiceMessages();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (serviceStatesReceiver != null) unregisterReceiver(serviceStatesReceiver);
        } catch (Exception e) {
            EDebug.l(e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isUIActive = true;
        CommonUtils.getIP(this, obj -> {
            if(obj != null) printIp(obj.toString());
        });
    }

    private void printIp(String ip) {
        runOnUiThread(() -> textViewIp.setText("Your IP: " + ip));
    }

    private void printMessage(String msg) {
        runOnUiThread(() -> {
            try {
                hideView(welcomeContainer);
                String dateTime = sdf.format(new Date(System.currentTimeMillis()));
                String viewText = "\n" + textViewServiceMessages.getText().toString();
                if (viewText.length() > 10000) {
                    viewText = "";
                }
                textViewServiceMessages.setText("[" + dateTime + "] " + msg + viewText);
            }catch (Exception e){
                EDebug.l(e);
            }
        });
    }

    private void hideView(View view){
        runOnUiThread(() -> view.setVisibility(View.GONE));
    }

    private void showView(View view){
        runOnUiThread(() -> view.setVisibility(View.VISIBLE));
    }

}