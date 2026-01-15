package com.example.studentqrscanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentqrscanner.activity.LoginActivity;
import com.example.studentqrscanner.config.SupabaseClient;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnLogout;
    private SupabaseClient supabaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWelcome = findViewById(R.id.tvWelcome);

        supabaseClient = new SupabaseClient(this);

        // Provjeri da li je korisnik ulogovan
        if (!supabaseClient.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        tvWelcome.setText("Dobrodošli u Attendance App!\n\nUspješno ste prijavljeni.");

        // Logout button
        btnLogout.setOnClickListener(v -> {
            supabaseClient.signOut();
            navigateToLogin();
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
