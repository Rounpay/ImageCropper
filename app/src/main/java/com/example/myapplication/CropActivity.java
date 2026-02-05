package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Stack;

public class CropActivity extends AppCompatActivity {
    private ImageView imageView;
    private View cropOverlay;
    private final Matrix matrix = new Matrix();
    private float translateX = 0f, translateY = 0f;
    private float prevX, prevY;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1f;
    Button btnRatio;
    CropOverlayView overlay;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crop);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cropRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        imageView = findViewById(R.id.imageView);
        cropOverlay = findViewById(R.id.cropOverlay);
        Button btnCrop = findViewById(R.id.btnCrop);
        Button btnCancel = findViewById(R.id.btnCancel);
        overlay = (CropOverlayView) cropOverlay;
        btnRatio = findViewById(R.id.btnRatio);
        String path = getIntent().getStringExtra("imagePath");

        Bitmap bitmap = decodeSampledBitmap(path); // memory safe load
        imageView.setScaleType(ImageView.ScaleType.MATRIX);
        imageView.setImageBitmap(bitmap);

        imageView.post(() -> fitImageToView(bitmap));

        // pinch zoom
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 5f));
                matrix.setScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                matrix.postTranslate(translateX, translateY);
                imageView.setImageMatrix(matrix);
                return true;
            }
        });
        imageView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            if (event.getPointerCount() == 1) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        prevX = event.getX();
                        prevY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getX() - prevX;
                        float dy = event.getY() - prevY;
                        translateX += dx;
                        translateY += dy;
                        matrix.postTranslate(dx, dy);
                        imageView.setImageMatrix(matrix);
                        prevX = event.getX();
                        prevY = event.getY();
                        break;
                }
            }
            return true;
        });
        btnCancel.setOnClickListener(v -> finish());
        btnCrop.setOnClickListener(v -> cropAndReturn());
        btnRatio.setOnClickListener(v -> {
            overlay.setAspectLocked(!overlay.isSelected());
            overlay.setSelected(!overlay.isSelected());
            btnRatio.setText(overlay.isSelected() ? "FREE" : "1:1");
        });
    }

    private void fitImageToView(Bitmap bitmap) {

        matrix.reset();

        float viewWidth = imageView.getWidth();
        float viewHeight = imageView.getHeight();

        float imageWidth = bitmap.getWidth();
        float imageHeight = bitmap.getHeight();

        float scale = Math.min(
                viewWidth / imageWidth,
                viewHeight / imageHeight
        );

        float dx = (viewWidth - imageWidth * scale) / 2f;
        float dy = (viewHeight - imageHeight * scale) / 2f;

        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);

        imageView.setImageMatrix(matrix);

        scaleFactor = scale;
    }


    private void cropAndReturn() {

        Bitmap original = BitmapFactory.decodeFile(
                getIntent().getStringExtra("imagePath"));

        RectF cropRect = ((CropOverlayView) cropOverlay).getCropRect();

        Matrix inverse = new Matrix();
        imageView.getImageMatrix().invert(inverse);

        RectF mapped = new RectF(cropRect);
        inverse.mapRect(mapped);

        int x = Math.max(0, (int) mapped.left);
        int y = Math.max(0, (int) mapped.top);
        int width = Math.min(original.getWidth() - x, (int) mapped.width());
        int height = Math.min(original.getHeight() - y, (int) mapped.height());

        Bitmap cropped = Bitmap.createBitmap(original, x, y, width, height);

        try {
            File file = new File(getCacheDir(),
                    "crop_" + System.currentTimeMillis() + ".jpg");

            FileOutputStream fos = new FileOutputStream(file);
            cropped.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

            Intent result = new Intent();
            result.putExtra("croppedPath", file.getAbsolutePath());
            setResult(RESULT_OK, result);
            finish();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // Memory efficient bitmap loader
    private Bitmap decodeSampledBitmap(String path){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        int inSampleSize = 1;
        if(options.outHeight > 1080 || options.outWidth > 1080){
            final int halfHeight = options.outHeight / 2;
            final int halfWidth = options.outWidth / 2;
            while((halfHeight/inSampleSize) >= 1080 && (halfWidth/inSampleSize) >= 1080){
                inSampleSize *= 2;
            }
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        return BitmapFactory.decodeFile(path, options);
    }
}