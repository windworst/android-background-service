package com.android.system.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.android.system.utils.AudioRecorder;

public class AudioRecorderService extends Service {
    private static final String SAVE_PATH = "SAVE_PATH";
    private static Intent sIntent;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String path = intent.getExtras().getString(SAVE_PATH);
        AudioRecorder.start(path);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        AudioRecorder.stop();
        super.onDestroy();
    }

    public static void start(Context context, String savePath) {
        Log.i("RECORD", "start service");
        if(sIntent != null) return;
        context.startService(sIntent = new Intent(context,AudioRecorderService.class).putExtra(SAVE_PATH,savePath));
    }

    public static void stop(Context context) {
        Log.i("RECORD", "stop service");
        if(sIntent == null) return;
        context.stopService(sIntent);
        sIntent = null;
    }
}
