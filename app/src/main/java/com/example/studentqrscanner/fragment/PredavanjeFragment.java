package com.example.studentqrscanner.fragment;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        adapter = new PredavanjeAdapter(new ArrayList<>(), this::prikaziQrDialog);
        rvPredavanja.setAdapter(adapter);

        dohvatiPodatke();

        FloatingActionButton fab = view.findViewById(R.id.fabDodajPredavanje);

        fab.setOnClickListener(v -> {
            BottomSheetDialog dialog =
                    new BottomSheetDialog(getContext());

            View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_dodaj_predavanje, null);
            dialog.setContentView(dialogView);

            View parent = (View) dialogView.getParent();
            if (parent != null) {
                parent.setBackgroundColor(Color.TRANSPARENT);
            }

            EditText etDatum = dialogView.findViewById(R.id.etDatum);
            EditText etNaslov = dialogView.findViewById(R.id.etNaslov);
            EditText etOpis = dialogView.findViewById(R.id.etOpis);
            EditText etUcionica = dialogView.findViewById(R.id.etUcionica);
            android.widget.Button btnDodaj = dialogView.findViewById(R.id.btnSpremiPredavanje);

            etDatum.setOnClickListener(v1 -> {
                java.util.Calendar kalendar = java.util.Calendar.getInstance();

                new android.app.DatePickerDialog(getContext(), R.style.MojPickerStil, (view1, y, m, d) -> {

                    new android.app.TimePickerDialog(getContext(), R.style.MojPickerStil, (view2, h, min) -> {
                        String odabrano = String.format("%02d.%02d.%d. %02d:%02d", d, m + 1, y, h, min);
                        etDatum.setText(odabrano);
                    }, kalendar.get(java.util.Calendar.HOUR_OF_DAY), kalendar.get(java.util.Calendar.MINUTE), true).show();

                }, kalendar.get(java.util.Calendar.YEAR), kalendar.get(java.util.Calendar.MONTH), kalendar.get(java.util.Calendar.DAY_OF_MONTH)).show();});

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
                                Toast.makeText(requireContext(), "UspjeÅ¡no spremljeno!", Toast.LENGTH_SHORT).show();
                                dohvatiPodatke();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                btnDodaj.setEnabled(true);
                                Toast.makeText(requireContext(), "GreÅ¡ka: " + error, Toast.LENGTH_LONG).show();
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
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> adapter.updateData(predavanja));
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

    private void prikaziQrDialog(Predavanje predavanje) {
        if (!isAdded() || getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_predavanje_qr, null);
        dialog.setContentView(dialogView);

        View parent = (View) dialogView.getParent();
        if (parent != null) {
            parent.setBackgroundColor(Color.TRANSPARENT);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvQrTitle);
        TextView tvWindow = dialogView.findViewById(R.id.tvQrWindow);
        TextView tvStatus = dialogView.findViewById(R.id.tvQrStatus);
        ImageView ivQr = dialogView.findViewById(R.id.ivPredavanjeQr);

        tvTitle.setText(predavanje.getNaslov() != null ? predavanje.getNaslov() : "Predavanje");

        Date startDate = parseDatum(predavanje.getDatum());
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault());
        if (startDate != null) {
            tvWindow.setText("Pocetak: " + displayFormat.format(startDate));
        } else {
            tvWindow.setText("Vrijeme pocetka nije dostupno.");
        }

        String qrPayload = buildQrPayload(predavanje);
        Bitmap bitmap = generateQrBitmap(qrPayload);
        if (bitmap != null) {
            ivQr.setImageBitmap(bitmap);
            ivQr.setVisibility(View.VISIBLE);
            tvStatus.setText("QR spreman za skeniranje.");
        } else {
            tvStatus.setText("Greska pri generisanju QR koda.");
            ivQr.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private String buildQrPayload(Predavanje predavanje) {
        String idValue = predavanje.getId() != null ? predavanje.getId() : (predavanje.getNaslov() != null ? predavanje.getNaslov() : "unknown");
        return "predavanjeId=" + idValue;
    }

    private Date parseDatum(String datum) {
        if (datum == null || datum.trim().isEmpty()) {
            return null;
        }

        String[] patterns = new String[]{
                "dd.MM.yyyy. HH:mm",
                "dd.MM.yyyy.",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                return sdf.parse(datum);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Bitmap generateQrBitmap(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 500, 500);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;
        } catch (WriterException e) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Greska pri generisanju QR koda.", Toast.LENGTH_SHORT).show();
            }
            return null;
        }
    }
}
