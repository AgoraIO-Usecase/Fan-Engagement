package io.agora.live_streaming.manager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;

public class RTMManager {
    private static final String TAG = RTMManager.class.getSimpleName();

    private Context mContext;
    private RtmClient mRtmClient;
    private RtmChannel mRtmChannel;
    private IRtmCallBack mIRtmCallBack;

    public RTMManager(Context context) {
        mContext = context;
    }

    public void init() {
        Log.e("mqi", "rtm init");
        String appID = mContext.getString(io.agora.live_streaming.R.string.agora_app_id);

        try {
            mRtmClient = RtmClient.createInstance(mContext, appID, new RtmClientListener() {
                @Override
                public void onConnectionStateChanged(int state, int reason) {

                }

                @Override
                public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
                    mIRtmCallBack.onPeerMessageReceived(rtmMessage, peerId);
                }

                @Override
                public void onTokenExpired() {

                }
            });

            mRtmClient.setParameters("{\"rtm.log_filter\": 65535}");
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtm sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    public void createAndJoin(String channelName) {
        mRtmChannel = mRtmClient.createChannel(channelName, new RtmChannelListener() {
            @Override
            public void onMessageReceived(RtmMessage rtmMessage, RtmChannelMember rtmChannelMember) {
                Log.e("mqi", "rtm manager onMessageReceived");
                mIRtmCallBack.onChannelMessageReceived(rtmMessage, rtmChannelMember);
            }

            @Override
            public void onMemberJoined(RtmChannelMember rtmChannelMember) {

            }

            @Override
            public void onMemberLeft(RtmChannelMember rtmChannelMember) {

            }
        });

        mRtmChannel.join(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                Log.i("mqi", "join channel success");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.e("mqi", "join channel failed:" + errorInfo.getErrorDescription());
            }
        });
    }

    public void sendChannelMessage(String content) {
        Log.e("mqi", "sendChannelMessage");
        RtmMessage message = mRtmClient.createMessage();
        message.setText(content);

        mRtmChannel.sendMessage(message, new ResultCallback<Void>() {

            @Override
            public void onSuccess(Void aVoid) {
                Log.e("mqi", "sendChannelMessage success");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                // refer to RtmStatusCode.ChannelMessageState for the message state
                final int errorCode = errorInfo.getErrorCode();
                String errordescription = errorInfo.getErrorDescription();
                Log.e("mqi", "sendChannelMessage error: " + errorCode + " " + errordescription);
            }
        });
    }

    public void sendPeerMessage(String peerId, String content) {

        RtmMessage message = mRtmClient.createMessage();
        message.setText(content);

        // step 2: send message to peer
        mRtmClient.sendMessageToPeer(peerId, message, new ResultCallback<Void>() {

            @Override
            public void onSuccess(Void aVoid) {
                Log.e("mqi", "sendpeermessage success");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.e("mqi", "sendpeermessage failed: " + errorInfo.getErrorDescription());
            }
        });
    }

    public void leaveAndReleaseChannel() {
        if (mRtmChannel != null) {
            mRtmChannel.leave(null);
            mRtmChannel.release();
            mRtmChannel = null;
        }
    }

    public void createAndJoinChannel(String uid, final String channelName) {
        mRtmClient.login(null, uid, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                Log.e("mqi", "login success");
                createAndJoin(channelName);
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.e("mqi", "login failed: " + errorInfo.getErrorCode());

            }
        });
    }

    public RtmClient getRtmClient() {
        return mRtmClient;
    }

    public void registerCallBack(IRtmCallBack callback) {
        mIRtmCallBack = callback;
    }
    public void unRegisterCallBack() {
        mIRtmCallBack = null;
    }

    public interface IRtmCallBack {
        void onChannelMessageReceived(RtmMessage rtmMessage, RtmChannelMember rtmChannelMember);
        void onPeerMessageReceived(RtmMessage rtmMessage, String peerId);
    }
}
