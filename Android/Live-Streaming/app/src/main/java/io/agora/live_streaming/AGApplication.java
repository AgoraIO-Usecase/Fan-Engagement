package io.agora.live_streaming;

import android.app.Application;

import io.agora.live_streaming.manager.RTCManager;
import io.agora.live_streaming.manager.RTMManager;

public class AGApplication extends Application {

    private static AGApplication sInstance;
    private RTMManager mRTMManager;
    private RTCManager mRTCManager;


    public static AGApplication the() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;

        mRTMManager = new RTMManager(this);
        mRTMManager.init();
        mRTCManager = new RTCManager(this);
        mRTCManager.init();
    }

    public RTMManager getRTMManager() {
        return mRTMManager;
    }

    public RTCManager getRTCManager() {
        return mRTCManager;
    }
}

