package com.android.system.function;

import com.android.system.utils.DataPack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class FileList {
    public void handle(InputStream inputStream, OutputStream outputStream) {
        while(true) {
            byte[] data = DataPack.receiveDataPack(inputStream);
            if(data == null) {
                break;
            }
            File[] fileList = new File(new String(data)).listFiles();
            if(fileList == null || fileList.length == 0) {
                DataPack.sendDataPack(outputStream,new JSONObject().toString().getBytes());
                continue;
            }
            try {
                JSONArray jsonArray = new JSONArray();
                for (File file : fileList) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", file.getName());
                    jsonObject.put("type", file.isDirectory() ? 1 : 0);
                    jsonArray.put(jsonObject);
                }
                DataPack.sendDataPack(outputStream,new JSONObject().put("file_list",jsonArray).toString().getBytes());
            } catch (JSONException e) {

            }
        }
    }
}
