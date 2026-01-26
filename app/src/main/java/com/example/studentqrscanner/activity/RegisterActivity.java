package com.example.studentqrscanner.activity;

import android.content.Intent;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.BaseUser;
import com.example.studentqrscanner.model.UserRole;
import com.example.studentqrscanner.model.Profesor;

public class RegisterActivity extends BaseActivity {

    private static final String REQUIRED_EMAIL_DOMAIN = "@fsre.sum.ba";

    private RadioGroup rgRole;
    private RadioButton rbStudent;
    private View studentContainer;
    private EditText etFirstName, etLastName, etEmail, etPassword, etPasswordConfirm;
    private EditText etIndex, etStudy, etYear;
    private Button btnRegister;
    private ProgressBar progressBar;

    private SupabaseClient supabaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        supabaseClient = new SupabaseClient(this);

        bindViews();
        setupRoleSwitcher();

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void bindViews() {
        rgRole = findViewById(R.id.rgRole);
        rbStudent = findViewById(R.id.rbStudent);
        studentContainer = findViewById(R.id.containerStudent);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmailReg);
        etPassword = findViewById(R.id.etPasswordReg);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        etIndex = findViewById(R.id.etIndex);
        etStudy = findViewById(R.id.etStudy);
        etYear = findViewById(R.id.etYear);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressRegister);
    }

    private void setupRoleSwitcher() {
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isStudent = checkedId == R.id.rbStudent;
            studentContainer.setVisibility(isStudent ? View.VISIBLE : View.GONE);
        });
    }

    private void attemptRegister() {
        clearErrors();

        String first = etFirstName.getText().toString().trim();
        String last = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirm = etPasswordConfirm.getText().toString();
        boolean isStudent = rgRole.getCheckedRadioButtonId() == R.id.rbStudent;

        String index = etIndex.getText().toString().trim();
        String study = etStudy.getText().toString().trim();
        String yearStr = etYear.getText().toString().trim();

        boolean hasError = false;

        if (TextUtils.isEmpty(first)) {
            etFirstName.setError(getString(R.string.error_required_field));
            hasError = true;
        }
        if (TextUtils.isEmpty(last)) {
            etLastName.setError(getString(R.string.error_required_field));
            hasError = true;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_required_field));
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.invalid_email));
            hasError = true;
        } else if (!email.toLowerCase().endsWith(REQUIRED_EMAIL_DOMAIN)) {
            etEmail.setError(getString(R.string.error_email_domain));
            hasError = true;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_required_field));
            hasError = true;
        } else if (password.length() < 6) {
            etPassword.setError(getString(R.string.password_min_length));
            hasError = true;
        }

        if (TextUtils.isEmpty(confirm)) {
            etPasswordConfirm.setError(getString(R.string.error_required_field));
            hasError = true;
        } else if (!confirm.equals(password)) {
            etPasswordConfirm.setError(getString(R.string.error_password_match));
            hasError = true;
        }

        Integer year = null;
        if (isStudent) {
            if (TextUtils.isEmpty(index)) {
                etIndex.setError(getString(R.string.error_required_field));
                hasError = true;
            }
            if (TextUtils.isEmpty(study)) {
                etStudy.setError(getString(R.string.error_required_field));
                hasError = true;
            }
            if (TextUtils.isEmpty(yearStr)) {
                etYear.setError(getString(R.string.error_required_field));
                hasError = true;
            } else {
                try {
                    year = Integer.parseInt(yearStr);
                } catch (NumberFormatException e) {
                    etYear.setError(getString(R.string.error_required_field));
                    hasError = true;
                }
            }
        }

        if (hasError) return;

        doRegister(isStudent, first, last, email, password, index, study, year);
    }

    private void clearErrors() {
        etFirstName.setError(null);
        etLastName.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        etPasswordConfirm.setError(null);
        etIndex.setError(null);
        etStudy.setError(null);
        etYear.setError(null);
    }

    private void doRegister(boolean isStudent,
                            String first,
                            String last,
                            String email,
                            String password,
                            String index,
                            String study,
                            Integer year) {

        showLoading(true);

        supabaseClient.signUpUser(email, password, isStudent, first, last, index, study, year, new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess(BaseUser user) {
                showLoading(false);
                Toast.makeText(RegisterActivity.this, getString(R.string.register_success), Toast.LENGTH_LONG).show();
                handleNavigation(user);
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showErrorDialog(error);
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

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etPasswordConfirm.setEnabled(!show);
    }

    private void showErrorDialog(String message) {
        String safe = (message == null || message.isEmpty()) ? "Nepoznata greška" : message;
        new AlertDialog.Builder(this)
                .setTitle("Greška")
                .setMessage(safe)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
