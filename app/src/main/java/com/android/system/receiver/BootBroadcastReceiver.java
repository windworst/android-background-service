package com.android.system.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.android.system.service.SystemService;

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SystemService.start(context);
        try {
            context.unregisterReceiver(this);
        } catch (Exception e) {
        }
    }
}
