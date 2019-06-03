package io.agora.fan_engagement;

import android.app.Application;

import io.agora.fan_engagement.manager.RTCManager;
import io.agora.fan_engagement.manager.RTMManager;

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

