package io.agora.fan_engagement.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.agora.fan_engagement.AGApplication;
import io.agora.fan_engagement.R;
import io.agora.fan_engagement.adapter.ItemDecor;
import io.agora.fan_engagement.adapter.MessageAdapter;
import io.agora.fan_engagement.manager.RTCManager;
import io.agora.fan_engagement.manager.RTMManager;
import io.agora.fan_engagement.util.Constant;
import io.agora.rtc.Constants;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmMessage;

public class LiveActivity extends Activity implements RTMManager.IRtmCallBack, RTCManager.IRtcCallBack {

    private RTCManager mRTCManager;
    private RTMManager mRTMManager;
    private FrameLayout mAudienceFL;
    private FrameLayout mBroadcasterFL;
    private FrameLayout mStreamingFL;
    private EditText mChat;
    private ImageView mPlus;
    private ImageView mExit;
    private ImageView mFullscreen;
    private RecyclerView mChatRoom;
    private LinearLayout mChatPanel;
    private LinearLayout mPeoplePanel;
    private ImageView mBroadcasterPH;
    private MessageAdapter mMessageAdapter;
    private List<String> mMessageDataSet = new ArrayList<>();
    private boolean mStreamingOn = false;
    private boolean mBroadcasterOn = false;
    private boolean mChatPanelOn = false;
    private boolean mFullscreenOn = false;
    private SurfaceView mStreamingView;
    private SurfaceView mBroadcasterView;

    private static final String[] REQUESTED_PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int PERMISSION_REQ_ID = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            init();
        }
    }

    private void init() {
        mAudienceFL = (FrameLayout) findViewById(R.id.audience);
        mBroadcasterFL = (FrameLayout) findViewById(R.id.broadcaster);
        mStreamingFL = (FrameLayout) findViewById(R.id.streaming);
        mChat = (EditText) findViewById(R.id.chatlayout);
        mPlus = (ImageView) findViewById(R.id.plus);
        mExit = (ImageView) findViewById(R.id.exit);
        mFullscreen = (ImageView) findViewById(R.id.fullscreenb_btn);
        mBroadcasterPH = (ImageView) findViewById(R.id.broadcaster_place_holder);

        mChatRoom = (RecyclerView) findViewById(R.id.chat_list);
        mChatRoom.setHasFixedSize(true);
        mMessageAdapter = new MessageAdapter(new WeakReference<Context>(this), mMessageDataSet);
        mChatRoom.setAdapter(mMessageAdapter);
        mChatRoom.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mChatRoom.addItemDecoration(new ItemDecor());
        mChatPanel = (LinearLayout) findViewById(R.id.chatpanel);
        mPeoplePanel = (LinearLayout) findViewById(R.id.people);

        mExit.setVisibility(View.GONE);
        mChat.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                Log.d(Constant.DEBUG_LOG_TAG, "onEditoraction");
                if (actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_SEND
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    String message = mChat.getText().toString();
                    mRTMManager.sendChannelMessage(message);
                    mChat.setText("");
                    mChat.clearFocus();
                    Log.d(Constant.DEBUG_LOG_TAG, "enter detected");

                    mMessageDataSet.add(Constant.RTM_UID_AUDIENCE + ": " + message);
                    if (mMessageDataSet.size() > 5) { // max value is 5
                        int len = mMessageDataSet.size() - 6;
                        for (int i = 0; i < len; i++) {
                            mMessageDataSet.remove(i);
                        }
                    }

                    mMessageAdapter.updateDataSet(mMessageDataSet);
                    mChatRoom.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
                }

                return true;
             }
         });

        mRTCManager = AGApplication.the().getRTCManager();
        mRTMManager = AGApplication.the().getRTMManager();

        mRTMManager.registerCallBack(this);
        mRTCManager.registerCallBack(this);

        mRTCManager.joinChannel(Constant.CHANNEL_NAME, Constant.UID_AUDIENCE);
        mRTMManager.createAndJoinChannel(Constant.RTM_UID_AUDIENCE, Constant.CHANNEL_NAME);

    }

    public void onPlusClicked(View v) {
        //showToast("Application has been sent. Please wait for the host's approval");
        showMyself();
        //mRTMManager.sendPeerMessage(Constant.RTM_UID_BROADCASTER, Constant.REQUEST_MESSAGE);
    }

    public void onToggleFullscreen(View v) {
        int targetWeight = mFullscreenOn ? 1 : 0;
        int targetIcon = mFullscreenOn ? R.drawable.expand_screen : R.drawable.collapse_screen;
        mFullscreenOn = !mFullscreenOn;
        ((LinearLayout.LayoutParams)mPeoplePanel.getLayoutParams()).weight = targetWeight;
        mPeoplePanel.requestLayout();
        mFullscreen.setImageDrawable(getResources().getDrawable(targetIcon, getTheme()));
    }

    public void onToggleChatPanel(View v) {
        int targetWeight = mChatPanelOn ? 0 : 1;
        mChatPanelOn = !mChatPanelOn;
        ((LinearLayout.LayoutParams)mChatPanel.getLayoutParams()).weight = targetWeight;
        mChatPanel.requestLayout();
    }

    private void showMyself() {
        mPlus.setVisibility(View.GONE);
        mRTCManager.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        setupLocalVideo();
        mExit.setVisibility(View.VISIBLE);
    }

    public void onExitClicked(View v) {
        mPlus.setVisibility(View.VISIBLE);
        mRTCManager.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
        mExit.setVisibility(View.GONE);
        mRTCManager.enableLocalVideo(false);
        mAudienceFL.removeAllViews();
    }

    private void setupLocalVideo() {
        Log.d(Constant.DEBUG_LOG_TAG, "setuplocalvideo");
        SurfaceView camV = mRTCManager.getRtcEngine().CreateRendererView(getApplicationContext());
        camV.setZOrderOnTop(true);
        camV.setZOrderMediaOverlay(true);
        mAudienceFL.addView(camV, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mRTCManager.setupLocalVideo(new VideoCanvas(camV, VideoCanvas.RENDER_MODE_HIDDEN, Constant.UID_AUDIENCE));
        mRTCManager.enableLocalVideo(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRTMManager.unRegisterCallBack();
        mRTCManager.unRegisterCallBack();
        mRTCManager.leaveAndDestroy();
        mRTMManager.leaveAndReleaseChannel();

    }

    protected void lowerRtmpVolume(boolean lower)
    {
        int volume = lower ? 5 : 90;
        String params = "{\"che.audio.playout.uid.volume\": {\"uid\": 666, \"volume\": " + volume + "}}";
        mRTCManager.getRtcEngine().setParameters(params);
    }

    public void onChannelMessageReceived(final RtmMessage rtmMessage, final RtmChannelMember rtmChannelMember) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMessageDataSet.add(rtmChannelMember.getUserId() + ": " + rtmMessage.getText());
                if (mMessageDataSet.size() > 5) { // max value is 5
                    int len = mMessageDataSet.size() - 6;
                    for (int i = 0; i < len; i++) {
                        mMessageDataSet.remove(i);
                    }
                }

                mMessageAdapter.updateDataSet(mMessageDataSet);
                mChatRoom.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1);
            }
        });

    }

    public void onPeerMessageReceived(final RtmMessage rtmMessage, final String peerId) {
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerId.equals(Constant.RTM_UID_BROADCASTER)) {
                    if (rtmMessage.getText().equals(Constant.ALLOW_MESSAGE)) {
                        showMyself();
                    } else {
                        showToast("The host rejected your applicaton.");
                    }
                }
            }
        });*/
    }

    public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (uid == Constant.UID_STREAMING) {
                    mStreamingView = mRTCManager.getRtcEngine().CreateRendererView(getApplicationContext());
                    mStreamingOn = true;
                    mStreamingView.setZOrderOnTop(true);
                    mStreamingView.setZOrderMediaOverlay(true);
                    mStreamingFL.addView(mStreamingView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    mRTCManager.setupRemoteVideo(new VideoCanvas(mStreamingView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                } else {
                    mBroadcasterOn = true;
                    mBroadcasterPH.setVisibility(View.GONE);
                    mBroadcasterView = mRTCManager.getRtcEngine().CreateRendererView(getApplicationContext());
                    mBroadcasterView.setZOrderOnTop(true);
                    mBroadcasterView.setZOrderMediaOverlay(true);
                    mBroadcasterFL.addView(mBroadcasterView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    mRTCManager.setupRemoteVideo(new VideoCanvas(mBroadcasterView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                }

            }
        });
    }

    public void onFirstRemoteAudioDecoded(final int uid, int elapsed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // if someone who's not rtmp streaming comes in with audio, lower rtmp volume
                if (uid != Constant.UID_STREAMING){
                    lowerRtmpVolume(true);
                }
            }
        });
    }

    public void onUserMuteAudio(final int uid, final boolean muted) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // if someone who's not rtmp streaming muted/unmuted himself, update rtmp volume
                if (uid != Constant.UID_STREAMING){
                    lowerRtmpVolume(!muted);
                }
            }
        });
    }

    public void onUserJoined(int uid, int elapsed) {

    }

    public void onUserOffline(final int uid, int reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (uid == Constant.UID_STREAMING) {
                    mStreamingOn = false;
                    mStreamingFL.removeAllViews();
                } else {
                    mBroadcasterOn = false;
                    mBroadcasterFL.removeAllViews();
                    mBroadcasterPH.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    private void showToast(final String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        Log.d(Constant.DEBUG_LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    REQUESTED_PERMISSIONS,
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d(Constant.DEBUG_LOG_TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED || grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                    break;
                }
                init();
                break;
            }
        }
    }
}
