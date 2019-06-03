package io.agora.live_streaming.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import io.agora.live_streaming.R;
import io.agora.live_streaming.util.Constant;

public class MainActivity extends Activity {
    private EditText mChannelName;
    private static final String[] REQUESTED_PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            init();
        }
    }

    private void init() {
        mChannelName = findViewById(R.id.et_channel_name);
    }

    public void onStartClicked(View v) {
        Constant.CHANNEL_NAME = mChannelName.getText().toString();
        if (TextUtils.isEmpty(Constant.CHANNEL_NAME)) {
            Toast.makeText(this, "Please input a channel name!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(MainActivity.this, LiveActivity.class);
        startActivity(i);

    }

    private static final int PERMISSION_REQ_ID = 1024;

    private boolean checkSelfPermission(String permission, int requestCode) {
        Log.i("mqi", "checkSelfPermission " + permission + " " + requestCode);
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
        Log.i("mqi", "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

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
