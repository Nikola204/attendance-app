package com.example.studentqrscanner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.model.StudentAttendance;

import java.util.List;

public class StudentAttendanceAdapter extends RecyclerView.Adapter<StudentAttendanceAdapter.AttendanceViewHolder> {

    private List<StudentAttendance> items;

    public StudentAttendanceAdapter(List<StudentAttendance> items) {
        this.items = items;
    }

    public void updateData(List<StudentAttendance> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        StudentAttendance item = items.get(position);
        holder.tvName.setText(item.getFullNameWithIndex());
        holder.tvCount.setText(holder.itemView.getContext().getString(R.string.predavanje_attendance_count, item.getAttendanceCount()));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvCount;

        AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvCount = itemView.findViewById(R.id.tvAttendanceCount);
        }
    }
}
