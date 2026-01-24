package com.example.studentqrscanner.activity;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.adapter.QrStyleAdapter;
import com.example.studentqrscanner.util.CustomQrGenerator;

/**
 * Aktivnost gdje student/profesor bira svoj omiljeni QR stil
 */
public class QrStyleSelectorActivity extends AppCompatActivity {

    private RecyclerView rvQrStyles;
    private QrStyleAdapter adapter;
    private Button btnSaveStyle;
    private TextView tvTitle;

    private boolean isStudent; // true = student, false = profesor
    private CustomQrGenerator.QrStyle selectedStyle;

    public static final String PREF_NAME = "QrStylePrefs";
    public static final String KEY_STUDENT_STYLE = "student_qr_style";
    public static final String KEY_PROFESSOR_STYLE = "professor_qr_style";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_style_selector);

        // Provjeri da li je student ili profesor
        isStudent = getIntent().getBooleanExtra("is_student", true);

        tvTitle = findViewById(R.id.tvSelectorTitle);
        rvQrStyles = findViewById(R.id.rvQrStyles);
        btnSaveStyle = findViewById(R.id.btnSaveStyle);

        tvTitle.setText(isStudent ? "Izaberi stil svog QR koda" : "Izaberi stil za predavanja");

        // Učitaj sačuvani stil
        loadSavedStyle();

        // Postavi grid layout (2 kolone)
        rvQrStyles.setLayoutManager(new GridLayoutManager(this, 2));

        // Dobavi stilove na osnovu tipa korisnika
        CustomQrGenerator.QrStyle[] availableStyles = isStudent ?
                CustomQrGenerator.QrStyle.getStudentStyles() :
                CustomQrGenerator.QrStyle.getProfessorStyles();

        // Kreiraj adapter
        adapter = new QrStyleAdapter(this, availableStyles, isStudent, selectedStyle,
                style -> {
                    selectedStyle = style;
                    adapter.setSelectedStyle(style);
                    btnSaveStyle.setEnabled(true);
                });

        rvQrStyles.setAdapter(adapter);

        // Dugme za spremanje
        btnSaveStyle.setOnClickListener(v -> saveStyle());
    }

    /**
     * Učitava sačuvani stil iz SharedPreferences
     */
    private void loadSavedStyle() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String styleName = prefs.getString(
                isStudent ? KEY_STUDENT_STYLE : KEY_PROFESSOR_STYLE,
                isStudent ? "STUDENT_BLUE" : "LECTURE_BLUE"
        );

        try {
            selectedStyle = CustomQrGenerator.QrStyle.valueOf(styleName);
        } catch (Exception e) {
            selectedStyle = isStudent ?
                    CustomQrGenerator.QrStyle.STUDENT_BLUE :
                    CustomQrGenerator.QrStyle.LECTURE_BLUE;
        }
    }

    /**
     * Sprema odabrani stil
     */
    private void saveStyle() {
        if (selectedStyle == null) {
            Toast.makeText(this, "Odaberi stil prvo!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(
                isStudent ? KEY_STUDENT_STYLE : KEY_PROFESSOR_STYLE,
                selectedStyle.name()
        );
        editor.apply();

        Toast.makeText(this, "Stil sačuvan! ✅", Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Helper metoda za dobijanje sačuvanog student stila
     */
    public static CustomQrGenerator.QrStyle getSavedStudentStyle(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String styleName = prefs.getString(KEY_STUDENT_STYLE, "STUDENT_BLUE");
        try {
            return CustomQrGenerator.QrStyle.valueOf(styleName);
        } catch (Exception e) {
            return CustomQrGenerator.QrStyle.STUDENT_BLUE;
        }
    }

    /**
     * Helper metoda za dobijanje sačuvanog profesor stila
     */
    public static CustomQrGenerator.QrStyle getSavedProfessorStyle(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String styleName = prefs.getString(KEY_PROFESSOR_STYLE, "LECTURE_BLUE");
        try {
            return CustomQrGenerator.QrStyle.valueOf(styleName);
        } catch (Exception e) {
            return CustomQrGenerator.QrStyle.LECTURE_BLUE;
        }
    }
}
