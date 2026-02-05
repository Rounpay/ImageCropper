package com.example.myapplication.zegocloude;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.zegocloud.uikit.prebuilt.livestreaming.ZegoUIKitPrebuiltLiveStreamingConfig;
import com.zegocloud.uikit.prebuilt.livestreaming.ZegoUIKitPrebuiltLiveStreamingFragment;

public class LiveActivity extends AppCompatActivity {
    String liveId, name, userId;
    boolean isHost;
    AppCompatTextView liveID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_live);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();
        userId = intent.getStringExtra("user_id");
        name = intent.getStringExtra("name");
        liveId = intent.getStringExtra("liveId");
        isHost = intent.getBooleanExtra("host", false);
        liveID= findViewById(R.id.liveID);
        liveID.setText(liveId);
        addLiveFragment();
    }

    private void addLiveFragment() {

        ZegoUIKitPrebuiltLiveStreamingConfig config =
                isHost ? getHostConfig() : getAudienceConfig();

        ZegoUIKitPrebuiltLiveStreamingFragment fragment =
                ZegoUIKitPrebuiltLiveStreamingFragment.newInstance(
                        Long.parseLong(Utils.appID),
                        Utils.appSign,
                        userId,
                        name,
                        liveId,
                        config
                );

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
    private ZegoUIKitPrebuiltLiveStreamingConfig getHostConfig() {
        ZegoUIKitPrebuiltLiveStreamingConfig config =
                ZegoUIKitPrebuiltLiveStreamingConfig.host();
        config.turnOnMicrophoneWhenJoining = true;
        config.turnOnCameraWhenJoining = false;
        return config;
    }

    private ZegoUIKitPrebuiltLiveStreamingConfig getAudienceConfig() {
        ZegoUIKitPrebuiltLiveStreamingConfig config =
                ZegoUIKitPrebuiltLiveStreamingConfig.audience();
        return config;
    }
}