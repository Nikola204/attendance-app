package com.example.studentqrscanner.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.activity.LoginActivity;
import com.example.studentqrscanner.activity.QrStyleSelectorActivity;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.BaseUser;
import com.example.studentqrscanner.model.Student;
import com.example.studentqrscanner.util.CustomQrGenerator;
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
    private Student currentStudent; // Čuvamo trenutnog studenta za refresh

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
        progressBar = view.findViewById(R.id.progressBarProfile);

        Button btnShowQrStyles = view.findViewById(R.id.btnShowQrStyles);
        btnShowQrStyles.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), QrStyleSelectorActivity.class);
            intent.putExtra("is_student", true);
            startActivity(intent);
        });

        supabaseClient = new SupabaseClient(requireContext());

        if (!supabaseClient.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        loadProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Osvježi QR kod ako se korisnik vratio iz style selector-a
        if (currentStudent != null) {
            generateQrCode(currentStudent);
        }
    }

    private void loadProfile() {
        showLoading(true);
        supabaseClient.fetchCurrentUser(new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess(BaseUser user) {
                if (!isAdded()) return;
                showLoading(false);
                if (user instanceof Student) {
                    currentStudent = (Student) user;
                    tvFullName.setText(currentStudent.getFullName());
                    tvBrojIndexa.setText(currentStudent.getBrojIndexa());
                    tvStudij.setText(currentStudent.getStudij());
                    tvGodina.setText(String.valueOf(currentStudent.getGodina()));

                    // Generate and display QR code
                    generateQrCode(currentStudent);
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
     * Generiranje custom QR koda sa odabranim stilom za studenta
     */
    private void generateQrCode(Student student) {
        if (student == null || student.getFullName() == null || student.getFullName().isEmpty()
                || student.getBrojIndexa() == null || student.getBrojIndexa().isEmpty()
                || student.getId() == null || student.getId().isEmpty()) {
            Toast.makeText(requireContext(), "Podaci za QR kod nisu dostupni", Toast.LENGTH_SHORT).show();
            return;
        }

        // Učitaj odabrani stil iz preferences
        CustomQrGenerator.QrStyle selectedStyle = QrStyleSelectorActivity.getSavedStudentStyle(requireContext());

        // Generiši QR kod sa odabranim stilom
        String content = "studentId=" + student.getId() +
                "&index=" + student.getBrojIndexa() +
                "&name=" + student.getFullName();

        CustomQrGenerator generator = new CustomQrGenerator(requireContext());
        Bitmap qrBitmap = generator.generateStyledQr(content, selectedStyle, "Student: " + student.getBrojIndexa());

        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap);
        } else {
            Toast.makeText(requireContext(), "Greška pri generisanju QR koda", Toast.LENGTH_SHORT).show();
        }
    }
}
