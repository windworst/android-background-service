package com.android.system.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DataPack {
    public static boolean sendDataPack(OutputStream os, byte[] data) {
        int len = data.length;
        DataOutputStream dos = new DataOutputStream(os);
        try {
            dos.writeInt(0XEEFF);
            dos.writeInt(len);
            dos.write(data);
            dos.flush();
            return true;
        } catch (IOException e) {
        }
        return false;
    }

    public static byte[] receiveDataPack(InputStream is)
    {
        DataInputStream dis = new DataInputStream(is);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int Sign = dis.readInt();
            if(Sign!=0XEEFF) {
                return null;
            }
            int len = dis.readInt();
            int bufLen = 1024;
            byte[] data = new byte[bufLen];
            int i = 0;
            while(i<len) {
                int nRead = bufLen;
                if(nRead + i > len)
                {
                    nRead = len - i;
                }
                nRead = dis.read(data, 0, nRead);
                if(nRead <=0)
                {
                    continue;
                }
                i+= nRead;
                byteArrayOutputStream.write(data, 0, nRead);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
        }
        return null;
    }
}

