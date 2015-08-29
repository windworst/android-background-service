package com.android.system.session;

import com.android.system.utils.DataPack;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private Map<String, SessionHandler> mSessionMap = new HashMap<>();

    public interface SessionHandler {
        void handleSession(String sessionName, InputStream inputStream, OutputStream outputStream);
    }

    public void handleSession(InputStream inputStream, OutputStream outputStream) {
        byte[] receiveData = DataPack.receiveDataPack(inputStream);
        if(receiveData == null) {
            return;
        }
        String action = null;
        try {
            action = new String(receiveData, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            action = new String(receiveData);
        }
        SessionHandler handler = getSessionHandler(action);
        if(handler!=null) {
            handler.handleSession(action, inputStream, outputStream);
        }
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
