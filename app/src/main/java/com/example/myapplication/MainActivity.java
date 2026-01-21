package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        btnPickGallery = findViewById(R.id.btnPickGallery);
        Button btnTakeCamera = findViewById(R.id.btnTakeCamera);
        btnPreview = findViewById(R.id.btnPreview);
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean granted = result.get(Manifest.permission.CAMERA);
                    if (granted != null && granted) openCamera();
                    else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
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
}