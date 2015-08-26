package com.android.system.utils;

import android.media.MediaRecorder;
import java.io.IOException;

public abstract class AudioRecorder {
    private static MediaRecorder sMediaRecorder = null;
    private static Object sLock = new Object();
    private static boolean sIsStart = false;

    public static void start(String savePath) {
        synchronized (sLock) {
            if (sIsStart) {
                return;
            }
            sMediaRecorder = new MediaRecorder();
            sMediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL); // 设置音频采集原
            sMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); //内容输出格式
            sMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); //音频编码方式
            sMediaRecorder.setOutputFile(savePath);
            try {
                sMediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            try {
                sMediaRecorder.start();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            sIsStart = true;
        }
    }

    public static synchronized void stop() {
        synchronized (sLock) {
            if (sIsStart) {
                try {
                    sMediaRecorder.setOnErrorListener(null);
                    sMediaRecorder.setOnInfoListener(null);
                    sMediaRecorder.stop();
                    sMediaRecorder.reset();
                    sMediaRecorder.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                sMediaRecorder = null;
                sIsStart = false;
            }
        }
    }
}
