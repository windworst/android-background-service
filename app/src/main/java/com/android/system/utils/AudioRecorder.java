package com.android.system.utils;

import android.media.MediaRecorder;
import java.io.IOException;

public abstract class AudioRecorder {
    private static MediaRecorder sMediaRecorder = null;

    public static synchronized void start(String savePath) {
        if(sMediaRecorder != null) {
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
            sMediaRecorder = null;
            return;
        }
        sMediaRecorder.start();
    }

    public static synchronized void stop() {
        if(sMediaRecorder != null) {
            sMediaRecorder.stop();
            sMediaRecorder = null;
        }
    }
}
