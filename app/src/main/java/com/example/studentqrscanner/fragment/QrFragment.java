package com.example.studentqrscanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.activity.LoginActivity;
import com.example.studentqrscanner.activity.PortraitCaptureActivity;
import com.example.studentqrscanner.config.SupabaseClient;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class QrFragment extends Fragment {

    private SupabaseClient supabaseClient;
    private TextView tvLastResult;

    private boolean scanning = false;
    private boolean allowAutoStart = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        supabaseClient = new SupabaseClient(requireContext());

        if (!supabaseClient.isLoggedIn()) {
            Toast.makeText(requireContext(), "Prijavite se ponovo", Toast.LENGTH_LONG).show();
            navigateToLogin();
            return;
        }

        tvLastResult = view.findViewById(R.id.tvLastResult);
        startScan();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (allowAutoStart && !scanning) {
            startScan();
        }
    }

    private void startScan() {
        if (scanning || !isAdded()) return;
        scanning = true;
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Skeniraj QR kod");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true); // keep portrait
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        scanning = false;
        allowAutoStart = false;
        if (result != null) {
            if (result.getContents() != null) {
                String scanned = result.getContents();
                tvLastResult.setText("Posljednji rezultat: " + scanned);
                showResultDialog(scanned);
            } else {
                Toast.makeText(requireContext(), "Skeniranje otkazano", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showResultDialog(String scanned) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Skenirano")
                .setMessage(scanned)
                .setPositiveButton("OK", null)
                .setNegativeButton("Ponovo skeniraj", (dialog, which) -> {
                    allowAutoStart = true;
                    startScan();
                })
                .show();
    }

    private void navigateToLogin() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
