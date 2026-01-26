package com.example.studentqrscanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.activity.LoginActivity;
import com.example.studentqrscanner.adapter.AttendanceAdapter;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.AttendanceRecord;

import java.util.List;

public class AnalyticsFragment extends Fragment {

    private SupabaseClient supabaseClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
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

        setupRecyclerView(view);
    }

    private void setupRecyclerView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        View emptyView = view.findViewById(R.id.tvNoAttendance);

        String studentId = supabaseClient.getStudentTableId();
        boolean isStudent = studentId != null && !studentId.trim().isEmpty();

        SupabaseClient.AttendanceRecordsCallback callback = new SupabaseClient.AttendanceRecordsCallback() {
            @Override
            public void onSuccess(List<AttendanceRecord> records) {
                if (!isAdded()) return;
                AttendanceAdapter adapter = new AttendanceAdapter(records);
                recyclerView.setAdapter(adapter);
                if (emptyView != null) {
                    emptyView.setVisibility(records == null || records.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
                if (emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                }
            }
        };

        if (isStudent) {
            supabaseClient.getEvidencijeForStudent(studentId, callback);
        } else {
            String profesorId = supabaseClient.getCurrentUserId();
            if (profesorId == null || profesorId.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Greška: profesor nije prijavljen.", Toast.LENGTH_LONG).show();
                if (emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                }
                return;
            }
            supabaseClient.getEvidencijeForProfesor(profesorId, callback);
        }
    }

    private void navigateToLogin() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
