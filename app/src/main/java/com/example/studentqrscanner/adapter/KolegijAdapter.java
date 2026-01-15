package com.example.studentqrscanner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.model.Kolegij;

import java.util.List;

public class KolegijAdapter extends RecyclerView.Adapter<KolegijAdapter.KolegijViewHolder> {

    private List<Kolegij> listaKolegija;

    public KolegijAdapter(List<Kolegij> listaKolegija) {
        this.listaKolegija = listaKolegija;
    }

    @NonNull
    @Override
    public KolegijViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kolegij, parent, false);
        return new KolegijViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KolegijViewHolder holder, int position) {
        Kolegij kolegij = listaKolegija.get(position);

        holder.tvNaziv.setText(kolegij.getNaziv());
        holder.tvStudij.setText(kolegij.getStudij());
        holder.tvGodina.setText("Godina: " + kolegij.getGodina());
    }

    @Override
    public int getItemCount() {
        return listaKolegija != null ? listaKolegija.size() : 0;
    }

    public static class KolegijViewHolder extends RecyclerView.ViewHolder {
        TextView tvNaziv, tvStudij, tvGodina;

        public KolegijViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNaziv = itemView.findViewById(R.id.item_naziv_kolegija);
            tvStudij = itemView.findViewById(R.id.item_studij);
            tvGodina = itemView.findViewById(R.id.item_godina);
        }
    }
}