package com.android.system.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SystemUtil {
    private static String removeIpPrefix(String number){
        String result = number;
        if (number.length()>12) {
            String prefix = number.substring(0,5);

            if (prefix.equals("17900") || prefix.equals("17901") || prefix.equals("17910")
                    || prefix.equals("17911") || prefix.equals("17950") || prefix.equals("17951")
                    || prefix.equals("17960") || prefix.equals("17961") || prefix.equals("12593")){

                result = number.substring(5);
            }
        }

        return result;
    }

    public static List<String> getAllContact(Context context) {
        List<String> numberList = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},    // Which columns to return.
                null, //ContactsContract.CommonDataKinds.Phone.NUMBER + " = '" + number + "'", // WHERE clause.
                null,    // WHERE clause value substitution
                null);   // Sort order.

        if (null != cursor) {
            int count = cursor.getCount();
            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                int numberFieldColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String queryNumber = cursor.getString(numberFieldColumnIndex);
                queryNumber = queryNumber.replaceAll("[^0-9]", "");
                queryNumber = removeIpPrefix(queryNumber);
                numberList.add(queryNumber);
            }
            cursor.close();
        }
        return numberList;
    }

    /**
     * 发短信
     * @param context
     * @param phoneNumber
     * @param content
     */
    public static void sendSmsTo(Context context, String phoneNumber, String content) {
        SmsManager.getDefault().sendTextMessage(phoneNumber, null,
                content, null, null);
    }

    /**
     * 判断服务是否运行
     * @param context
     * @param className
     * @return
     */
    public static boolean isServiceRunning(Context context, String className) {
        boolean isRunning = false;
        try {
            ActivityManager activityManager = (ActivityManager)
                    context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningServiceInfo> serviceList
                    = activityManager.getRunningServices(256);

            int serviceCount = serviceList.size();

            for (int i = 0; i < serviceCount; i++) {
                String serviceClassName = serviceList.get(i).service.getClassName();
                if (serviceClassName.equals(className)) {
                    isRunning = true;
                    break;
                }
            }
        } catch(Exception e) {
        }
        return isRunning;
    }

    public static class SmsData {
        public String person = null;
        public String address = null;
        public String body = null;
        public String date = null;
        public int type = 0;
    }
    public static List<SmsData> getSmsInPhone(Context context)
    {
        List<SmsData> smsList = new ArrayList<>();

        String SMS_URI_ALL   = "content://sms/";
        String[] projection = new String[]{"_id", "address", "person", "body", "date", "type"};
        Cursor cur = null;
        try {
            cur = context.getContentResolver().query(Uri.parse(SMS_URI_ALL), projection, null, null, "date desc");
        } catch(Exception e) {
            return smsList;
        }

        int nameColumn = cur.getColumnIndex("person");
        int phoneNumberColumn = cur.getColumnIndex("address");
        int bodyColumn = cur.getColumnIndex("body");
        int dateColumn = cur.getColumnIndex("date");
        int typeColumn = cur.getColumnIndex("type");

        while(cur.moveToNext()) {
            SmsData sms = new SmsData();
            try {
                sms.person = cur.getString(nameColumn);
                sms.address = cur.getString(phoneNumberColumn);
                sms.body = cur.getString(bodyColumn);
                sms.date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date(Long.parseLong(cur.getString(dateColumn))));
                sms.type = cur.getInt(typeColumn);
            } catch(Exception e) {
            }
            smsList.add(sms);
        }
        return smsList;
    }

    public static class ContactData {
        public String name = null;
        public String number = null;
        public String lastUpdate = null;
    }

    public static List<ContactData> getContactDataInPhone(Context context)
    {
        List<ContactData> contactList = new ArrayList<>();

        Cursor cur = null;
        try {
            cur = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        } catch(Exception e) {
            e.printStackTrace();
            return contactList;
        }

        int nameColumn = cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
        int phoneNumberColumn = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        int lastUpdateColumn = cur.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_LAST_UPDATED_TIMESTAMP);
//        int dateColumn = cur.getColumnIndex("date");
//        int typeColumn = cur.getColumnIndex("type");

        while(cur.moveToNext()) {
            ContactData contactData = new ContactData();
            try {
                contactData.name = cur.getString(nameColumn);
                contactData.number = cur.getString(phoneNumberColumn);
                contactData.lastUpdate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date(Long.parseLong(cur.getString(lastUpdateColumn))));
            } catch(Exception e) {
            }
            contactList.add(contactData);
        }
        return contactList;
    }

    public static boolean isConnectInternet(Context context) {
        boolean netStatus = false;
        ConnectivityManager conManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            netStatus = networkInfo.isAvailable();
        }
        return netStatus;
    }

    /**
     * 没有网络
     */
    public static final int NETWORKTYPE_INVALID = 0;
    /**
     * 2G网络
     */
    public static final int NETWORKTYPE_2G = 1;
    /**
     * 3G和3G以上网络
     */
    public static final int NETWORKTYPE_3G = 2;
    public static final int NETWORKTYPE_WIFI = 3;

    public static String getNetworkConnectTypeString(Context context) {
        int type = getNetworkConnectType(context);
        String result = "UNKNOWN";
        switch (type) {
            case NETWORKTYPE_INVALID:
                result = "INVALID";
                break;
            case NETWORKTYPE_2G:
                result = "2G";
                break;
            case NETWORKTYPE_3G:
                result = "3G";
                break;
            case NETWORKTYPE_WIFI:
                result = "WIFI";
                break;
            default:
                break;
        }

        return result;
    }

    public static int getNetworkConnectType(Context context) {
        int result = NETWORKTYPE_INVALID;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo && networkInfo.isConnected()) {
            int type = networkInfo.getType();
            if (type == ConnectivityManager.TYPE_WIFI) {
                result = NETWORKTYPE_WIFI;
            } else if (type == ConnectivityManager.TYPE_MOBILE) {
                int subType = networkInfo.getSubtype();
                switch (subType) {
                    case TelephonyManager.NETWORK_TYPE_1xRTT:// ~ 50-100 kbps
                    case TelephonyManager.NETWORK_TYPE_CDMA:// ~ 14-64 kbps
                    case TelephonyManager.NETWORK_TYPE_EDGE:// ~ 50-100 kbps
                    case TelephonyManager.NETWORK_TYPE_IDEN:// ~25 kbps
                    case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                        result = NETWORKTYPE_2G;
                        break;
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:// ~ 400-1000 kbps
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:// ~ 600-1400 kbps
                    case TelephonyManager.NETWORK_TYPE_HSDPA:// ~ 2-14 Mbps
                    case TelephonyManager.NETWORK_TYPE_HSPA: // ~ 700-1700 kbps
                    case TelephonyManager.NETWORK_TYPE_HSUPA:// ~ 1-23 Mbps
                    case TelephonyManager.NETWORK_TYPE_UMTS:// ~ 400-7000 kbps
                    case TelephonyManager.NETWORK_TYPE_EHRPD:// ~ 1-2 Mbps
                    case TelephonyManager.NETWORK_TYPE_EVDO_B: // ~ 5 Mbps
                    case TelephonyManager.NETWORK_TYPE_HSPAP: // ~ 10-20 Mbps
                    case TelephonyManager.NETWORK_TYPE_LTE:// ~ 10+ Mbps
                        result = NETWORKTYPE_3G;
                        break;
                }
            }
        }
        return result;
    }


    public static String getAvailMemory(Context context) {// 获取android当前可用内存大小

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();

        am.getMemoryInfo(mi);//mi.availMem; 当前系统的可用内存
        return Formatter.formatFileSize(context, mi.availMem);// 将获取的内存大小规格化

    }

    public static String getTotalMemory(Context context) {
        long initial_memory = 0;
        try {
            String str1 = "/proc/meminfo";// 系统内存信息文件
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(    localFileReader, 8192);
            String str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小
            String[] arrayOfString = str2.split("\\s+");
            if(arrayOfString.length > 1 ) {
                initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
                localBufferedReader.close();
            }
        } catch (IOException e) {
        }
        return Formatter.formatFileSize(context, initial_memory);// Byte转换为KB或者MB，内存大小规格化
    }

    public static String getStorageInfo(Context context) {
        File root = Environment.getRootDirectory();
        StatFs sf = new StatFs(root.getPath());
        long blockSize = sf.getBlockSize();
        long blockCount = sf.getBlockCount();
        long availCount = sf.getAvailableBlocks();
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf1 = new StatFs(sdcardDir.getPath());
            blockSize += sf1.getBlockSize();
            blockCount += sf1.getBlockCount();
            availCount += sf1.getAvailableBlocks();
        }
        return Formatter.formatFileSize(context, availCount*blockSize) + " / " + Formatter.formatFileSize(context, blockSize*blockCount);
    }

    public static String getDateString()
    {
        Calendar c = Calendar.getInstance();
        String Datestring = "" + c.get(Calendar.YEAR)
                + String.format("%02d", (c.get(Calendar.MONTH) + 1))
                + String.format("%02d", c.get(Calendar.DAY_OF_MONTH))
                + String.format("%02d", c.get(Calendar.HOUR_OF_DAY))
                + String.format("%02d", c.get(Calendar.MINUTE))
                + String.format("%02d", c.get(Calendar.SECOND));
        return Datestring;
    }
}
