package com.example.studentqrscanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.activity.LoginActivity;
import com.example.studentqrscanner.activity.PortraitCaptureActivity;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.Predavanje;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Locale;

public class QrFragment extends Fragment {

    private SupabaseClient supabaseClient;
    private TextView tvLastResult;

    private boolean scanning = false;
    private boolean allowAutoStart = true;

    // Modern Activity Result API
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                scanning = false;
                if (result.getContents() == null) {
                    Toast.makeText(requireContext(), "Skeniranje otkazano", Toast.LENGTH_SHORT).show();
                    allowAutoStart = false;
                } else {
                    handleScanResult(result.getContents());
                    allowAutoStart = false;
                }
            });

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

        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Skeniraj QR kod");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(PortraitCaptureActivity.class);
        options.setBarcodeImageEnabled(false);

        barcodeLauncher.launch(options);
    }

    private void handleScanResult(String scanned) {
        if (!isAdded()) return;

        String predavanjeId = extractPredavanjeId(scanned);
        if (predavanjeId == null) {
            tvLastResult.setText("Nevažeći QR kod");
            Toast.makeText(requireContext(), "QR ne sadrži ID predavanja", Toast.LENGTH_LONG).show();
            return;
        }

        tvLastResult.setText("Pronađen QR, dohvaćam podatke...");
        fetchPredavanje(predavanjeId);
    }

    private String extractPredavanjeId(String scanned) {
        if (scanned == null) return null;
        String trimmed = scanned.trim();

        String lower = trimmed.toLowerCase(Locale.US);
        int markerIndex = lower.indexOf("predavanjeid=");
        if (markerIndex >= 0) {
            String idPart = trimmed.substring(markerIndex + "predavanjeId=".length());
            int ampIndex = idPart.indexOf("&");
            if (ampIndex >= 0) {
                idPart = idPart.substring(0, ampIndex);
            }
            trimmed = idPart.trim();
        } else if (trimmed.contains("=")) {
            String[] parts = trimmed.split("=", 2);
            trimmed = parts[1].trim();
        }

        return trimmed.isEmpty() ? null : trimmed;
    }

    private void fetchPredavanje(String predavanjeId) {
        supabaseClient.getPredavanjeById(predavanjeId, new SupabaseClient.PredavanjeDetailCallback() {
            @Override
            public void onSuccess(Predavanje predavanje) {
                if (!isAdded()) return;
                String naziv = predavanje.getNaslov() != null ? predavanje.getNaslov() : "Predavanje";
                tvLastResult.setText("Predavanje: " + naziv);
                showAttendanceDialog(predavanje);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                tvLastResult.setText("Greška: " + error);
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAttendanceDialog(Predavanje predavanje) {
        if (!isAdded()) return;

        String naziv = predavanje.getNaslov() != null ? predavanje.getNaslov() : "Predavanje";
        String datum = predavanje.getDatum() != null ? predavanje.getDatum() : "";
        String message = datum.isEmpty() ? "Zelite li evidentirati dolazak?" : "Evidentirati dolazak za datum: " + datum;

        new AlertDialog.Builder(requireContext())
                .setTitle(naziv)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> potvrdiDolazak(predavanje.getId()))
                .setNegativeButton("Ponovo skeniraj", (dialog, which) -> {
                    allowAutoStart = true;
                    startScan();
                })
                .show();
    }

    private void potvrdiDolazak(String predavanjeId) {
        if (!isAdded()) return;
        if (predavanjeId == null || predavanjeId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Nedostaje ID predavanja", Toast.LENGTH_SHORT).show();
            return;
        }

        supabaseClient.addEvidencija(predavanjeId, new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Dolazak evidentiran", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToLogin() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
