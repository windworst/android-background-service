package com.android.system.utils;

public class Tea {

    public static byte[] encrypt(byte[] data, byte[] key) {
        int data_len = data.length;
        if (data_len == 0) {
            return new byte[] {};
        }
        Tea t = new Tea();
        if (!t.setKey(key)) {
            return new byte[] {};
        }

        int group_len = 8;
        int residues = data_len % group_len; // 余数
        int dlen = data_len - residues;

        // 用于储存加密的密文，第一字节为余数的大小
        int result_len = data_len + 1;
        if (residues > 0) {
            result_len += group_len - residues;
        }
        byte[] result = new byte[result_len];
        result[0] = (byte)residues;

        byte[] plain = new byte[group_len];
        byte[] enc = new byte[group_len];

        for (int i = 0; i < dlen; i += group_len) {
            for (int j = 0; j < group_len; j++) {
                plain[j] = data[i + j];
            }
            enc = t.encrypt_group(plain);
            for (int k = 0; k < group_len; k++) {
                result[i + k + 1] = enc[k];
            }
        }
        if (residues > 0) {
            for (int j = 0; j < residues; j++) {
                plain[j] = data[dlen + j];
            }
            int padding = group_len - residues;
            for (int j = 0; j < padding; j++) {
                plain[residues + j] = (byte)0x00;
            }
            enc = t.encrypt_group(plain);
            for (int k = 0; k < group_len; k++) {
                result[dlen + k + 1] = enc[k];
            }
        }
        return result;
    }

    public static byte[] decrypt(byte[] data, byte[] key) {
        int group_len = 8;
        if (data.length % group_len != 1) {
            return new byte[] {};
        }
        Tea t = new Tea();
        if (!t.setKey(key)) {
            return new byte[] {};
        }
        int data_len = data.length - 1, dlen;
        int residues = (int)(data[0]); // 余数
        if (residues > 0) {
            dlen = data_len - group_len;
        } else {
            dlen = data_len;
        }

        byte[] result = new byte[dlen + residues];

        byte[] dec = new byte[group_len];
        byte[] enc = new byte[group_len];
        for (int i = 0; i < dlen; i += group_len) {
            for (int j = 0; j < group_len; j++) {
                enc[j] = data[i + j + 1];
            }
            dec = t.decrypt_group(enc);
            for (int k = 0; k < group_len; k++) {
                result[i + k] = dec[k];
            }
        }
        if (residues > 0) {
            for (int j = 0; j < group_len; j++) {
                enc[j] = data[dlen + j + 1];
            }
            dec = t.decrypt_group(enc);
            for (int k = 0; k < residues; k++) {
                result[dlen + k] = dec[k];
            }
        }
        return result;
    }

    public boolean setKey(byte[] k) {
        if (k.length != 16) {
            return false;
        }
        k0 = bytes_to_uint32(new byte[] {k[0], k[1], k[2], k[3]});
        k1 = bytes_to_uint32(new byte[] {k[4], k[5], k[6], k[7]});
        k2 = bytes_to_uint32(new byte[] {k[8], k[9], k[10], k[11]});
        k3 = bytes_to_uint32(new byte[] {k[12], k[13], k[14], k[15]});
        return true;
    }

    public boolean setLoops(int loops) {
        switch (loops) {
            case 16:
            case 32:
            case 64:
                this.loops = loops;
                return true;
        }
        return false;
    }

    private static long UINT32_MAX = 0xFFFFFFFFL;
    private static long BYTE_1 = 0xFFL;
    private static long BYTE_2 = 0xFF00L;
    private static long BYTE_3 = 0xFF0000L;
    private static long BYTE_4 = 0xFF000000L;

    private static long delta = 0x9E3779B9L;

    private long k0, k1, k2, k3;

    private int loops = 32;

    private byte[] encrypt_group(byte[] v) {
        long v0 = bytes_to_uint32(new byte[] {v[0], v[1], v[2], v[3]});
        long v1 = bytes_to_uint32(new byte[] {v[4], v[5], v[6], v[7]});
        long sum = 0L;
        long v0_xor_1 = 0L, v0_xor_2 = 0L, v0_xor_3 = 0L;
        long v1_xor_1 = 0L, v1_xor_2 = 0L, v1_xor_3 = 0L;
        for (int i = 0; i < loops; i++) {
            sum = toUInt32(sum + delta);
            v0_xor_1 = toUInt32(toUInt32(v1 << 4) + k0);
            v0_xor_2 = toUInt32(v1 + sum);
            v0_xor_3 = toUInt32((v1 >> 5) + k1);
            v0 = toUInt32(  v0 + toUInt32(v0_xor_1 ^ v0_xor_2 ^ v0_xor_3)  );
            v1_xor_1 = toUInt32(toUInt32(v0 << 4) + k2);
            v1_xor_2 = toUInt32(v0 + sum);
            v1_xor_3 = toUInt32((v0 >> 5) + k3);
            System.out.printf("%08X\t%08X\t%08X\t%08X\n", i, v0, v0 >> 5, k3);
            v1 = toUInt32(  v1 + toUInt32(v1_xor_1 ^ v1_xor_2 ^ v1_xor_3)  );
        }
        byte[] b0 = long_to_bytes(v0, 4);
        byte[] b1 = long_to_bytes(v1, 4);
        return new byte[] {b0[0], b0[1], b0[2], b0[3], b1[0], b1[1], b1[2], b1[3]};
    }

    private byte[] decrypt_group(byte[] v) {
        long v0 = bytes_to_uint32(new byte[] {v[0], v[1], v[2], v[3]});
        long v1 = bytes_to_uint32(new byte[] {v[4], v[5], v[6], v[7]});
        long sum = 0xC6EF3720L, tmp = 0L;
        for (int i = 0; i < loops; i++) {
            tmp = toUInt32(toUInt32(v0 << 4) + k2);
            v1 = toUInt32(  v1 - toUInt32(tmp ^  toUInt32(v0 + sum) ^ toUInt32((v0 >> 5) + k3))  );
            tmp = toUInt32(toUInt32(v1 << 4) + k0);
            v0 = toUInt32(  v0 - toUInt32(tmp ^  toUInt32(v1 + sum) ^ toUInt32((v1 >> 5) + k1))  );
            sum = toUInt32(sum - delta);
        }
        byte[] b0 = long_to_bytes(v0, 4);
        byte[] b1 = long_to_bytes(v1, 4);
        return new byte[] {b0[0], b0[1], b0[2], b0[3], b1[0], b1[1], b1[2], b1[3]};
    }


    private static byte[] long_to_bytes(long n, int len) {
        byte a = (byte)((n & BYTE_4) >> 24);
        byte b = (byte)((n & BYTE_3) >> 16);
        byte c = (byte)((n & BYTE_2) >> 8);
        byte d = (byte)(n & BYTE_1);
        if (len == 4) {
            return new byte[] {a, b, c, d};
        }
        byte ha = (byte)(n >> 56);
        byte hb = (byte)((n >> 48) & BYTE_1);
        byte hc = (byte)((n >> 40) & BYTE_1);
        byte hd = (byte)((n >> 32) & BYTE_1);
        return new byte[] {ha, hb, hc, hd, a, b, c, d};
    }

    private static long bytes_to_uint32(byte[] bs) {
        return ((bs[0]<<24) & BYTE_4) +
                ((bs[1]<<16) & BYTE_3) +
                ((bs[2]<<8)  & BYTE_2) +
                (bs[3] & BYTE_1);
    }

    private static long toUInt32(long n) {
        return n & UINT32_MAX;
    }


    private static void println_array(byte[] b) {
        for (byte x : b) {
            System.out.printf("%02X ", x);
        }
        System.out.println();
    }
    /*private static void println_array(long[] b) {
        for (long x : b) {
            System.out.printf("%016X ", x);
        }
        System.out.println();
    }*/

    private static void test() {

    }

    public static void main(String[] args) {
//        byte[] bs = new byte[] {(byte)0xFF, (byte)0xEE, (byte)0xDD, (byte)0xCC};
//        System.out.printf("%016X\n", bytes_to_uint32(bs));
//        System.out.println(bytes_to_uint32(bs));
//
//
        Tea t = new Tea();
        byte[] pnt = new byte[] {
                0x00, 0x00, 0x00, 0x20,
                0x00, 0x00, 0x00, 0x10
        };
        byte[] k = new byte[] {
                0x00, 0x00, 0x00, 0x04,
                0x00, 0x00, 0x00, 0x03,
                0x00, 0x00, 0x00, 0x02,
                0x00, 0x00, 0x00, 0x01
        };
        t.setKey(k);
//        byte[] enc = t.encrypt(v, k);
//        byte[] dec = t.decrypt(enc, k);
        byte[] enc = t.encrypt_group(pnt);
        //byte[] enc = new byte[] {(byte) 0xC1, (byte) 0xC6, 0x48, 0x7A, (byte) 0x9E, 0x6F, (byte) 0xF2, 0x56};
        byte[] dec = t.decrypt_group(enc);

        //println_array(v_from_byte_to_long(new byte[]{ 0x7F, 0x1E, 0x55, 0x56, 0x32, 0x35, 0x65, 0x78 }));
        //println_array(k_from_byte_to_long(new byte[]{ 0x7F, 0x1E, 0x55, 0x56, 0x32, 0x35, 0x65, 0x78, 0x6F, 0x1E, 0x55, 0x56, 0x32, 0x35, 0x65, 0x78 }));
        //println_array(long_to_bytes((long)0x7E987654, 8));
        //byte b = (byte)0xEF;
        //println_array(new long[] { (b << 24) & 0xFF000000L } );
        //println_array(new long[] {(byte)0xEF});

//        String[] plain = new String[32];
//        for (i = 0; i < 32; i++) {
//            plain[i] = String.
//        }
//        byte[] pnt = "123".getBytes();
//        byte[] enc = encrypt(pnt, k);
//        byte[] dec = decrypt(enc, k);

        System.out.println("Key:");
        println_array(k);

        System.out.println("Encrypt And Decrypt:");
        println_array(pnt);
        println_array(enc);
        println_array(dec);
    }
}
