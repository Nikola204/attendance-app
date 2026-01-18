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
        holder.tvNaslov.setText(p.getNaslov());
        holder.tvOpis.setText(p.getOpis());
        holder.tvDatum.setText(p.getDatum());
        holder.tvUcionica.setText(p.getUcionica());
    }

    @Override
    public int getItemCount() {
        return listaPredavanja != null ? listaPredavanja.size() : 0;
    }

    public static class PredavanjeViewHolder extends RecyclerView.ViewHolder {
        TextView tvNaslov, tvOpis, tvDatum, tvUcionica;

        public PredavanjeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNaslov = itemView.findViewById(R.id.tvStavkaNaslov);
            tvOpis = itemView.findViewById(R.id.tvStavkaOpis);
            tvDatum = itemView.findViewById(R.id.tvStavkaDatum);
            tvUcionica = itemView.findViewById(R.id.tvStavkaUcionica);
        }
    }
}