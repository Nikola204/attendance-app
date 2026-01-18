package com.example.studentqrscanner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentqrscanner.R;
import com.example.studentqrscanner.model.Predavanje;
import java.util.List;

public class PredavanjeAdapter extends RecyclerView.Adapter<PredavanjeAdapter.PredavanjeViewHolder> {

    private List<Predavanje> listaPredavanja;

    public PredavanjeAdapter(List<Predavanje> listaPredavanja) {
        this.listaPredavanja = listaPredavanja;
    }

    @NonNull
    @Override
    public PredavanjeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_predavanje, parent, false);
        return new PredavanjeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PredavanjeViewHolder holder, int position) {
        Predavanje p = listaPredavanja.get(position);
        holder.tvDatum.setText(p.getDatum());
        holder.tvUcionica.setText("Učionica: " + p.getUcionica());
    }

    @Override
    public int getItemCount() {
        return listaPredavanja != null ? listaPredavanja.size() : 0;
    }

    public static class PredavanjeViewHolder extends RecyclerView.ViewHolder {
        TextView tvDatum, tvUcionica;

        public PredavanjeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDatum = itemView.findViewById(R.id.item_datum_predavanja);
            tvUcionica = itemView.findViewById(R.id.item_ucionica);
        }
    }
}