package com.example.studentqrscanner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.fragment.AnalyticsFragment;
import com.example.studentqrscanner.fragment.CreateClassFragment;
import com.example.studentqrscanner.fragment.ProfesorProfileFragment;

public class ProfesorHomeActivity extends BaseActivity {

    private static final int TAB_ANALYTICS = 0;
    private static final int TAB_CREATE = 1;
    private static final int TAB_PROFILE = 2;

    private SupabaseClient supabaseClient;
    private ImageView iconAnalytics;
    private ImageView iconCreateLecture;
    private ImageView iconProfile;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profesor_home);

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
            openTab(TAB_PROFILE);
        }
    }

    private void setupBackStackListener() {
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            // Kada se back button klikne, vrati naslov na "Profil"
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                updateTitle("Profil");
            }
        });
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
        String title;

        switch (tab) {
            case TAB_ANALYTICS:
                fragment = new AnalyticsFragment();
                title = "Povijest evidencija";
                break;
            case TAB_CREATE:
                fragment = new CreateClassFragment();
                title = "Novi kolegij";
                break;
            case TAB_PROFILE:
            default:
                fragment = new ProfesorProfileFragment();
                title = "Profil";
                break;
        }

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

    private void setSelectedTab(int tab) {
        setIconState(iconAnalytics, tab == TAB_ANALYTICS);
        setIconState(iconCreateLecture, tab == TAB_CREATE);
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
}
