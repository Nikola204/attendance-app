package com.example.studentqrscanner.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.example.studentqrscanner.model.BaseUser;
import com.example.studentqrscanner.model.Profesor;
import com.example.studentqrscanner.model.Student;
import com.example.studentqrscanner.model.UserRole;

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

    private static final String PROFESORI_ENDPOINT = SUPABASE_URL + "/rest/v1/profesori";

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
        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(user));
    }

    private void postError(AuthCallback callback, String error) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(error));
    }

    /**
     * Dohvati spremljeni access token ili null
     */
    private String getAccessToken() {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        return prefs.getString("access_token", null);
    }

    /**
     * Login korisnika sa email i password
     */
    public void signInWithEmail(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Autentifikacija sa Supabase auth
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

                    // 2. Dohvati user podatke iz custom users tablice
                    getUserProfile(accessToken, callback);
                } else {
                    JSONObject errorResponse = new JSONObject(response.toString());
                    String errorMsg = errorResponse.optString("error_description", "Greska pri loginu");
                    postError(callback, errorMsg);
                }

            } catch (Exception e) {
                postError(callback, "Greska pri konekciji: " + e.getMessage());
            }
        });
    }

    private String getUserIdFromAccessToken(String accessToken) throws Exception {
        String[] parts = accessToken.split("\\.");
        String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
        JSONObject json = new JSONObject(payload);
        return json.getString("sub"); // user id
    }

    /**
     * Dohvati user profil iz custom users tabele
     */
    private void getUserProfile(String accessToken, AuthCallback callback) {
        executor.execute(() -> {
            try {
                String userId = getUserIdFromAccessToken(accessToken);

                String studentData = fetchDataFromTable(STUDENTI_ENDPOINT, userId, accessToken);

                if (studentData != null) {
                    JSONObject j = new JSONObject(studentData);
                    Student student = new Student();
                    student.setEmail(j.optString("email", ""));
                    student.setIme(j.getString("ime"));
                    student.setPrezime(j.getString("prezime"));
                    student.setBrojIndexa(j.optString("broj_indexa", ""));
                    student.setStudij(j.optString("studij", ""));
                    student.setGodina(j.optInt("godina", 0));
                    student.setRole(UserRole.STUDENT);
                    postSuccess(callback, student);
                    return;
                }

                String profData = fetchDataFromTable(PROFESORI_ENDPOINT, userId, accessToken);

                if (profData != null) {
                    JSONObject j = new JSONObject(profData);
                    Profesor professor = new Profesor();
                    professor.setEmail(j.optString("mail", ""));
                    professor.setIme(j.getString("ime"));
                    professor.setPrezime(j.getString("prezime"));
                    professor.setRole(UserRole.PROFESOR);
                    postSuccess(callback, professor);
                    return;
                }

                signOut();
                postError(callback, "Korisnički podaci nisu pronađeni.");

            } catch (Exception e) {
                signOut();
                postError(callback, "Greška pri dohvaćanju profila: " + e.getMessage());
            }
        });
    }
    /**
     * Logout
     */
    public void signOut() {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    /**
     * Dohvati profil trenutnog korisnika koristeći spremljeni token
     */
    public void fetchCurrentUser(AuthCallback callback) {
        executor.execute(() -> {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                postError(callback, "Korisnik nije prijavljen");
                return;
            }
            getUserProfile(accessToken, callback);
        });
    }

    /**
     * Provjeri je li korisnik prijavljen
     */
    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    /**
     * Spremi access token
     */
    private void saveAccessToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("access_token", token).apply();
    }

    private String fetchDataFromTable(String endpoint, String userId, String accessToken) throws Exception {
        URL url = new URL(endpoint + "?id=eq." + userId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String res = response.toString();
            if (res.startsWith("[") && res.length() > 2) {
                return res.substring(1, res.length() - 1);
            }
        }
        return null;
    }
}
