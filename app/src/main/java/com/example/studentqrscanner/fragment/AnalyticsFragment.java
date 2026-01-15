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

import java.util.ArrayList;
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

        List<AttendanceRecord> records = getDummyRecords();
        AttendanceAdapter adapter = new AttendanceAdapter(records);
        recyclerView.setAdapter(adapter);
    }

    private List<AttendanceRecord> getDummyRecords() {
        List<AttendanceRecord> list = new ArrayList<>();
        list.add(new AttendanceRecord("Matematika 1", "12", "NOV", "10:00", "D204", true));
        list.add(new AttendanceRecord("Fizika", "11", "NOV", "08:00", "A101", true));
        list.add(new AttendanceRecord("Programiranje 1", "10", "NOV", "12:00", "L102", false));
        list.add(new AttendanceRecord("Engleski Jezik", "09", "NOV", "14:00", "B303", true));
        list.add(new AttendanceRecord("Baze Podataka", "08", "NOV", "09:00", "L201", true));
        list.add(new AttendanceRecord("Matematika 1", "05", "NOV", "10:00", "D204", false));
        list.add(new AttendanceRecord("Fizika", "04", "NOV", "08:00", "A101", true));
        return list;
    }

    private void navigateToLogin() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
