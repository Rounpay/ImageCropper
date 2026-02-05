package com.example.myapplication.ms;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

import live.hms.video.error.HMSException;
import live.hms.video.media.tracks.HMSLocalAudioTrack;
import live.hms.video.media.tracks.HMSLocalVideoTrack;
import live.hms.video.media.tracks.HMSTrack;
import live.hms.video.sdk.HMSActionResultListener;
import live.hms.video.sdk.HMSSDK;
import live.hms.video.sdk.HMSUpdateListener;
import live.hms.video.sdk.models.HMSConfig;
import live.hms.video.sdk.models.HMSMessage;
import live.hms.video.sdk.models.HMSPeer;
import live.hms.video.sdk.models.HMSRecordingConfig;
import live.hms.video.sdk.models.HMSRemovedFromRoom;
import live.hms.video.sdk.models.HMSRoleChangeRequest;
import live.hms.video.sdk.models.HMSRoom;
import live.hms.video.sdk.models.enums.HMSPeerUpdate;
import live.hms.video.sdk.models.enums.HMSRoomUpdate;
import live.hms.video.sdk.models.enums.HMSTrackUpdate;
import live.hms.video.sdk.models.trackchangerequest.HMSChangeTrackStateRequest;
import live.hms.video.sdk.transcripts.HmsTranscripts;
import live.hms.video.sessionstore.HmsSessionStore;
import live.hms.video.signal.init.HMSTokenListener;
import live.hms.video.signal.init.TokenRequest;
import live.hms.videoview.HMSVideoView;

public class LiveStreamingActivity extends AppCompatActivity {
    // ===== 100ms SDK =====
    private HMSSDK hmsSdk;
    private HMSRoom currentRoom;

    // ===== State flags =====
    private boolean isAudioMuted = false;
    private boolean isVideoMuted = false;
    private boolean isHLSRunning = false;
    private boolean isRTMPRunning = false;

    // ===== UI =====
    private HMSVideoView videoViewLocal;
    private TextView tvLiveBadge;
    private TextView tvHlsUrl;
    private Button btnMuteAudio;
    private Button btnMuteVideo;
    private Button btnHLS;
    private Button btnRTMP;
    private Button btnLeave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_live_streaming);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // ===== UI Bind =====
        videoViewLocal = findViewById(R.id.videoViewLocal);
        tvLiveBadge = findViewById(R.id.tvLiveBadge);
        tvHlsUrl = findViewById(R.id.tvHlsUrl);
        btnMuteAudio = findViewById(R.id.btnMuteAudio);
        btnMuteVideo = findViewById(R.id.btnMuteVideo);
        btnHLS = findViewById(R.id.btnHLS);
        btnRTMP = findViewById(R.id.btnRTMP);
        btnLeave = findViewById(R.id.btnLeave);

        // ===== Room code Intent se lo =====
        String roomCode = getIntent().getStringExtra("ROOM_CODE");

        // ===== Step 1: HMSSDK instance banao =====
        hmsSdk = new HMSSDK.Builder(getApplication()).build();

        // ===== Step 2: Auth token generate karo =====
        generateToken(roomCode);

        // ===== Step 3: Button listeners =====
        setupButtons();
    }

    // ============================================================
    //  TOKEN GENERATION
    //  100ms docs: hmsInstance.getAuthTokenByRoomCode()
    // ============================================================
    private void generateToken(String roomCode) {
        hmsSdk.getAuthTokenByRoomCode(
                new TokenRequest(roomCode, "broadcaster_user"),
                null,                           // optional metadata
                new HMSTokenListener() {
                    @Override
                    public void onError(@NonNull HMSException error) {
                        runOnUiThread(() ->
                                Toast.makeText(LiveStreamingActivity.this,
                                        "Token Error: " + error.getMessage(),
                                        Toast.LENGTH_LONG).show()
                        );
                    }

                    @Override
                    public void onTokenSuccess(String token) {
                        // Token mila → room join karo
                        joinRoom(token);
                    }
                }
        );
    }

    // ============================================================
    //  JOIN ROOM
    //  100ms docs: hmsSdk.join(config, HMSUpdateListener)
    // ============================================================
    private void joinRoom(String authToken) {
        HMSConfig config = new HMSConfig("Broadcaster", authToken);

        hmsSdk.join(config, new HMSUpdateListener() {

            @Override
            public void onPermissionsRequested(@NonNull List<String> list) {

            }

            @Override
            public void onTranscripts(@NonNull HmsTranscripts hmsTranscripts) {

            }

            @Override
            public void onSessionStoreAvailable(@NonNull HmsSessionStore hmsSessionStore) {

            }

            @Override
            public void peerListUpdated(@Nullable ArrayList<HMSPeer> arrayList, @Nullable ArrayList<HMSPeer> arrayList1) {

            }

            @Override
            public void onChangeTrackStateRequest(@NonNull HMSChangeTrackStateRequest hmsChangeTrackStateRequest) {

            }

            @Override
            public void onRemovedFromRoom(@NonNull HMSRemovedFromRoom hmsRemovedFromRoom) {

            }

            // ---- onJoin: room mein aa gaye ----
            @Override
            public void onJoin(HMSRoom room) {
                currentRoom = room;
                runOnUiThread(() -> {
                    Toast.makeText(LiveStreamingActivity.this,
                            "Room join ho gaya!", Toast.LENGTH_SHORT).show();
                });

                // Local video preview
                showLocalPreview();

                // Check karo agar HLS / RTMP pehle se chal raha hai
                checkExistingStatus(room);
            }

            // ---- onPeerUpdate: koi join / leave hua ----
            @Override
            public void onPeerUpdate(HMSPeerUpdate type, HMSPeer peer) {
                runOnUiThread(() -> {
                    if (type == HMSPeerUpdate.PEER_JOINED) {
                        Toast.makeText(LiveStreamingActivity.this,
                                peer.getName() + " join hua", Toast.LENGTH_SHORT).show();
                    } else if (type == HMSPeerUpdate.PEER_LEFT) {
                        Toast.makeText(LiveStreamingActivity.this,
                                peer.getName() + " left", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // ---- onTrackUpdate: audio/video track mila ----
            @Override
            public void onTrackUpdate(HMSTrackUpdate type, HMSTrack track, HMSPeer peer) {
                // Apna local video track milne pe preview show karo
                if (peer.isLocal() && track instanceof HMSLocalVideoTrack) {
                    runOnUiThread(() -> videoViewLocal.addTrack((HMSLocalVideoTrack) track));
                }
            }

            // ---- onRoomUpdate: HLS / RTMP status =====
            @Override
            public void onRoomUpdate(HMSRoomUpdate type, HMSRoom hmsRoom) {
                runOnUiThread(() -> {
                    if (type == HMSRoomUpdate.HLS_STREAMING_STATE_UPDATED) {
                        handleHLSUpdate(hmsRoom);
                    } else if (type == HMSRoomUpdate.RTMP_STREAMING_STATE_UPDATED) {
                        handleRTMPUpdate(hmsRoom);
                    }
                });
            }

            // ---- onMessageReceived: chat ----
            @Override
            public void onMessageReceived(HMSMessage message) {
            }

            // ---- onRoleChangeRequest ----
            @Override
            public void onRoleChangeRequest(HMSRoleChangeRequest request) {
            }

            // ---- onError ----
            @Override
            public void onError(HMSException error) {
                runOnUiThread(() ->
                        Toast.makeText(LiveStreamingActivity.this,
                                "Error: " + error.getMessage(), Toast.LENGTH_LONG).show()
                );
            }

            // ---- onReconnecting ----
            @Override
            public void onReconnecting(HMSException error) {
            }

            // ---- onReconnected ----
            @Override
            public void onReconnected() {
            }
        });
    }

    // ============================================================
    //  LOCAL VIDEO PREVIEW
    // ============================================================
    private void showLocalPreview() {
        HMSPeer localPeer = hmsSdk.getLocalPeer();
        if (localPeer != null && localPeer.getVideoTrack() != null) {
            runOnUiThread(() ->
                    // videoViewLocal.addSink(localPeer.getVideoTrack());
                    videoViewLocal.addTrack(localPeer.getVideoTrack()));
        }
    }

    // ============================================================
    //  CHECK EXISTING STATUS (on join)
    // ============================================================
    private void checkExistingStatus(HMSRoom room) {
        runOnUiThread(() -> {
            // HLS
            if (room.getHlsStreamingState() != null && room.getHlsStreamingState().getRunning()) {
                isHLSRunning = true;
                btnHLS.setText("Stop HLS");
                tvLiveBadge.setVisibility(android.view.View.VISIBLE);
                if (room.getHlsStreamingState().getVariants() != null
                        && !room.getHlsStreamingState().getVariants().isEmpty()) {
                    tvHlsUrl.setText("HLS: " + room.getHlsStreamingState().getVariants().get(0).getHlsStreamUrl());
                }
            }
            // RTMP
            if (room.getRtmpHMSRtmpStreamingState() != null && room.getRtmpHMSRtmpStreamingState().getRunning()) {
                isRTMPRunning = true;
                btnRTMP.setText("Stop RTMP");
            }
        });
    }

    // ============================================================
    //  HLS UPDATE HANDLER
    // ============================================================
    private void handleHLSUpdate(HMSRoom room) {
        if (room.getHlsStreamingState() != null && room.getHlsStreamingState().getRunning()) {
            isHLSRunning = true;
            btnHLS.setText("Stop HLS");
            tvLiveBadge.setVisibility(android.view.View.VISIBLE);

            if (room.getHlsStreamingState().getVariants() != null
                    && !room.getHlsStreamingState().getVariants().isEmpty()) {
                String url = room.getHlsStreamingState().getVariants().get(0).getHlsStreamUrl();
                tvHlsUrl.setText("HLS: " + url);
            }
            Toast.makeText(this, "HLS Stream shuru ho gaya!", Toast.LENGTH_SHORT).show();
        } else {
            isHLSRunning = false;
            btnHLS.setText("HLS");
            tvLiveBadge.setVisibility(android.view.View.GONE);
            tvHlsUrl.setText("HLS URL: ---");
        }
    }

    // ============================================================
    //  RTMP UPDATE HANDLER
    // ============================================================
    private void handleRTMPUpdate(HMSRoom room) {
        if (room.getRtmpHMSRtmpStreamingState() != null && room.getRtmpHMSRtmpStreamingState().getRunning()) {
            isRTMPRunning = true;
            btnRTMP.setText("Stop RTMP");
            Toast.makeText(this, "RTMP Stream shuru ho gaya!", Toast.LENGTH_SHORT).show();
        } else {
            isRTMPRunning = false;
            btnRTMP.setText("RTMP");
        }
    }

    // ============================================================
    //  BUTTONS SETUP
    // ============================================================
    private void setupButtons() {

        // ---------- AUDIO MUTE / UNMUTE ----------
        btnMuteAudio.setOnClickListener(v -> {
            HMSPeer local = hmsSdk.getLocalPeer();
            if (local != null && local.getAudioTrack() != null) {
                HMSLocalAudioTrack audioTrack = (HMSLocalAudioTrack) local.getAudioTrack();
                isAudioMuted = !isAudioMuted;
                audioTrack.setMute(isAudioMuted);
                btnMuteAudio.setText(isAudioMuted ? "Unmute Audio" : "Audio");
            }
        });

        // ---------- VIDEO MUTE / UNMUTE ----------
        btnMuteVideo.setOnClickListener(v -> {
            HMSPeer local = hmsSdk.getLocalPeer();
            if (local != null && local.getVideoTrack() != null) {
                HMSLocalVideoTrack videoTrack = (HMSLocalVideoTrack) local.getVideoTrack();
                isVideoMuted = !isVideoMuted;
                videoTrack.setMute(isVideoMuted);
                btnMuteVideo.setText(isVideoMuted ? "Unmute Video" : "Video");
            }
        });

        // ---------- HLS START / STOP ----------
        // 100ms docs: hmsSdk.startHLSStreaming() / hmsSdk.stopHLSStreaming()
        btnHLS.setOnClickListener(v -> {
            if (!isHLSRunning) {
                // ===== START HLS (Default view) =====
                hmsSdk.startHLSStreaming(null, new HMSActionResultListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() ->
                                Toast.makeText(LiveStreamingActivity.this,
                                        "HLS starting...", Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onError(HMSException error) {
                        runOnUiThread(() ->
                                Toast.makeText(LiveStreamingActivity.this,
                                        "HLS Error: " + error.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    }
                });
            } else {
                // ===== STOP HLS =====
                hmsSdk.stopHLSStreaming(null, new HMSActionResultListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            isHLSRunning = false;
                            btnHLS.setText("HLS");
                            tvLiveBadge.setVisibility(android.view.View.GONE);
                            tvHlsUrl.setText("HLS URL: ---");
                            Toast.makeText(LiveStreamingActivity.this,
                                    "HLS band ho gaya", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(HMSException error) {
                        runOnUiThread(() ->
                                Toast.makeText(LiveStreamingActivity.this,
                                        "Stop Error: " + error.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    }
                });
            }
        });

        // ---------- RTMP START / STOP ----------
        // 100ms docs: hmsSdk.startRtmpOrRecording(config, listener)
        // ⚠️  Apna RTMP URL aur Meeting URL dalo
        btnRTMP.setOnClickListener(v -> {
            if (!isRTMPRunning) {

                // ===== APNA RTMP URL YAHAN DALO =====
                // Format: rtmp://server/app/STREAM_KEY
                String rtmpUrl = "rtmp://a.rtmp.youtube.com/live2/YOUR_STREAM_KEY";
                // ===== APNA MEETING URL YAHAN DALO =====
                String meetingUrl = "https://100ms.live/meetings/YOUR_MEETING_LINK";

                HMSRecordingConfig recordingConfig = new HMSRecordingConfig(
                        meetingUrl,                         // meeting URL (bot use karega)
                        java.util.Arrays.asList(rtmpUrl),   // RTMP URLs (max 3)
                        false,
                        null// record = false (sirf streaming)
                );

                hmsSdk.startRtmpOrRecording(recordingConfig, new HMSActionResultListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() ->
                                Toast.makeText(LiveStreamingActivity.this,
                                        "RTMP starting...", Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onError(HMSException error) {
                        runOnUiThread(() ->
                                Toast.makeText(LiveStreamingActivity.this,
                                        "RTMP Error: " + error.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    }
                });

            } else {
                // ===== STOP RTMP =====
                // 100ms docs: hmsSdk.stopRtmpAndRecording(listener)
                hmsSdk.stopRtmpAndRecording(new HMSActionResultListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            isRTMPRunning = false;
                            btnRTMP.setText("RTMP");
                            Toast.makeText(LiveStreamingActivity.this,
                                    "RTMP band ho gaya", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(HMSException error) {
                        runOnUiThread(() ->
                                Toast.makeText(LiveStreamingActivity.this,
                                        "Stop Error: " + error.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    }
                });
            }
        });

        // ---------- LEAVE ----------
        btnLeave.setOnClickListener(v -> {
            hmsSdk.leave(null);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hmsSdk != null) {
            hmsSdk.leave(null);
        }
    }
}