package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.io.File;

public class FullScreenPreviewDialog extends DialogFragment {

    private final String imagePath;

    public FullScreenPreviewDialog(String imagePath) {
        this.imagePath = imagePath;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fullscreen_preview, container, false);

        ImageView ivPreview = view.findViewById(R.id.ivFullPreview);
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(new File(imagePath).getAbsolutePath());
            ivPreview.setImageBitmap(bitmap);
        }

        view.findViewById(R.id.ivFullPreview).setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }
}
