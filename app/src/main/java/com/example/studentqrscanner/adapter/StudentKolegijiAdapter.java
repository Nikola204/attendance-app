package com.example.studentqrscanner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.model.Kolegij;

import java.util.ArrayList;
import java.util.List;

public class StudentKolegijiAdapter extends RecyclerView.Adapter<StudentKolegijiAdapter.ViewHolder> {

    public interface OnRemoveClickListener {
        void onRemoveClick(Kolegij kolegij);
    }

    private final List<Kolegij> items = new ArrayList<>();
    private final OnRemoveClickListener removeListener;

    public StudentKolegijiAdapter(OnRemoveClickListener removeListener) {
        this.removeListener = removeListener;
    }

    public void setItems(List<Kolegij> kolegiji) {
        items.clear();
        if (kolegiji != null) {
            items.addAll(kolegiji);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_kolegij, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Kolegij k = items.get(position);
        holder.tvTitle.setText(k.getNaziv() != null ? k.getNaziv() : "Kolegij");

        StringBuilder details = new StringBuilder();
        if (k.getStudij() != null && !k.getStudij().isEmpty()) {
            details.append(k.getStudij());
        }
        if (k.getGodina() > 0) {
            if (details.length() > 0) details.append(" • ");
            details.append(k.getGodina()).append(". god");
        }
        holder.tvSubtitle.setText(details.toString());

        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemoveClick(k);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        View btnRemove;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvKolegijTitle);
            tvSubtitle = itemView.findViewById(R.id.tvKolegijSubtitle);
            btnRemove = itemView.findViewById(R.id.btnRemoveKolegij);
        }
    }
}
