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
import com.example.studentqrscanner.model.UserRole;
import com.example.studentqrscanner.model.Profesor;

public class LoginActivity extends BaseActivity {

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
            checkUserRoleAndNavigate();
            return;
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void checkUserRoleAndNavigate() {
        showLoading(true);
        supabaseClient.fetchCurrentUser(new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess(BaseUser user) {
                showLoading(false);
                handleNavigation(user);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
            }
        });
    }

    private void attemptLogin() {
        etEmail.setError(null);
        etPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean hasError = false;

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

        if (hasError) return;

        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        showLoading(true);
        supabaseClient.signInWithEmail(email, password, new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess(BaseUser user) {
                showLoading(false);
                handleNavigation(user);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Greška: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleNavigation(BaseUser user) {
        Intent intent;
        if (user instanceof Profesor || user.getRole() == UserRole.PROFESOR) {
            intent = new Intent(this, ProfesorHomeActivity.class);
        } else {
            intent = new Intent(this, StudentHomeActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isEmailDomainValid(String email) {
        return email.toLowerCase().endsWith(REQUIRED_EMAIL_DOMAIN);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
    }
}
