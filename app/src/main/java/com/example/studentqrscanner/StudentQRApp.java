package com.example.studentqrscanner;

import android.app.Application;
import android.content.Context;

import com.example.studentqrscanner.util.LocaleManager;

public class StudentQRApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.wrapContext(base));
    }
}
