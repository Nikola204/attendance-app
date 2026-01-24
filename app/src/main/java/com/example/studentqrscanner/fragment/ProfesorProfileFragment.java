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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.activity.LoginActivity;
import com.example.studentqrscanner.activity.QrStyleSelectorActivity;
import com.example.studentqrscanner.adapter.KolegijAdapter;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.BaseUser;
import com.example.studentqrscanner.model.Kolegij;
import com.example.studentqrscanner.model.Profesor;

import java.util.List;

public class ProfesorProfileFragment extends Fragment {

    private TextView tvProfesorLastName;
    private ProgressBar progressBar;
    private SupabaseClient supabaseClient;

    private RecyclerView rvKolegiji;
    private KolegijAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profesor_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvProfesorLastName = view.findViewById(R.id.tvProfesorLastName);
        progressBar = view.findViewById(R.id.progressBarProfile);

        rvKolegiji = view.findViewById(R.id.rvKolegiji);
        rvKolegiji.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        Button btnChangeQrStyle = view.findViewById(R.id.btnChangeQrStyle);
        btnChangeQrStyle.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), QrStyleSelectorActivity.class);
            intent.putExtra("is_student", false); // Nije student = profesor
            startActivity(intent);
        });

        supabaseClient = new SupabaseClient(requireContext());

        if (!supabaseClient.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        loadProfile();
        loadKolegiji();
    }

    private void loadProfile() {
        showLoading(true);
        supabaseClient.fetchCurrentUser(new SupabaseClient.AuthCallback() {
            @Override
            public void onSuccess(BaseUser user) {
                if (!isAdded()) return;
                showLoading(false);
                if (user instanceof Profesor) {
                    Profesor profesor = (Profesor) user;
                    tvProfesorLastName.setText(profesor.getPrezime());
                } else {
                    Toast.makeText(requireContext(), "Profil nije dostupan.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(), "Greska: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadKolegiji() {
        String currentProfId = supabaseClient.getCurrentUserId();

        supabaseClient.getKolegijiByProfesor(currentProfId, new SupabaseClient.KolegijiCallback() {
            @Override
            public void onSuccess(List<Kolegij> kolegiji) {
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter = new KolegijAdapter(kolegiji);
                        rvKolegiji.setAdapter(adapter);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Greska kolegiji: " + error, Toast.LENGTH_SHORT).show();
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
}