package com.android.system.function;

import com.android.system.utils.DataPack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileDownload {
    public void handle(String path, OutputStream outputStream) {
        File file = new File(path);
        if(file.isFile()) {
            InputStream is = null;
            try {
                is = new FileInputStream(file);
//                int length = is.available();
//                DataPack.sendDataPack(outputStream, new JSONObject().put("action", "download").put("length", length).toString().getBytes());
                byte[] buffer = new byte[1024 * 1024 * 32];
                while(true) {
                    int readLength = is.read(buffer);
                    if(readLength <= 0) {
                        break;
                    }
                    outputStream.write(buffer,0,readLength);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
//            } catch (JSONException e) {
//                e.printStackTrace();
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
