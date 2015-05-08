package com.android.system.utils;

public class Crypt {
    public static byte[] encrypt(byte data[]) {
        byte result[] = new byte[data.length];
        for(int i=0; i< data.length; ++i) {
            result[i] = (byte) (data[i]^0X88);
        }
        return result;
    }

    public static byte[] decrypt(byte data[]) {
        return encrypt(data);
    }
}
