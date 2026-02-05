package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.ms.LiveStreamingActivity;
import com.example.myapplication.ms.ViewerActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.listeners.BannerAdEventListener;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.SdkInitializationListener;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button btnPickGallery;
    private Button btnPreview;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri cameraUri;
    private File cameraFile;
    private String lastCroppedPath;

    private ActivityResultLauncher<String[]> permissionLauncher;
    private AdView adView;
    private FrameLayout adViewContainer;
    InMobiBanner inMobiBanner;
    private EditText etRoomCode;
    private Button btnGoLive;
    private Button btnWatch;
    // ===== Runtime Permission Launcher =====
    private ActivityResultLauncher<String[]> permissionLauncherLive =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        // Sabhi permissions mile?
                        boolean allGranted = result.values().stream().allMatch(v -> v);
                        if (!allGranted) {
                            Toast.makeText(this, "Camera aur Audio permission zaroori hai!", Toast.LENGTH_LONG).show();
                        }
                    }
            );
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        InMobiSdk.init(this, "1f95b17666274ab59b7fe74118c3e918", null,
                new SdkInitializationListener() {
                    @Override
                    public void onInitializationComplete(@Nullable Error error) {

                        if (error != null) {
                            Log.e("INMOBI", "Init failed: " + error.getMessage());
                            return;
                        }

                        Log.d("INMOBI", "Init success");

                        runOnUiThread(() -> loadInMobiBanner());
                    }
                });

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        EdgeToEdge.enable(this);
        etRoomCode = findViewById(R.id.etRoomCode);
        btnGoLive  = findViewById(R.id.btnGoLive);
        btnWatch   = findViewById(R.id.btnWatch);
        // ===== Permissions maango app start pe =====
        requestPermissions();

        // ===== Go Live → Broadcaster =====
        btnGoLive.setOnClickListener(v -> {
            String roomCode = etRoomCode.getText().toString().trim();
            if (roomCode.isEmpty()) {
                Toast.makeText(this, "Room Code enter karo!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, LiveStreamingActivity.class);
            intent.putExtra("ROOM_CODE", roomCode);
            startActivity(intent);
        });

        // ===== Watch Stream → Viewer =====
        btnWatch.setOnClickListener(v -> {
            String roomCode = etRoomCode.getText().toString().trim();
            if (roomCode.isEmpty()) {
                Toast.makeText(this, "Room Code enter karo!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ViewerActivity.class);
            intent.putExtra("ROOM_CODE", roomCode);
            startActivity(intent);
        });
        MobileAds.initialize(this, initializationStatus -> {
        });

        RequestConfiguration configuration =
                new RequestConfiguration.Builder().setTestDeviceIds(Collections.singletonList("1097FD163428C5EE77C4A200FA7B9761"))
                        .build();
        MobileAds.setRequestConfiguration(configuration);
        adViewContainer = findViewById(R.id.ad_view_container);
        loadBannerAd();
        btnPickGallery = findViewById(R.id.btnPickGallery);
        Button btnTakeCamera = findViewById(R.id.btnTakeCamera);
        btnPreview = findViewById(R.id.btnPreview);
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean granted = result.get(Manifest.permission.CAMERA);
                    if (granted != null && granted) openCamera();
                    else
                        Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        openCrop(cameraFile.getAbsolutePath());
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                        Uri uri = result.getData().getData();
                        if (uri == null) return;

                        try {
                            File file = FileUtils.fromUri(this, uri);

                            if (file != null && file.exists()) {
                                openCrop(file.getAbsolutePath()); //  CORRECT
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        lastCroppedPath = result.getData().getStringExtra("croppedPath");
                        Toast.makeText(this, "Cropped: " + lastCroppedPath, Toast.LENGTH_LONG).show();
                    }

                });
        btnTakeCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
            } else openCamera();
        });

        btnPickGallery.setOnClickListener(v -> openGallery());

        btnPreview.setOnClickListener(v -> {
            if (lastCroppedPath != null) {
                FullScreenPreviewDialog dialog = new FullScreenPreviewDialog(lastCroppedPath);
                dialog.show(getSupportFragmentManager(), "fullScreenPreview");
            } else {
                Toast.makeText(this, "No cropped image to preview", Toast.LENGTH_SHORT).show();
            }

        });

    }

    private void loadInMobiBanner() {

        LinearLayout bannerContainer = findViewById(R.id.banner_container);

        inMobiBanner = new InMobiBanner(this, 1000000000L);

        //  MUST be before load()
        inMobiBanner.setBannerSize(320, 50);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        dpToPx(320),
                        dpToPx(50)
                );
        lp.gravity = Gravity.CENTER;

        inMobiBanner.setLayoutParams(lp);

        bannerContainer.removeAllViews();
        bannerContainer.addView(inMobiBanner);

        inMobiBanner.setListener(new BannerAdEventListener() {
            @Override
            public void onAdDisplayed(@NonNull InMobiBanner banner) {
                Log.d("InMobi", "BANNER DISPLAYED");
            }

            @Override
            public void onAdLoadFailed(
                    @NonNull InMobiBanner banner,
                    @NonNull InMobiAdRequestStatus status) {

                Log.e("InMobi", "LOAD FAILED: "
                        + status.getStatusCode() + " | "
                        + status.getMessage());
            }
        });

        Log.d("InMobi", "Loading banner 320x50");
        InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG);
        inMobiBanner.load();
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }



    private void loadBannerAd() {

        adView = new AdView(this);
        adView.setAdUnitId("ca-app-pub-3940256099942544/9214589741");
        adView.setAdSize(AdSize.BANNER);
        adViewContainer.removeAllViews();
        adViewContainer.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d("AdMob", "BANNER LOADED");
                adViewContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Log.e("AdMob", "FAILED: " + adError.getCode() + " | " + adError.getMessage());
                adViewContainer.setVisibility(View.GONE);
            }
        });
    }

    private void openCamera() {
        try {
            cameraFile = new File(getCacheDir(), "camera_" + System.currentTimeMillis() + ".jpg");
            cameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", cameraFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            cameraLauncher.launch(intent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void openCrop(String path) {
        Intent i = new Intent(this, CropActivity.class);
        i.putExtra("imagePath", path);
        cropLauncher.launch(i);
    }
    // ===== Permission Request Logic =====
    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.CAMERA);
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.RECORD_AUDIO);
        }

        if (!permissions.isEmpty()) {
            permissionLauncherLive.launch(permissions.toArray(new String[0]));
        }
    }
}