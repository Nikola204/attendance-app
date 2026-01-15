package com.example.studentqrscanner.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.activity.LoginActivity;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.BaseUser;
import com.example.studentqrscanner.model.Student;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class StudentProfileFragment extends Fragment {

    private TextView tvFullName;
    private TextView tvBrojIndexa;
    private TextView tvStudij;
    private TextView tvGodina;
    private ImageView ivQrCode;
    private ProgressBar progressBar;

    private SupabaseClient supabaseClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvFullName = view.findViewById(R.id.tvFullName);
        tvBrojIndexa = view.findViewById(R.id.tvBrojIndexa);
        tvStudij = view.findViewById(R.id.tvStudij);
        tvGodina = view.findViewById(R.id.tvGodina);
        ivQrCode = view.findViewById(R.id.ivQrCode);
        ivQrCode = view.findViewById(R.id.ivQrCode);
        progressBar = view.findViewById(R.id.progressBarProfile);

        supabaseClient = new SupabaseClient(requireContext());

        if (!supabaseClient.isLoggedIn()) {
            navigateToLogin();
            return;
        }


        loadProfile();
    }

    private void loadProfile() {
        showLoading(true);
        supabaseClient.fetchCurrentUser(new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess(BaseUser user) {
                if (!isAdded()) return;
                showLoading(false);
                if (user instanceof Student) {
                    Student student = (Student) user;
                    tvFullName.setText(student.getFullName());
                    tvBrojIndexa.setText(student.getBrojIndexa());
                    tvStudij.setText(student.getStudij());
                    tvGodina.setText(String.valueOf(student.getGodina()));

                    // Generate and display QR code
                    generateQrCode(student.getFullName(), student.getBrojIndexa());
                } else {
                    Toast.makeText(requireContext(), "Profil nije dostupan.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                showLoading(false);
                supabaseClient.signOut();
                Toast.makeText(requireContext(),
                        "Greska pri dohvatanju profila: " + error,
                        Toast.LENGTH_LONG).show();
                navigateToLogin();
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

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Generiranje QR koda na osnovu imena i broja indexa
     */
    private void generateQrCode(String fullName, String brojIndexa) {
        if (fullName == null || fullName.isEmpty() || brojIndexa == null || brojIndexa.isEmpty()) {
            Toast.makeText(requireContext(), "Podaci za QR kod nisu dostupni", Toast.LENGTH_SHORT).show();
            return;
        }

        String qrContent = fullName + " - " + brojIndexa;

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 500, 500);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Toast.makeText(requireContext(), "Greška pri generisanju QR koda", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
