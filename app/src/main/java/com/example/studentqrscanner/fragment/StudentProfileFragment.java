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
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.activity.LoginActivity;
import com.example.studentqrscanner.adapter.StudentKolegijiAdapter;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.BaseUser;
import com.example.studentqrscanner.model.Kolegij;
import com.example.studentqrscanner.model.Student;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.List;

public class StudentProfileFragment extends Fragment {

    private TextView tvFullName;
    private TextView tvBrojIndexa;
    private TextView tvStudij;
    private TextView tvGodina;
    private ImageView ivQrCode;
    private View cardInfo;
    private View cardQr;
    private View cardKolegiji;
    private ProgressBar progressBar;
    private RecyclerView rvKolegiji;
    private TextView tvNoKolegiji;
    private ImageButton btnAddKolegij;
    private StudentKolegijiAdapter kolegijiAdapter;
    private final List<Kolegij> currentKolegiji = new ArrayList<>();
    private String currentStudentId;

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
        cardInfo = view.findViewById(R.id.cardInfo);
        cardQr = view.findViewById(R.id.cardQr);
        cardKolegiji = view.findViewById(R.id.cardKolegiji);
        progressBar = view.findViewById(R.id.progressBarProfile);
        rvKolegiji = view.findViewById(R.id.rvKolegiji);
        tvNoKolegiji = view.findViewById(R.id.tvNoKolegiji);
        btnAddKolegij = view.findViewById(R.id.btnAddKolegij);

        supabaseClient = new SupabaseClient(requireContext());

        if (!supabaseClient.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setupKolegijiList();
        loadProfile();
    }

    private void setupKolegijiList() {
        kolegijiAdapter = new StudentKolegijiAdapter(kolegij -> {
            if (currentStudentId == null) return;
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Ukloni kolegij")
                    .setMessage("Ukloniti " + (kolegij.getNaziv() != null ? kolegij.getNaziv() : "kolegij") + "?")
                    .setPositiveButton("Da", (d, w) -> removeStudentFromKolegij(currentStudentId, kolegij))
                    .setNegativeButton("Ne", null)
                    .show();
        });
        rvKolegiji.setLayoutManager(new LinearLayoutManager(getContext()));
        rvKolegiji.setAdapter(kolegijiAdapter);
    }

    private void loadProfile() {
        showLoading(true);
        supabaseClient.fetchCurrentUser(new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess(BaseUser user) {
                if (!isAdded()) return;
                showLoading(false);
                toggleContentVisibility(true);
                if (user instanceof Student) {
                    Student student = (Student) user;
                    currentStudentId = student.getId();
                    tvFullName.setText(student.getFullName());
                    tvBrojIndexa.setText(student.getBrojIndexa());
                    tvStudij.setText(student.getStudij());
                    tvGodina.setText(String.valueOf(student.getGodina()));

                    // Generate and display QR code
                    generateQrCode(student);
                    btnAddKolegij.setOnClickListener(v -> showAddKolegijDialog(student.getId()));
                    loadStudentKolegiji(student.getId());
                } else {
                    Toast.makeText(requireContext(), "Profil nije dostupan.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                showLoading(false);
                toggleContentVisibility(false);
                supabaseClient.signOut();
                Toast.makeText(requireContext(),
                        "Greška pri dohvatanju profila: " + error,
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

    private void loadStudentKolegiji(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return;
        }
        supabaseClient.getKolegijiForStudent(studentId, new SupabaseClient.KolegijiCallback() {
            @Override
            public void onSuccess(List<Kolegij> kolegiji) {
                if (!isAdded()) return;
                currentKolegiji.clear();
                if (kolegiji != null) currentKolegiji.addAll(kolegiji);
                kolegijiAdapter.setItems(kolegiji);
                updateKolegijiEmptyState();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Greška pri dohvatu kolegija: " + error, Toast.LENGTH_LONG).show();
                updateKolegijiEmptyState();
            }
        });
    }

    private void updateKolegijiEmptyState() {
        if (tvNoKolegiji != null) {
            boolean empty = currentKolegiji.isEmpty();
            tvNoKolegiji.setVisibility(empty ? View.VISIBLE : View.GONE);
            rvKolegiji.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    private void showAddKolegijDialog(String studentId) {
        supabaseClient.getAllKolegiji(new SupabaseClient.KolegijiCallback() {
            @Override
            public void onSuccess(List<Kolegij> kolegiji) {
                if (!isAdded()) return;
                List<Kolegij> available = new ArrayList<>();
                for (Kolegij k : kolegiji) {
                    boolean already = false;
                    for (Kolegij owned : currentKolegiji) {
                        if (owned.getId() != null && owned.getId().equals(k.getId())) {
                            already = true;
                            break;
                        }
                    }
                    if (!already) available.add(k);
                }
                if (available.isEmpty()) {
                    Toast.makeText(requireContext(), "Nema dostupnih kolegija za upis.", Toast.LENGTH_LONG).show();
                    return;
                }
                CharSequence[] items = new CharSequence[available.size()];
                for (int i = 0; i < available.size(); i++) {
                    Kolegij k = available.get(i);
                    String title = k.getNaziv() != null ? k.getNaziv() : "Kolegij";
                    String subtitle = k.getStudij() != null ? k.getStudij() : "";
                    if (k.getGodina() > 0) {
                        subtitle = subtitle.isEmpty() ? (k.getGodina() + ". god") : (subtitle + " • " + k.getGodina() + ". god");
                    }
                    items[i] = subtitle.isEmpty() ? title : title + " (" + subtitle + ")";
                }

                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Odaberi kolegij")
                        .setItems(items, (dialog, which) -> {
                            Kolegij chosen = available.get(which);
                            enrollStudentInKolegij(studentId, chosen);
                        })
                        .setNegativeButton("Odustani", null)
                        .show();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Greška pri dohvatu kolegija: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void enrollStudentInKolegij(String studentId, Kolegij kolegij) {
        supabaseClient.addStudentToKolegij(studentId, kolegij.getId(), new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Upisano na " + kolegij.getNaziv(), Toast.LENGTH_SHORT).show();
                loadStudentKolegiji(studentId);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Greška pri upisu: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void removeStudentFromKolegij(String studentId, Kolegij kolegij) {
        supabaseClient.removeStudentFromKolegij(studentId, kolegij.getId(), new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Uklonjeno: " + kolegij.getNaziv(), Toast.LENGTH_SHORT).show();
                loadStudentKolegiji(studentId);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Greška pri uklanjanju: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        toggleContentVisibility(!show);
    }

    private void toggleContentVisibility(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        if (tvFullName != null) tvFullName.setVisibility(visibility);
        if (cardInfo != null) cardInfo.setVisibility(visibility);
        if (cardQr != null) cardQr.setVisibility(visibility);
        if (cardKolegiji != null) cardKolegiji.setVisibility(visibility);
        if (ivQrCode != null) ivQrCode.setVisibility(visibility);
    }

    /**
     * Generiranje QR koda na osnovu imena i broja indexa
     */
    private void generateQrCode(Student student) {
        if (student == null || student.getFullName() == null || student.getFullName().isEmpty()
                || student.getBrojIndexa() == null || student.getBrojIndexa().isEmpty()
                || student.getId() == null || student.getId().isEmpty()) {
            Toast.makeText(requireContext(), "Podaci za QR kod nisu dostupni", Toast.LENGTH_SHORT).show();
            return;
        }

        String qrContent = "studentId=" + student.getId()
                + "&index=" + student.getBrojIndexa()
                + "&name=" + student.getFullName();

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
