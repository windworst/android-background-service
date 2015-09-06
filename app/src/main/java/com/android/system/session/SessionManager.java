package com.android.system.session;

import com.android.system.utils.DataPack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private Map<String, SessionHandler> mSessionMap = new HashMap<String, SessionHandler>();

    public interface SessionHandler {
        void handleSession(JSONObject responseJSON, InputStream inputStream, OutputStream outputStream);
    }

    public boolean handleSession(InputStream inputStream, OutputStream outputStream) {
        byte[] receiveData = DataPack.receiveDataPack(inputStream);
        if(receiveData == null) {
            return false;
        }
        String data = null;
        try {
            data = new String(receiveData, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            data = new String(receiveData);
        }
        JSONObject jsonObject = null;
        String action =  null;
        try {
            jsonObject = new JSONObject(data);
            action = jsonObject.getString("action");
        } catch (JSONException e) {
            return true;
        }
        SessionHandler handler = getSessionHandler(action);
        if(handler!=null) {
            handler.handleSession(jsonObject, inputStream, outputStream);
        }
        return true;
    }

    public void addSessionHandler(String sessionName, SessionHandler handler) {
        mSessionMap.put(sessionName, handler);
    }

    public void removeSessionHandler(String sessionName) {
        mSessionMap.remove(sessionName);
    }

    public void removeAllSessionHandler() {
        mSessionMap.clear();
    }

    public SessionHandler getSessionHandler(String sessionName) {
        return mSessionMap.get(sessionName);
    }

}
