package com.example.studentqrscanner.activity;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studentqrscanner.R;
import com.example.studentqrscanner.util.LocaleManager;

public class SettingsActivity extends BaseActivity {

    private RadioGroup rgLanguage;
    private RadioButton rbHr;
    private RadioButton rbEn;
    private TextView tvCurrentLang;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        rgLanguage = findViewById(R.id.rgLanguage);
        rbHr = findViewById(R.id.rbHr);
        rbEn = findViewById(R.id.rbEn);
        tvCurrentLang = findViewById(R.id.tvCurrentLanguage);
        ImageButton btnBack = findViewById(R.id.btnBackSettings);

        btnBack.setOnClickListener(v -> finish());

        String current = LocaleManager.getSavedLanguage(this);
        if ("en".equalsIgnoreCase(current)) {
            rbEn.setChecked(true);
            setLanguageText(getString(R.string.settings_language_value_en));
        } else {
            rbHr.setChecked(true);
            setLanguageText(getString(R.string.settings_language_value_hr));
        }

        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbHr) {
                setLanguage("hr");
            } else if (checkedId == R.id.rbEn) {
                setLanguage("en");
            }
        });
    }

    private void setLanguage(String langCode) {
        LocaleManager.saveLanguage(this, langCode);
        String label = langCode.equalsIgnoreCase("en")
                ? getString(R.string.settings_language_value_en)
                : getString(R.string.settings_language_value_hr);
        setLanguageText(label);
        recreate();
    }

    private void setLanguageText(String lang) {
        tvCurrentLang.setText(lang);
        Toast.makeText(this, getString(R.string.settings_language) + ": " + lang, Toast.LENGTH_SHORT).show();
    }
}
