package io.agora.live_streaming.manager;

import android.content.Context;
import android.util.Log;

import io.agora.live_streaming.util.Constant;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class RTCManager {
    private static final String TAG = RTCManager.class.getSimpleName();
    private RtcEngine mRtcEngine;
    private Context mContext;

    public RTCManager(Context context) {
        mContext = context;
    }

    public void init() {
        setupAgoraEngine();
    }

    private IRtcCallBack mIRtcCallBack;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            Log.e("mqi", "rtcmanager onFirsetRemoteVideoDecoded");
            mIRtcCallBack.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            mIRtcCallBack.onUserJoined(uid, elapsed);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            mIRtcCallBack.onUserOffline(uid, reason);
        }

    };

    public RtcEngine getRtcEngine() {
        return mRtcEngine;
    }

    private void setupAgoraEngine() {
        Log.e("mqi", "setupAgoraEngine");
        String appID = mContext.getString(io.agora.live_streaming.R.string.agora_app_id);

        try {
            mRtcEngine = RtcEngine.create(mContext, appID, mRtcEventHandler);
            mRtcEngine.setLogFile(Constant.RTC_LOG_PATH);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.enableVideo();
            mRtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

            mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15, VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_LANDSCAPE));


            Log.i(TAG, "setupAgoraEngine mRtcEngine :" + mRtcEngine);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    public void enableLocalVideo(boolean enable) {
        mRtcEngine.enableLocalVideo(enable);
    }

    public void setupLocalVideo(VideoCanvas canvas) {
        mRtcEngine.setupLocalVideo(canvas);
    }

    public void setupRemoteVideo(VideoCanvas canvas) {
        mRtcEngine.setupRemoteVideo(canvas);
    }
    public void joinChannel(String channelName, int uid) {
        Log.e("mqi", "rtcmanger joinChannel");
        mRtcEngine.joinChannel(null, channelName, "", uid);
    }

    public void leaveAndDestroy() {
        mRtcEngine.leaveChannel();
        mRtcEngine.destroy();
    }

    public void setClientRole(int role) {
        mRtcEngine.setClientRole(role);
    }

    public void registerCallBack(IRtcCallBack callback) {
        mIRtcCallBack = callback;
    }
    public void unRegisterCallBack() {
        mIRtcCallBack = null;
    }

    public interface IRtcCallBack {
        void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed);

        void onUserJoined(int uid, int elapsed);

        void onUserOffline(int uid, int reason);
    }

}
