package com.example.studentqrscanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class StudentProfileFragment extends Fragment {

    private TextView tvFullName;
    private TextView tvBrojIndexa;
    private TextView tvStudij;
    private TextView tvGodina;
    private Button btnLogout;
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
        btnLogout = view.findViewById(R.id.btnLogout);
        progressBar = view.findViewById(R.id.progressBarProfile);

        supabaseClient = new SupabaseClient(requireContext());

        if (!supabaseClient.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        btnLogout.setOnClickListener(v -> {
            supabaseClient.signOut();
            navigateToLogin();
        });

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
        btnLogout.setEnabled(!show);
    }
}
