package com.example.studentqrscanner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.model.AttendanceRecord;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private List<AttendanceRecord> records;

    public AttendanceAdapter(List<AttendanceRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecord record = records.get(position);
        holder.tvDateDay.setText(record.getDateDay());
        holder.tvDateMonth.setText(record.getDateMonth());
        holder.tvSubject.setText(record.getSubject());
        holder.tvTimeRoom.setText(record.getTime() + " | " + record.getRoom());

        if (record.isPresent()) {
            holder.viewStatus.setBackgroundResource(R.drawable.shape_circle_green);
        } else {
            holder.viewStatus.setBackgroundResource(R.drawable.shape_circle_red);
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateDay, tvDateMonth, tvSubject, tvTimeRoom;
        View viewStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateDay = itemView.findViewById(R.id.tvDateDay);
            tvDateMonth = itemView.findViewById(R.id.tvDateMonth);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvTimeRoom = itemView.findViewById(R.id.tvTimeRoom);
            viewStatus = itemView.findViewById(R.id.viewStatus);
        }
    }
}
