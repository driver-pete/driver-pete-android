package com.otognan.driverpete.android;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

public class LogService extends Service {

    private final IBinder logServiceBinder = new LogServiceBinder();
    private ArrayList<String> logs = new ArrayList<String>();

    public static String LOG_INTENT_ACTION_NAME = "com.otognan.driverpete.android.LOG_INTENT";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return logServiceBinder;
    }

    public class LogServiceBinder extends Binder {
        LogService getService() {
            return LogService.this;
        }
    }

    public void addLog(String message) {
        this.logs.add(message);
        Intent intent = new Intent();
        intent.setAction(LOG_INTENT_ACTION_NAME);
        intent.putExtra("action", "add");
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    public List<String> getLogs() {
        return this.logs;
    }

    public void clearLog() {
        this.logs.clear();
        Intent intent = new Intent();
        intent.setAction(LOG_INTENT_ACTION_NAME);
        intent.putExtra("action", "clear");
        sendBroadcast(intent);
    }
}
