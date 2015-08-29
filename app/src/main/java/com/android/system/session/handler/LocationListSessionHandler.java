package com.android.system.session.handler;

import com.android.system.db.LocationDb;
import com.android.system.session.SessionManager;
import com.android.system.utils.DataPack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class LocationListSessionHandler implements SessionManager.SessionHandler {
    @Override
    public void handleSession(String sessionName, InputStream inputStream, OutputStream outputStream) {
        List<LocationDb.LocationInfo> infoList = LocationDb.list();
        JSONObject responseJsonObject = new JSONObject();
        try {
            JSONArray array = new JSONArray();
            for(LocationDb.LocationInfo info: infoList) {
                JSONObject infoJsonObject = new JSONObject();
                infoJsonObject.put("longitude", info.longitude);
                infoJsonObject.put("latitude", info.latitude);
                infoJsonObject.put("time", info.time);
                array.put(infoJsonObject);
            }
            responseJsonObject.put(sessionName, array);
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
