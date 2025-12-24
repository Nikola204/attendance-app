package com.example.studentqrscanner.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.example.studentqrscanner.model.BaseUser;
import com.example.studentqrscanner.model.Student;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

public class SupabaseClient {

    private static final String SUPABASE_URL = "https://gjksdicomuuxrxqurqqu.supabase.co";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_cvBUkg_yYjkpRdf5D2VErg_KZ3fnj7V";

    private static final String AUTH_ENDPOINT = SUPABASE_URL + "/auth/v1/token?grant_type=password";
    private static final String STUDENTI_ENDPOINT = SUPABASE_URL + "/rest/v1/studenti";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Context context;

    public SupabaseClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public interface AuthCallback {
        void onSuccess(BaseUser user);

        void onError(String error);
    }

    private void postSuccess(AuthCallback callback, BaseUser user) {
        new Handler(Looper.getMainLooper()).post(() -> {
            callback.onSuccess(user);
        });
    }

    private void postError(AuthCallback callback, String error) {
        new Handler(Looper.getMainLooper()).post(() -> {
            callback.onError(error);
        });
    }


    /**
     * Login korisnika sa email i password
     */
    public void signInWithEmail(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Autentifikacija sa Supabase Auth
                JSONObject authPayload = new JSONObject();
                authPayload.put("email", email);
                authPayload.put("password", password);

                URL url = new URL(AUTH_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(authPayload.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader;

                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject authResponse = new JSONObject(response.toString());
                    String accessToken = authResponse.getString("access_token");

                    // Spremi token
                    saveAccessToken(accessToken);

                    // 2. Dohvati user podatke iz custom users tabele
                    getUserProfile(accessToken, callback);
                } else {
                    JSONObject errorResponse = new JSONObject(response.toString());
                    String errorMsg = errorResponse.optString("error_description", "Greška pri loginu");
                    callback.onError(errorMsg);
                }

            } catch (Exception e) {
                callback.onError("Greška pri konekciji: " + e.getMessage());
            }
        });
    }

    private String getUserIdFromAccessToken(String accessToken) throws Exception {
        String[] parts = accessToken.split("\\.");
        String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
        JSONObject json = new JSONObject(payload);
        return json.getString("sub"); // THIS IS THE UID
    }

    /**
     * Dohvati user profil iz custom users tabele
     */
    private void getUserProfile(String accessToken, AuthCallback callback) {
        try {
            String userId = getUserIdFromAccessToken(accessToken);

            URL url = new URL(STUDENTI_ENDPOINT + "?user_id=eq." + userId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseStr = response.toString();

            if (responseStr.startsWith("[") && responseStr.length() > 2) {
                responseStr = responseStr.substring(1, responseStr.length() - 1);
                JSONObject j = new JSONObject(responseStr);

                Student student = new Student();
                student.setIme(j.getString("ime"));
                student.setPrezime(j.getString("prezime"));
                student.setBrojIndexa(j.getString("broj_index"));
                student.setStudij(j.getString("studij"));
                student.setGodina(j.getInt("godina"));

                postSuccess(callback, student);
            } else {
                postError(callback, "Student nije pronađen");
            }

        } catch (Exception e) {
            postError(callback, "Greška pri dohvaćanju profila: " + e.getMessage());
        }
    }

    /**
     * Logout
     */
    public void signOut() {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    /**
     * Provjeri da li je korisnik prijavljen
     */
    public boolean isLoggedIn() {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        return prefs.getString("access_token", null) != null;
    }

    /**
     * Spremi access token
     */
    private void saveAccessToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("access_token", token).apply();
    }
}
