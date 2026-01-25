package com.example.studentqrscanner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.model.AttendanceItem;

import java.util.List;

public class AttendeesAdapter extends RecyclerView.Adapter<AttendeesAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(AttendanceItem item);
    }

    private List<AttendanceItem> items;
    private final OnDeleteClickListener deleteListener;

    public AttendeesAdapter(List<AttendanceItem> items, OnDeleteClickListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    public void updateData(List<AttendanceItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }
    
    public void removeItem(AttendanceItem item) {
        int position = items.indexOf(item);
        if (position >= 0) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceItem item = items.get(position);
        String fullName = item.getStudentName();
        String displayName = !TextUtils.isEmpty(fullName) ? fullName : "Nepoznati student";
        if (!TextUtils.isEmpty(item.getStudentIndex())) {
            displayName = displayName + " - " + item.getStudentIndex();
        }
        holder.tvName.setText(displayName);

        String date = item.getTimestamp();
        holder.tvIndex.setText(!TextUtils.isEmpty(date) ? date : "");

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIndex;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStudentName);
            tvIndex = itemView.findViewById(R.id.tvStudentIndex);
            btnDelete = itemView.findViewById(R.id.btnDeleteAttendance);
        }
    }
}
