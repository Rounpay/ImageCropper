package com.example.myapplication.ms;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.myapplication.R;

import java.util.ArrayList;
import java.util.List;

import live.hms.video.error.HMSException;
import live.hms.video.media.tracks.HMSTrack;
import live.hms.video.sdk.HMSSDK;
import live.hms.video.sdk.HMSUpdateListener;
import live.hms.video.sdk.models.HMSConfig;
import live.hms.video.sdk.models.HMSMessage;
import live.hms.video.sdk.models.HMSPeer;
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

public class ViewerActivity extends AppCompatActivity {
    // ===== 100ms SDK =====
    private HMSSDK hmsSdk;

    // ===== ExoPlayer =====
    private ExoPlayer player;

    // ===== UI =====
    private PlayerView playerView;
    private LinearLayout llLoading;       // loading container
    private ProgressBar progressBar;
    private TextView tvLoading;
    private TextView tvLiveBadge;
    private Button btnLeave;

    // ===== Flag: stream play ho raha hai? =====
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_viewer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // ===== UI Bind =====
        playerView = findViewById(R.id.playerView);
        llLoading = findViewById(R.id.llLoading);
        progressBar = findViewById(R.id.progressBar);
        tvLoading = findViewById(R.id.tvLoading);
        tvLiveBadge = findViewById(R.id.tvLiveBadge);
        btnLeave = findViewById(R.id.btnLeave);

        // ===== Room Code =====
        String roomCode = getIntent().getStringExtra("ROOM_CODE");

        // ===== Step 1: HMSSDK instance =====
        hmsSdk = new HMSSDK.Builder(getApplication()).build();

        // ===== Step 2: Token generate =====
        generateToken(roomCode);

        // ===== Leave button =====
        btnLeave.setOnClickListener(v -> {
            releasePlayer();
            hmsSdk.leave(null);
            finish();
        });
    }
    // ============================================================
    //  TOKEN GENERATION
    // ============================================================
    private void generateToken(String roomCode) {
        hmsSdk.getAuthTokenByRoomCode(
                new TokenRequest(roomCode, "viewer_user"),
                null,
                new HMSTokenListener() {
                    @Override
                    public void onError(@NonNull HMSException error) {
                        runOnUiThread(() ->
                                Toast.makeText(ViewerActivity.this,
                                        "Token Error: " + error.getMessage(),
                                        Toast.LENGTH_LONG).show()
                        );
                    }
                    @Override
                    public void onTokenSuccess(@NonNull String token) {
                        joinRoom(token);
                    }
                }
        );
    }
    // ============================================================
    //  JOIN ROOM  (Viewer bhi room join karta hai — HLS URL milne ke liye)
    // ============================================================
    private void joinRoom(String authToken) {
        HMSConfig config = new HMSConfig("Viewer", authToken);

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

            @Override
            public void onJoin(HMSRoom room) {
                runOnUiThread(() ->
                        Toast.makeText(ViewerActivity.this,
                                "Room join ho gaya, stream wait kar raha hai...",
                                Toast.LENGTH_SHORT).show()
                );
                // Join ho ne pe check karo agar HLS already chal raha hai
                checkAndPlayHLS(room);
            }

            @Override
            public void onPeerUpdate(HMSPeerUpdate type, HMSPeer peer) { }

            @Override
            public void onTrackUpdate(HMSTrackUpdate type, HMSTrack track, HMSPeer peer) { }

            // ===== Room update: HLS status milega yahan =====
            @Override
            public void onRoomUpdate(HMSRoomUpdate type, HMSRoom hmsRoom) {
                if (type == HMSRoomUpdate.HLS_STREAMING_STATE_UPDATED) {
                    runOnUiThread(() -> checkAndPlayHLS(hmsRoom));
                }
            }

            @Override
            public void onMessageReceived(HMSMessage message) { }

            @Override
            public void onRoleChangeRequest(HMSRoleChangeRequest request) { }

            @Override
            public void onError(HMSException error) {
                runOnUiThread(() ->
                        Toast.makeText(ViewerActivity.this,
                                "Error: " + error.getMessage(), Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onReconnecting(HMSException error) { }

            @Override
            public void onReconnected() { }
        });
    }

    // ============================================================
    //  HLS CHECK & PLAY
    //  100ms docs: room.getHlsStreamingState().getVariants().get(0).getHlsStreamUrl()
    // ============================================================
    private void checkAndPlayHLS(HMSRoom room) {
        if (room.getHlsStreamingState().getRunning() && room.getHlsStreamingState().getVariants() != null && !room.getHlsStreamingState().getVariants().isEmpty()) {

            String hlsUrl = room.getHlsStreamingState().getVariants().get(0).getHlsStreamUrl();

            if (hlsUrl != null && !hlsUrl.isEmpty() && !isPlaying) {
                runOnUiThread(() -> playHLSStream(hlsUrl));
            }
        }
    }
    // ============================================================
    //  EXOPLAYER — HLS STREAM PLAY
    // ============================================================
    private void playHLSStream(String hlsUrl) {
        isPlaying = true;

        // Loading hide, player show
        llLoading.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);
        tvLiveBadge.setVisibility(View.VISIBLE);

        // ExoPlayer banao
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // HLS URL (M3U8) load karo
        MediaItem mediaItem = MediaItem.fromUri(hlsUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        Toast.makeText(this, "Stream chal raha hai!", Toast.LENGTH_SHORT).show();
    }
    // ============================================================
    //  CLEANUP
    // ============================================================
    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
        isPlaying = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) player.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        if (hmsSdk != null) hmsSdk.leave(null);
    }
}