package com.example.studentqrscanner.fragment;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.studentqrscanner.R;
import com.example.studentqrscanner.activity.PortraitCaptureActivity;
import com.example.studentqrscanner.adapter.PredavanjeAdapter;
import com.example.studentqrscanner.adapter.StudentAttendanceAdapter;
import com.example.studentqrscanner.config.SupabaseClient;
import com.example.studentqrscanner.model.Predavanje;
import com.example.studentqrscanner.model.StudentAttendance;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
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
    private Predavanje targetPredavanjeForScan;
    private boolean scanningStudent = false;
    private TextView tvNoPredavanja;
    private RecyclerView rvStudenti;
    private StudentAttendanceAdapter studentiAdapter;
    private TextView tvNoStudenti;
    private final List<Predavanje> currentPredavanja = new ArrayList<>();

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
        rvPredavanja.setNestedScrollingEnabled(false);
        tvNoPredavanja = view.findViewById(R.id.tvNoPredavanja);

        rvStudenti = view.findViewById(R.id.rvStudenti);
        rvStudenti.setLayoutManager(new LinearLayoutManager(getContext()));
        rvStudenti.setNestedScrollingEnabled(false);
        tvNoStudenti = view.findViewById(R.id.tvNoStudenti);

        adapter = new PredavanjeAdapter(new ArrayList<>(), this::prikaziQrDialog);
        rvPredavanja.setAdapter(adapter);

        studentiAdapter = new StudentAttendanceAdapter(new ArrayList<>());
        rvStudenti.setAdapter(studentiAdapter);

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
                                Toast.makeText(requireContext(), "Uspješno spremljeno!", Toast.LENGTH_SHORT).show();
                                dohvatiPodatke();
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
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentPredavanja.clear();
                        if (predavanja != null) {
                            currentPredavanja.addAll(predavanja);
                        }
                        adapter.updateData(predavanja);
                        if (tvNoPredavanja != null) {
                            boolean empty = predavanja == null || predavanja.isEmpty();
                            tvNoPredavanja.setVisibility(empty ? View.VISIBLE : View.GONE);
                            tvNoPredavanja.setText(R.string.predavanja_empty);
                            rvPredavanja.setVisibility(empty ? View.GONE : View.VISIBLE);
                        }
                        dohvatiStudenteSaEvidencijom();
                    });
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
        ImageButton btnScanStudent = dialogView.findViewById(R.id.btnScanStudent);
        ImageButton btnViewAttendees = dialogView.findViewById(R.id.btnViewAttendees);

        tvTitle.setText(predavanje.getNaslov() != null ? predavanje.getNaslov() : "Predavanje");

        Date startDate = parseDatum(predavanje.getDatum());
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault());
        if (startDate != null) {
            tvWindow.setText(getString(R.string.qr_dialog_start, displayFormat.format(startDate)));
        } else {
            tvWindow.setText(R.string.qr_dialog_start_unavailable);
        }

        String qrPayload = buildQrPayload(predavanje);
        Bitmap bitmap = generateQrBitmap(qrPayload);
        if (bitmap != null) {
            ivQr.setImageBitmap(bitmap);
            ivQr.setVisibility(View.VISIBLE);
            tvStatus.setText(R.string.qr_dialog_ready);
        } else {
            tvStatus.setText(R.string.qr_dialog_generate_error);
            ivQr.setVisibility(View.GONE);
        }

        btnScanStudent.setOnClickListener(v -> {
            targetPredavanjeForScan = predavanje;
            startStudentScan();
        });

        if (btnViewAttendees != null) {
            btnViewAttendees.setOnClickListener(v -> {
                dialog.dismiss();
                prikaziEvidencijuDialog(predavanje);
            });
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
                Toast.makeText(requireContext(), "Greška pri generisanju QR koda.", Toast.LENGTH_SHORT).show();
            }
            return null;
        }
    }

    private void startStudentScan() {
        if (!isAdded()) return;
        if (targetPredavanjeForScan == null || targetPredavanjeForScan.getId() == null || targetPredavanjeForScan.getId().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Nedostaje ID predavanja.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (scanningStudent) return;
        scanningStudent = true;
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt(getString(R.string.qr_fragment_prompt_student));
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            scanningStudent = false;
            if (result.getContents() != null) {
                handleStudentScanResult(result.getContents());
            } else if (isAdded()) {
                Toast.makeText(requireContext(), "Skeniranje otkazano", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleStudentScanResult(String contents) {
        if (!isAdded()) return;
        if (targetPredavanjeForScan == null || targetPredavanjeForScan.getId() == null) {
            Toast.makeText(requireContext(), "Nedostaje predavanje za evidenciju.", Toast.LENGTH_SHORT).show();
            return;
        }

        StudentScanData data = parseStudentScan(contents);
        if (data.studentId != null && !data.studentId.isEmpty()) {
            potvrdiDolazakZaStudenta(data.studentId);
        } else if (data.index != null && !data.index.isEmpty()) {
            supabaseClient.fetchStudentIdByIndex(data.index, new SupabaseClient.StudentIdCallback() {
                @Override
                public void onSuccess(String studentId) {
                    if (!isAdded()) return;
                    potvrdiDolazakZaStudenta(studentId);
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Student nije pronađen: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(requireContext(), "QR ne sadrži podatke o studentu.", Toast.LENGTH_LONG).show();
        }
    }

    private void potvrdiDolazakZaStudenta(String studentId) {
        if (!isAdded()) return;
        if (targetPredavanjeForScan == null || targetPredavanjeForScan.getId() == null || studentId == null || studentId.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Nedostaju ID predavanja ili studenta.", Toast.LENGTH_SHORT).show();
            return;
        }

        supabaseClient.addEvidencijaZaStudenta(targetPredavanjeForScan.getId(), studentId, new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Dolazak studenta evidentiran.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void dohvatiStudenteSaEvidencijom() {
        List<String> predavanjeIds = new ArrayList<>();
        for (Predavanje p : currentPredavanja) {
            if (p != null && p.getId() != null && !p.getId().trim().isEmpty()) {
                predavanjeIds.add(p.getId().trim());
            }
        }

        supabaseClient.getStudentAttendanceForKolegij(kolegijId, predavanjeIds, new SupabaseClient.StudentAttendanceCallback() {
            @Override
            public void onSuccess(List<StudentAttendance> items) {
                if (!isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    studentiAdapter.updateData(items);
                    boolean empty = items == null || items.isEmpty();
                    if (tvNoStudenti != null) {
                        tvNoStudenti.setVisibility(empty ? View.VISIBLE : View.GONE);
                    }
                    if (rvStudenti != null) {
                        rvStudenti.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private StudentScanData parseStudentScan(String raw) {
        StudentScanData data = new StudentScanData();
        if (raw == null) return data;

        String lower = raw.toLowerCase(Locale.US);

        int idIndex = lower.indexOf("studentid=");
        if (idIndex >= 0) {
            String idPart = raw.substring(idIndex + "studentId=".length());
            int ampIndex = idPart.indexOf("&");
            if (ampIndex >= 0) {
                idPart = idPart.substring(0, ampIndex);
            }
            data.studentId = idPart.trim();
        }

        int indexIndex = lower.indexOf("index=");
        if (indexIndex >= 0) {
            String idxPart = raw.substring(indexIndex + "index=".length());
            int ampIndex = idxPart.indexOf("&");
            if (ampIndex >= 0) {
                idxPart = idxPart.substring(0, ampIndex);
            }
            data.index = idxPart.trim();
        } else if (lower.contains("broj_indexa=")) {
            String idxPart = raw.substring(lower.indexOf("broj_indexa=") + "broj_indexa=".length());
            int ampIndex = idxPart.indexOf("&");
            if (ampIndex >= 0) {
                idxPart = idxPart.substring(0, ampIndex);
            }
            data.index = idxPart.trim();
        } else if (raw.contains(" - ")) {
            String[] parts = raw.split(" - ");
            data.index = parts[parts.length - 1].trim();
        }

        return data;
    }

    private void prikaziEvidencijuDialog(Predavanje predavanje) {
        if (!isAdded() || getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_attendees, null);
        dialog.setContentView(dialogView);

        View parent = (View) dialogView.getParent();
        if (parent != null) {
            parent.setBackgroundColor(Color.TRANSPARENT);
        }

        RecyclerView rvAttendees = dialogView.findViewById(R.id.rvAttendees);
        TextView tvNoAttendees = dialogView.findViewById(R.id.tvNoAttendees);
        android.widget.Button btnClose = dialogView.findViewById(R.id.btnCloseAttendees);

        rvAttendees.setLayoutManager(new LinearLayoutManager(getContext()));
        
        com.example.studentqrscanner.adapter.AttendeesAdapter adapter = new com.example.studentqrscanner.adapter.AttendeesAdapter(
                new ArrayList<>(),
                item -> {
                    // On delete click
                    new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Brisanje evidencije")
                            .setMessage("Jeste li sigurni da želite obrisati evidenciju za studenta " + item.getStudentName() + "?")
                            .setPositiveButton("Obriši", (d, w) -> {
                                supabaseClient.deleteEvidencija(item.getEvidencijaId(), new SupabaseClient.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        if (isAdded() && getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                Toast.makeText(requireContext(), "Evidencija obrisana.", Toast.LENGTH_SHORT).show();
                                                // Refresh list (easiest way is to remove item from adapter)
                                                // We need reference to adapter here.
                                                // Or just reload data. Let's just remove from adapter for smoothness.
                                                // Actually we can't easily access adapter method inside lambda unless final or specialized.
                                                // Since we are inside the adapter callback, 'adapter' variable isn't initialized yet if defined below.
                                                // But we can refactor.
                                            });
                                        }
                                    }
                                    @Override
                                    public void onError(String error) {
                                        if (isAdded()) {
                                             getActivity().runOnUiThread(() -> 
                                                Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_SHORT).show()
                                             );
                                        }
                                    }
                                });
                            })
                            .setNegativeButton("Odustani", null)
                            .show();
                }
        );
        
        // We need to set the adapter's delete listener properly to handle UI updates. 
        // The above listener handles the API call but not the UI update within the adapter directly (unless we refresh).
        // Let's rewrite the adapter initiation to include the UI update logic.
        
        adapter = new com.example.studentqrscanner.adapter.AttendeesAdapter(
                new ArrayList<>(),
                item -> {
                        new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Brisanje evidencije")
                            .setMessage("Obriši dolazak za: " + item.getStudentName() + "?")
                            .setPositiveButton("Da", (d, w) -> {
                                supabaseClient.deleteEvidencija(item.getEvidencijaId(), new SupabaseClient.SimpleCallback() {
                                    @Override
                                    public void onSuccess() {
                                        if (!isAdded()) return;
                                        getActivity().runOnUiThread(() -> {
                                            Toast.makeText(requireContext(), "Obrisano.", Toast.LENGTH_SHORT).show();
                                            // Refresh list by reloading
                                            loadAttendees(predavanje.getId(), rvAttendees, tvNoAttendees);
                                        });
                                    }
                                    @Override
                                    public void onError(String error) {
                                        if (!isAdded()) return;
                                        getActivity().runOnUiThread(() -> 
                                            Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                });
                            })
                            .setNegativeButton("Ne", null)
                            .show();
                }
        );

        rvAttendees.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        loadAttendees(predavanje.getId(), rvAttendees, tvNoAttendees);

        dialog.show();
    }

    private void loadAttendees(String predavanjeId, RecyclerView rv, TextView tvEmpty) {
        supabaseClient.getPrisutniStudenti(predavanjeId, new SupabaseClient.AttendanceListCallback() {
            @Override
            public void onSuccess(List<com.example.studentqrscanner.model.AttendanceItem> items) {
                if (!isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    com.example.studentqrscanner.adapter.AttendeesAdapter adp = (com.example.studentqrscanner.adapter.AttendeesAdapter) rv.getAdapter();
                    if (adp != null) {
                        adp.updateData(items);
                    }
                    if (items.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rv.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Greška: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private static class StudentScanData {
        String studentId;
        String index;
    }
}
