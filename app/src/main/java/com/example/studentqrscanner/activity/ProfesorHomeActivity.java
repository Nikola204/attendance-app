package com.example.studentqrscanner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.fragment.AnalyticsFragment;
import com.example.studentqrscanner.fragment.CreateLectureFragment;
import com.example.studentqrscanner.fragment.ProfesorProfileFragment;

public class ProfesorHomeActivity extends AppCompatActivity {

    private static final int TAB_ANALYTICS = 0;
    private static final int TAB_CREATE = 1;
    private static final int TAB_PROFILE = 2;

    private SupabaseClient supabaseClient;
    private ImageView iconAnalytics;
    private ImageView iconCreateLecture;
    private ImageView iconProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profesor_home);

        supabaseClient = new SupabaseClient(this);
        if (!supabaseClient.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setupBottomNav();
        applyInsetsPadding();

        if (savedInstanceState == null) {
            openTab(TAB_PROFILE);
        }
    }

    private void setupBottomNav() {
        View navAnalytics = findViewById(R.id.navAnalytics);
        View navCreateLecture = findViewById(R.id.navCreateLecture);
        View navProfile = findViewById(R.id.navProfile);

        iconAnalytics = findViewById(R.id.iconAnalytics);
        iconCreateLecture = findViewById(R.id.iconCreateLecture);
        iconProfile = findViewById(R.id.iconProfile);

        navAnalytics.setOnClickListener(v -> openTab(TAB_ANALYTICS));
        navCreateLecture.setOnClickListener(v -> openTab(TAB_CREATE));
        navProfile.setOnClickListener(v -> openTab(TAB_PROFILE));
    }

    private void openTab(int tab) {
        Fragment fragment;
        switch (tab) {
            case TAB_ANALYTICS:
                fragment = new AnalyticsFragment();
                break;
            case TAB_CREATE:
                fragment = new CreateLectureFragment();
                break;
            case TAB_PROFILE:
            default:
                fragment = new ProfesorProfileFragment();
                break;
        }

        setSelectedTab(tab);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void setSelectedTab(int tab) {
        setIconState(iconAnalytics, tab == TAB_ANALYTICS);
        setIconState(iconCreateLecture, tab == TAB_CREATE);
        setIconState(iconProfile, tab == TAB_PROFILE);
    }

    private void setIconState(ImageView icon, boolean selected) {
        if (icon == null) return;
        icon.setAlpha(selected ? 1f : 0.4f);
    }

    private void applyInsetsPadding() {
        View root = findViewById(R.id.rootContainer);
        View bottomNav = findViewById(R.id.bottomNavContainer);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            bottomNav.setPadding(bottomNav.getPaddingLeft(), bottomNav.getPaddingTop(),
                    bottomNav.getPaddingRight(), bottom);
            return insets;
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
