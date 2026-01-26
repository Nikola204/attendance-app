package com.example.studentqrscanner.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.config.SupabaseClient;
import com.google.android.material.textfield.TextInputEditText;

public class DeleteAccountActivity extends AppCompatActivity {

    private TextInputEditText etEmailConfirmation;
    private Button btnDeleteAccount;
    private ProgressBar progressBar;
    private SupabaseClient supabaseClient;
    private String userEmail;
    private String userId;
    private boolean isStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);

        etEmailConfirmation = findViewById(R.id.etEmailConfirmation);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        progressBar = findViewById(R.id.progressBarDelete);

        supabaseClient = new SupabaseClient(this);

        // Get data from intent
        Intent intent = getIntent();
        userEmail = intent.getStringExtra("USER_EMAIL");
        userId = intent.getStringExtra("USER_ID");
        isStudent = intent.getBooleanExtra("IS_STUDENT", true);

        if (userEmail == null || userId == null) {
            Toast.makeText(this, "Greška: Nedostaju podaci o korisniku.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupInputListener();
        setupDeleteButton();
    }

    private void setupInputListener() {
        etEmailConfirmation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Check if input matches generic email format first slightly, but strictly match the user email
                String input = s.toString().trim();
                btnDeleteAccount.setEnabled(input.equals(userEmail));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupDeleteButton() {
        btnDeleteAccount.setOnClickListener(v -> showConfirmationDialog());
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Potvrda brisanja")
                .setMessage("Jeste li apsolutno sigurni? Ovi podaci će biti trajno obrisani.")
                .setPositiveButton("Obriši", (dialog, which) -> performDeletion())
                .setNegativeButton("Odustani", null)
                .show();
    }

    private void performDeletion() {
        setLoading(true);
        supabaseClient.deleteAccount(userId, isStudent, new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(DeleteAccountActivity.this, "Račun uspješno obrisan.", Toast.LENGTH_LONG).show();
                supabaseClient.signOut();
                
                Intent intent = new Intent(DeleteAccountActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(DeleteAccountActivity.this, "Greška pri brisanju: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnDeleteAccount.setEnabled(false);
            etEmailConfirmation.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            etEmailConfirmation.setEnabled(true);
            // Re-validate to enable button if text is still correct
            String input = etEmailConfirmation.getText().toString().trim();
            btnDeleteAccount.setEnabled(input.equals(userEmail));
        }
    }
}
