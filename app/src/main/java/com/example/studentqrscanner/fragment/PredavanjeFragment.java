package com.example.studentqrscanner.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.adapter.PredavanjeAdapter;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.Predavanje;

import java.util.ArrayList;
import java.util.List;

public class PredavanjeFragment extends Fragment {

    private String kolegijId;
    private String kolegijNaziv;
    private RecyclerView rvPredavanja;
    private PredavanjeAdapter adapter;

    private SupabaseClient supabaseClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_predavanje, container, false);

        supabaseClient = new SupabaseClient(requireContext());

        if (getArguments() != null) {
            kolegijId = getArguments().getString("kolegij_id");
            kolegijNaziv = getArguments().getString("kolegij_naziv");
        }

        TextView tvNaslov = view.findViewById(R.id.tvNaslovKolegija);
        if (kolegijNaziv != null) {
            tvNaslov.setText(kolegijNaziv);
        }

        rvPredavanja = view.findViewById(R.id.rvPredavanja);
        rvPredavanja.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Predavanje> fejkLista = new ArrayList<>();
        fejkLista.add(new Predavanje("Uučionica 101", "20.01.2024. 08:00", kolegijId, "title1", "desc1"));
        fejkLista.add(new Predavanje("Uučionica 202", "27.01.2024. 08:00", kolegijId, "title2", "desc2"));
        fejkLista.add(new Predavanje("Laboratorij 3", "03.02.2024. 08:00", kolegijId, "t", "t" ));

        adapter = new PredavanjeAdapter(fejkLista);
        rvPredavanja.setAdapter(adapter);

        dohvatiPodatke();

        com.google.android.material.floatingactionbutton.FloatingActionButton fab = view.findViewById(R.id.fabDodajPredavanje);

        fab.setOnClickListener(v -> {
            com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                    new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());

            View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_dodaj_predavanje, null);
            dialog.setContentView(dialogView);

            ((View) dialogView.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);

            EditText etDatum = dialogView.findViewById(R.id.etDatum);
            EditText etNaslov = dialogView.findViewById(R.id.etNaslov);
            EditText etOpis = dialogView.findViewById(R.id.etOpis);
            EditText etUcionica = dialogView.findViewById(R.id.etUcionica);
            android.widget.Button btnDodaj = dialogView.findViewById(R.id.btnSpremiPredavanje);

            etDatum.setOnClickListener(v1 -> {
                java.util.Calendar kalendar = java.util.Calendar.getInstance();

                new android.app.DatePickerDialog(getContext(), (view1, y, m, d) -> {

                    new android.app.TimePickerDialog(getContext(), (view2, h, min) -> {
                        String odabrano = String.format("%02d.%02d.%d. %02d:%02d", d, m + 1, y, h, min);
                        etDatum.setText(odabrano);
                    }, kalendar.get(java.util.Calendar.HOUR_OF_DAY), kalendar.get(java.util.Calendar.MINUTE), true).show();

                }, kalendar.get(java.util.Calendar.YEAR), kalendar.get(java.util.Calendar.MONTH), kalendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
            });

            btnDodaj.setOnClickListener(v2 -> {
                String naslov = etNaslov.getText() != null ? etNaslov.getText().toString().trim() : "";
                String ucionica = etUcionica.getText() != null ? etUcionica.getText().toString().trim() : "";
                String datum = etDatum.getText() != null ? etDatum.getText().toString().trim() : "";
                String opis = etOpis.getText() != null ? etOpis.getText().toString().trim() : "";

                if (naslov.isEmpty() || datum.isEmpty() || kolegijId == null) {
                    Toast.makeText(requireContext(), "Nedostaju podaci!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Predavanje novo = new Predavanje(ucionica, datum, kolegijId, naslov, opis);

                btnDodaj.setEnabled(false);

                supabaseClient.addPredavanje(novo, new SupabaseClient.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                                Toast.makeText(requireContext(), "Uspješno spremljeno!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                btnDodaj.setEnabled(true);
                                Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    }
                });
            });
            dialog.show();
        });

        return view;
    }

    private void dohvatiPodatke() {
        supabaseClient.getPredavanja(kolegijId, new SupabaseClient.PredavanjaCallback() {
            @Override
            public void onSuccess(List<Predavanje> predavanja) {
                if (isAdded()) {
                    adapter = new PredavanjeAdapter(predavanja);
                    rvPredavanja.setAdapter(adapter);
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}