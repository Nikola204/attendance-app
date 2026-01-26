package com.example.studentqrscanner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.fragment.AnalyticsFragment;
import com.example.studentqrscanner.fragment.QrFragment;
import com.example.studentqrscanner.fragment.StudentProfileFragment;

public class StudentHomeActivity extends BaseActivity {

    private static final int TAB_ANALYTICS = 0;
    private static final int TAB_SCAN = 1;
    private static final int TAB_PROFILE = 2;
    private static final String KEY_CURRENT_TAB = "current_tab";

    private SupabaseClient supabaseClient;

    private ImageView iconAnalytics;
    private ImageView iconScan;
    private ImageView iconProfile;
    private TextView tvTitle;
    private int currentTab = TAB_PROFILE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt(KEY_CURRENT_TAB, TAB_PROFILE);
        }

        supabaseClient = new SupabaseClient(this);
        if (!supabaseClient.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        tvTitle = findViewById(R.id.textTitle);

        setupBottomNav();
        setupTopNavbar();
        applyInsetsPadding();
        setupBackStackListener();

        if (savedInstanceState == null) {
            openTab(currentTab);
        } else {
            setSelectedTab(currentTab);
            updateTitle(getTitleForTab(currentTab));
        }
    }

    private void setupBackStackListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            // Kada se back button klikne, vrati naslov na trenutni tab
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                updateTitle(getString(R.string.nav_title_profile));
                currentTab = TAB_PROFILE;
            }
        });
    }

    private void setupBottomNav() {
        View navAnalytics = findViewById(R.id.navAnalytics);
        View navScan = findViewById(R.id.navScan);
        View navProfile = findViewById(R.id.navProfile);

        iconAnalytics = findViewById(R.id.iconAnalytics);
        iconScan = findViewById(R.id.iconScan);
        iconProfile = findViewById(R.id.iconProfile);

        navAnalytics.setOnClickListener(v -> openTab(TAB_ANALYTICS));
        navScan.setOnClickListener(v -> openTab(TAB_SCAN));
        navProfile.setOnClickListener(v -> openTab(TAB_PROFILE));
    }

    private void openTab(int tab) {
        Fragment fragment;
        String title;

        switch (tab) {
            case TAB_ANALYTICS:
                fragment = new AnalyticsFragment();
                title = getString(R.string.nav_title_analytics);
                break;
            case TAB_SCAN:
                fragment = new QrFragment();
                title = getString(R.string.nav_title_scan_qr);
                break;
            case TAB_PROFILE:
            default:
                fragment = new StudentProfileFragment();
                title = getString(R.string.nav_title_profile);
                break;
        }

        currentTab = tab;
        setSelectedTab(tab);
        updateTitle(title);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void updateTitle(String title) {
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
    }

    /**
     * Javna metoda za fragmentima da mogu ažurirati naslov
     */
    public void setToolbarTitle(String title) {
        updateTitle(title);
    }

    private String getTitleForTab(int tab) {
        switch (tab) {
            case TAB_ANALYTICS:
                return getString(R.string.nav_title_analytics);
            case TAB_SCAN:
                return getString(R.string.nav_title_scan_qr);
            case TAB_PROFILE:
            default:
                return getString(R.string.nav_title_profile);
        }
    }

    private void setSelectedTab(int tab) {
        setIconState(iconAnalytics, tab == TAB_ANALYTICS);
        setIconState(iconScan, tab == TAB_SCAN);
        setIconState(iconProfile, tab == TAB_PROFILE);
    }

    private void setIconState(ImageView icon, boolean selected) {
        if (icon == null) return;
        icon.setAlpha(selected ? 1f : 0.4f);
    }

    private void setupTopNavbar() {
        ImageView btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> showPopupMenu(v));
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.top_nav_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                supabaseClient.signOut();
                navigateToLogin();
                return true;
            } else if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void applyInsetsPadding() {
        View root = findViewById(R.id.rootContainer);
        View bottomNav = findViewById(R.id.bottomNavContainer);
        View topNav = findViewById(R.id.topNavContainer);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            bottomNav.setPadding(bottomNav.getPaddingLeft(), bottomNav.getPaddingTop(),
                    bottomNav.getPaddingRight(), systemBars.bottom);
            
            topNav.setPadding(topNav.getPaddingLeft(), systemBars.top,
                    topNav.getPaddingRight(), topNav.getPaddingBottom());
            
            return insets;
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_TAB, currentTab);
    }
}
