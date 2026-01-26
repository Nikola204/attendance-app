package com.example.studentqrscanner.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

public class LocaleManager {

    private static final String PREFS_NAME = "locale_prefs";
    private static final String KEY_LANG = "app_lang";
    private static final String DEFAULT_LANG = Locale.getDefault().getLanguage();

    public static Context wrapContext(Context context) {
        String lang = getSavedLanguage(context);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            //noinspection deprecation
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            return context;
        }
    }

    public static void saveLanguage(Context context, String lang) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String stored = prefs.getString(KEY_LANG, null);
        if (stored != null && !stored.trim().isEmpty()) {
            return stored.trim();
        }
        return DEFAULT_LANG;
    }

}
