package com.android.system.function;

import com.android.system.utils.DataPack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileList {
    public void handle(InputStream inputStream, OutputStream outputStream) {
        while(true) {
            byte[] data = DataPack.receiveDataPack(inputStream);
            if(data == null) {
                break;
            }
            String dataString = new String(data);
            String path = "";
            String action = "";
            try {
                JSONObject dataObject = new JSONObject(dataString);
                path = dataObject.getString("path");
                action = dataObject.getString("action");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if(action.equals("ls")) {
                File filePath = new File(path);
                if(filePath.isDirectory()) {
                    File[] fileList = filePath.listFiles();
                    if (fileList == null || fileList.length == 0) {
                        DataPack.sendDataPack(outputStream, new JSONObject().toString().getBytes());
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
                        DataPack.sendDataPack(outputStream, new JSONObject().put("action", "ls").put("file_list", jsonArray).toString().getBytes());
                    } catch (JSONException e) {

                    }
                } else if(filePath.isFile()) {
                    try {
                        DataPack.sendDataPack(outputStream, new JSONObject().put("action", "file").put("length", filePath.length()).put("name", filePath.getName()).put("path",path).toString().getBytes());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
