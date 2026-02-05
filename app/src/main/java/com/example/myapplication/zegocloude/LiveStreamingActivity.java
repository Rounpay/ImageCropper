package com.example.myapplication.zegocloude;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;
import java.util.UUID;

public class LiveStreamingActivity extends AppCompatActivity {
    TextInputLayout tilLiveId, tilUserName;
    TextInputEditText etLiveId, etUserName;
    MaterialButton btnStartLive;

    String liveId, name, userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_live_strem);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        tilLiveId = findViewById(R.id.tilLiveId);
        tilUserName = findViewById(R.id.tilUserName);
        etLiveId = findViewById(R.id.etLiveId);
        etUserName = findViewById(R.id.etUserName);
        btnStartLive = findViewById(R.id.btnStartLive);
        etLiveId.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                liveId = Objects.requireNonNull(etLiveId.getText()).toString();
                if (liveId.isEmpty()) {
                    btnStartLive.setText("Start New Live");
                } else {
                    btnStartLive.setText("Join Live");
                }

            }
        });
        btnStartLive.setOnClickListener(v -> {
            liveId = Objects.requireNonNull(etLiveId.getText()).toString();
            name = Objects.requireNonNull(etUserName.getText()).toString();
            if (name.isEmpty()) {
                tilUserName.setError("Enter your name");
                tilUserName.requestFocus();
                return;
            }
            if (!liveId.isEmpty() && liveId.length() != 5) {
                tilLiveId.setError("Enter valid live ID");
                tilLiveId.requestFocus();
                return;
            }
            startMeeting();
        });

    }

    private void startMeeting() {
        boolean isHost = true;
        if (liveId.length() == 5)
            isHost = false;
        else
            liveId = generateLiveID();

        userId = UUID.randomUUID().toString();

        Intent intent = new Intent(this, LiveActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("name", name);
        intent.putExtra("liveId", liveId);
        intent.putExtra("host", isHost);
        startActivity(intent);
        finish();
    }

    String generateLiveID() {
        StringBuilder builder = new StringBuilder();
        while (builder.length() != 5) {
            int random = (int) (Math.random() * 10);
            builder.append(random);
        }
        return builder.toString();
    }
}