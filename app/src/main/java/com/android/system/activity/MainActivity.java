package com.android.system.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.system.service.SystemService;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                startActivity(new Intent(MainActivity.this,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
        SystemService.start(this);
        finish();
    }
}