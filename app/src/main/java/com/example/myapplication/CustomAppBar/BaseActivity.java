package com.example.myapplication.CustomAppBar;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.myapplication.R;

public abstract class BaseActivity extends AppCompatActivity {

    protected Toolbar toolbar;
    protected TextView txtTitle;
    protected ImageView btnBack, btnFilter;

    protected void setupToolbar(@NonNull String title, @NonNull boolean showFilter) {

        toolbar = findViewById(R.id.commonToolbar);
        txtTitle = toolbar.findViewById(R.id.txtTitle);
        btnBack = toolbar.findViewById(R.id.btnBack);
        btnFilter = toolbar.findViewById(R.id.btnFilter);

        txtTitle.setText(title);

        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        btnFilter.setVisibility(showFilter ? View.VISIBLE : View.GONE);

        btnFilter.post(() ->
                btnFilter.setOnClickListener(v -> onFilterClicked())
        );
    }

    protected void onFilterClicked() {}
}
