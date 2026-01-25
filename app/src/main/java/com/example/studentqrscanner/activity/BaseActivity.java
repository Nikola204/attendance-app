package com.example.studentqrscanner.activity;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studentqrscanner.util.LocaleManager;

public abstract class BaseActivity extends AppCompatActivity {
    private String lastLang;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrapContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lastLang = LocaleManager.getSavedLanguage(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String saved = LocaleManager.getSavedLanguage(this);
        if (lastLang != null && saved != null && !lastLang.equalsIgnoreCase(saved)) {
            lastLang = saved;
            recreate();
            return;
        }
        lastLang = saved;
    }
}
