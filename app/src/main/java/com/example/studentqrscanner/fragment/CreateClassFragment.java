package com.example.studentqrscanner.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.Kolegij;

public class CreateClassFragment extends Fragment {

    private EditText etNaziv, etStudij, etGodina;
    private Button btnSpremi;
    private SupabaseClient supabaseClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_class, container, false);

        supabaseClient = new SupabaseClient(requireContext());

        etNaziv = view.findViewById(R.id.etNazivKolegija);
        etStudij = view.findViewById(R.id.etStudij);
        etGodina = view.findViewById(R.id.etGodina);
        btnSpremi = view.findViewById(R.id.btnSpremiKolegij);

        btnSpremi.setOnClickListener(v -> spremiKolegij());

        return view;
    }

    private void spremiKolegij() {
        String naziv = etNaziv.getText().toString().trim();
        String studij = etStudij.getText().toString().trim();
        String godinaStr = etGodina.getText().toString().trim();

        if (naziv.isEmpty() || studij.isEmpty() || godinaStr.isEmpty()) {
            Toast.makeText(getContext(), "Molimo popunite sva polja", Toast.LENGTH_SHORT).show();
            return;
        }

        String profesorId = supabaseClient.getCurrentUserId();
        if (profesorId == null) {
            Toast.makeText(getContext(), "Greška: ID profesora nije pronađen", Toast.LENGTH_SHORT).show();
            return;
        }

        Kolegij noviKolegij = new Kolegij(naziv, Integer.parseInt(godinaStr), studij, profesorId);

        btnSpremi.setEnabled(false);

        supabaseClient.addKolegij(noviKolegij, new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (isAdded() && getActivity() != null) {
                    Toast.makeText(getContext(), "Kolegij uspješno kreiran!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new ProfesorProfileFragment())
                            .commit();
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    btnSpremi.setEnabled(true);
                    Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
