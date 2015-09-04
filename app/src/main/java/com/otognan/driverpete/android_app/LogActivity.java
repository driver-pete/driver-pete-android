package com.otognan.driverpete.android_app;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class LogActivity extends ActionBarActivity {


    private LogService logService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.setMovementMethod(new ScrollingMovementMethod());
    }

    public void screenLog(String message) {
        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.append(message);
    }

    public void clearLog() {
        ((TextView) findViewById(R.id.logTextView)).setText("");
    }

    @Override
    protected  void onPause() {
        super.onPause();
        unbindService(logServiceConnection);
    }

    @Override
    protected  void onResume() {
        super.onResume();

        registerReceiver(new BroadcastReceiver() {
                             @Override
                             public void onReceive(Context context, Intent intent) {
                                 String action = intent.getStringExtra("action");
                                 if (action.equals("add")) {
                                     String message = intent.getStringExtra("message");
                                     LogActivity.this.screenLog(message);
                                 } else if (action.equals("clear")) {
                                     LogActivity.this.clearLog();
                                 }
                             }
                         },
                new IntentFilter(LogService.LOG_INTENT_ACTION_NAME));

        Intent intent= new Intent(this, LogService.class);
        bindService(intent, logServiceConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection logServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            LogService.LogServiceBinder b = (LogService.LogServiceBinder) binder;
            logService = b.getService();
            for(String message: logService.getLogs()) {
                LogActivity.this.screenLog(message);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            logService = null;
        }
    };
}
