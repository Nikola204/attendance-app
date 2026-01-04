package com.example.studentqrscanner.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.BaseUser;
import com.example.studentqrscanner.activity.StudentHomeActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private SupabaseClient supabaseClient;

    private static final String REQUIRED_EMAIL_DOMAIN = "@fsre.sum.ba";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        supabaseClient = new SupabaseClient(this);

        if (supabaseClient.isLoggedIn()) {
            navigateToHome();
            return;
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    /**
     * Pokušaj logina
     */
    private void attemptLogin() {
        // Reset errors
        etEmail.setError(null);
        etPassword.setError(null);

        // Dohvati vrijednosti
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validacija
        boolean hasError = false;

        // Provjeri jesu li polja prazna
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email je obavezan");
            hasError = true;
        } else if (!isValidEmail(email)) {
            etEmail.setError("Unesite validan email");
            hasError = true;
        } else if (!isEmailDomainValid(email)) {
            etEmail.setError("Email mora biti sa domene fsre.sum.ba");
            hasError = true;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Lozinka je obavezna");
            hasError = true;
        } else if (password.length() < 3) {
            etPassword.setError("Lozinka mora imati najmanje 3 karaktera");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        performLogin(email, password);
    }

    /**
     * Izvrši login preko Supabase-a
     */
    private void performLogin(String email, String password) {
        showLoading(true);

        supabaseClient.signInWithEmail(email, password, new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess(BaseUser user) {
                showLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Dobrodosli, " + user.getFullName() + " (" + user.getRole().getValue() + ")",
                        Toast.LENGTH_LONG).show();
                navigateToHome();
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this,
                        "Greska: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Provjeri email
     */
    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Provjeri domenu
     */
    private boolean isEmailDomainValid(String email) {
        return email.toLowerCase().endsWith(REQUIRED_EMAIL_DOMAIN);
    }

    /**
     * Prikaži/sakrij loading indicator
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            etEmail.setEnabled(false);
            etPassword.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            etEmail.setEnabled(true);
            etPassword.setEnabled(true);
        }
    }

    /**
     * Navigiraj na profil studenta
     */
    private void navigateToHome() {
        Intent intent = new Intent(this, StudentHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
