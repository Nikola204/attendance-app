package com.example.studentqrscanner.config;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.studentqrscanner.model.User;
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

    private static final String SUPABASE_URL = "https://ovopwcqzcpnizulssxhg.supabase.co";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_d0opedPHgM-tg5FjJDkBcA_4-ZtWr8W";

    private static final String AUTH_ENDPOINT = SUPABASE_URL + "/auth/v1/token?grant_type=password";
    private static final String USER_ENDPOINT = SUPABASE_URL + "/rest/v1/users";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Context context;

    public SupabaseClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public interface AuthCallback {
        void onSuccess(User user);

        void onError(String error);
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
                    String userId = authResponse.getJSONObject("user").getString("id");

                    // Spremi token
                    saveAccessToken(accessToken);

                    // 2. Dohvati user podatke iz custom users tabele
                    getUserProfile(userId, email, accessToken, callback);
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

    /**
     * Dohvati user profil iz custom users tabele
     */
    private void getUserProfile(String userId, String email, String accessToken, AuthCallback callback) {
        try {
            URL url = new URL(USER_ENDPOINT + "?id=eq." + userId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse response
            String responseStr = response.toString();
            if (responseStr.startsWith("[") && responseStr.length() > 2) {
                // Ukloni [ i ]
                responseStr = responseStr.substring(1, responseStr.length() - 1);
                JSONObject userJson = new JSONObject(responseStr);

                User user = new User();
                user.setId(userJson.getString("id"));
                user.setEmail(userJson.getString("email"));
                user.setFullName(userJson.optString("full_name", ""));

                String roleStr = userJson.getString("role");
                UserRole role = UserRole.fromString(roleStr);

                if (role == null) {
                    callback.onError("Nevažeća user role");
                    return;
                }

                user.setRole(role);
                callback.onSuccess(user);
            } else {
                callback.onError("User profil nije pronađen");
            }

        } catch (Exception e) {
            callback.onError("Greška pri dohvaćanju profila: " + e.getMessage());
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
