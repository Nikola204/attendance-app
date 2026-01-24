package com.example.studentqrscanner.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.example.studentqrscanner.model.BaseUser;
import com.example.studentqrscanner.model.Kolegij;
import com.example.studentqrscanner.model.Predavanje;
import com.example.studentqrscanner.model.Profesor;
import com.example.studentqrscanner.model.Student;
import com.example.studentqrscanner.model.UserRole;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import kotlinx.serialization.json.internal.JsonException;

public class SupabaseClient {

    private static final String SUPABASE_URL = "https://gjksdicomuuxrxqurqqu.supabase.co";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_cvBUkg_yYjkpRdf5D2VErg_KZ3fnj7V";

    private static final String AUTH_ENDPOINT = SUPABASE_URL + "/auth/v1/token?grant_type=password";
    private static final String STUDENTI_ENDPOINT = SUPABASE_URL + "/rest/v1/studenti";
    private static final String PROFESORI_ENDPOINT = SUPABASE_URL + "/rest/v1/profesori";
    private static final String KOLEGIJ_ENDPOINT = SUPABASE_URL + "/rest/v1/kolegij";

    private final String PREDAVANJE_ENDPOINT = SUPABASE_URL + "/rest/v1/predavanja";
    private final String EVIDENCIJA_ENDPOINT = SUPABASE_URL + "/rest/v1/evidencija";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Context context;

    public SupabaseClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public interface AuthCallback {
        void onSuccess(BaseUser user);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface KolegijiCallback {
        void onSuccess(List<Kolegij> kolegiji);
        void onError(String error);
    }

    private void postSuccess(AuthCallback callback, BaseUser user) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(user));
    }

    private void postError(AuthCallback callback, String error) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onError(error));
    }

    private String getAccessToken() {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        return prefs.getString("access_token", null);
    }

    private void saveStudentTableId(String id) {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("student_table_id", id).apply();
    }

    private String getStudentTableId() {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        return prefs.getString("student_table_id", null);
    }

    public String getCurrentUserId() {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        return prefs.getString("user_id", null);
    }

    private void saveAccessToken(String token) {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("access_token", token).apply();
    }

    private void saveUserId(String userId) {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("user_id", userId).apply();
    }

    public interface PredavanjaCallback {
        void onSuccess(List<Predavanje> predavanja);
        void onError(String error);
    }

    public interface PredavanjeDetailCallback {
        void onSuccess(Predavanje predavanje);
        void onError(String error);
    }

    public interface StudentIdCallback {
        void onSuccess(String studentId);
        void onError(String error);
    }

    public interface AttendanceListCallback {
        void onSuccess(List<com.example.studentqrscanner.model.AttendanceItem> items);
        void onError(String error);
    }

    public void signInWithEmail(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            try {
                if (PREDAVANJE_ENDPOINT == null) {
                    throw new Exception("Endpoint nije definiran!");
                }

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
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject authResponse = new JSONObject(response.toString());
                    String accessToken = authResponse.getString("access_token");

                    String userId = authResponse.getJSONObject("user").getString("id");

                    saveAccessToken(accessToken);
                    saveUserId(userId);

                    getUserProfile(accessToken, callback);
                } else {
                    JSONObject errorResponse = new JSONObject(response.toString());
                    String errorMsg = errorResponse.optString("error_description", "Greska pri loginu");
                    postError(callback, errorMsg);
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Sustav: " + e.getMessage()));
                e.printStackTrace();
                postError(callback, "Greska pri konekciji: " + e.getMessage());
            }
        });
    }

    private void getUserProfile(String accessToken, AuthCallback callback) {
        executor.execute(() -> {
            try {
                String[] parts = accessToken.split("\\.");
                String payloadStr = new String(Base64.decode(parts[1], Base64.URL_SAFE));
                JSONObject json = new JSONObject(payloadStr);
                String userId = json.getString("sub");

                String studentData = fetchDataFromTable(STUDENTI_ENDPOINT, userId, accessToken);
                if (studentData != null) {
                    JSONObject j = new JSONObject(studentData);
                    Student student = new Student();
                    String tableId = j.optString("id", userId);
                    student.setId(tableId);
                    saveStudentTableId(tableId);
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

    public void addKolegij(Kolegij kolegij, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("naziv", kolegij.getNaziv());
                payload.put("godina", String.valueOf(kolegij.getGodina()));
                payload.put("studij", kolegij.getStudij());
                payload.put("profesor_id", kolegij.getProfesorId());

                URL url = new URL(KOLEGIJ_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder err = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) err.append(line);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Baza kaze: " + err.toString()));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getKolegijiByProfesor(String profesorId, KolegijiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(KOLEGIJ_ENDPOINT + "?profesor_id=eq." + profesorId + "&select=*");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();

                    String jsonResponse = response.toString();

                    android.util.Log.d("SUPABASE_ODGOVOR", "Sirovi JSON iz baze: " + jsonResponse);

                    java.util.List<Kolegij> listaKolegija = new java.util.ArrayList<>();
                    org.json.JSONArray jsonArray = new org.json.JSONArray(jsonResponse);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        try{
                            org.json.JSONObject obj = jsonArray.getJSONObject(i);

                            String naziv = obj.optString("naziv", "");
                            String studij = obj.optString("studij", "");
                            String profId = obj.optString("profesor_id", "");
                            String id = obj.optString("id", null);

                            int godina = obj.optInt("godina", 0);

                            Kolegij k = new Kolegij(naziv, godina, studij, profId);
                            k.setId(id);

                            listaKolegija.add(k);

                            android.util.Log.d("SUPABASE_OBRADA", "Uspješno dodan kolegij: " + naziv + " sa ID: " + id);
                        } catch (Exception e)
                        {
                            android.util.Log.e("JSON_ERROR", "Greška pri čitanju pojedinog kolegija: " + e.getMessage());
                        }
                    }
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(listaKolegija));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Greška: " + responseCode));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void signOut() {
        SharedPreferences prefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

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

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }


    public void addPredavanje(Predavanje predavanje, SimpleCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {

                String datumZaBazu;
                try {
                    java.text.SimpleDateFormat ulazniFormat = new java.text.SimpleDateFormat("dd.MM.yyyy. HH:mm", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat izlazniFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
                    java.util.Date d = ulazniFormat.parse(predavanje.getDatum());
                    datumZaBazu = izlazniFormat.format(d);
                } catch (Exception e) {
                    datumZaBazu = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
                }

                JSONObject payload = new JSONObject();
                try {
                    payload.put("naslov", predavanje.getNaslov());
                    payload.put("opis", predavanje.getOpis());
                    payload.put("ucionica", predavanje.getUcionica());
                    payload.put("datum", datumZaBazu);
                    payload.put("kolegij_id", predavanje.getKolegijId());
                } catch (JsonException e) {
                    e.printStackTrace();
                }

                URL url = new URL(PREDAVANJE_ENDPOINT);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();

                String errorFromStream = "";
                if (code >= 400) {
                    try (Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                        errorFromStream = s.hasNext() ? s.next() : "Nema detalja";
                    } catch (Exception e) {
                        errorFromStream = "Nije moguće pročitati error stream";
                    }
                }

                final String finalError = errorFromStream;
                final int finalCode = code;

                mainHandler.post(() -> {
                    if (finalCode >= 200 && finalCode < 300) {
                        callback.onSuccess();
                    } else {
                        Log.e("SUPABASE_ERROR", "Status Code: " + finalCode);
                        Log.e("SUPABASE_ERROR", "Detalji: " + finalError);

                        callback.onError("Greška " + finalCode + ": " + finalError);
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }


    public void getPredavanja(String kolegijId, PredavanjaCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String urlString = PREDAVANJE_ENDPOINT + "?kolegij_id=eq." + kolegijId;
                URL url = new URL(urlString);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "[]";

                    List<Predavanje> lista = new ArrayList<>();
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Predavanje predavanje = new Predavanje(
                                obj.optString("ucionica"),
                                obj.optString("datum"),
                                obj.optString("kolegij_id"),
                                obj.optString("naslov"),
                                obj.optString("opis")
                        );
                        predavanje.setId(obj.optString("id", null));
                        predavanje.setCreatedAt(obj.optString("created_at", null));
                        lista.add(predavanje);
                    }

                    mainHandler.post(() -> callback.onSuccess(lista));
                } else {
                    mainHandler.post(() -> callback.onError("Greška pri dohvaćanju: " + code));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
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
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            String res = response.toString();
            if (res.startsWith("[") && res.length() > 2) {
                return res.substring(1, res.length() - 1);
            }
        }
        return null;
    }

    public void getPredavanjeById(String predavanjeId, PredavanjeDetailCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (predavanjeId == null || predavanjeId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje ID predavanja"));
                    return;
                }

                String encodedId = java.net.URLEncoder.encode(predavanjeId, java.nio.charset.StandardCharsets.UTF_8.toString());
                URL url = new URL(PREDAVANJE_ENDPOINT + "?id=eq." + encodedId);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "[]";

                    JSONArray array = new JSONArray(response);
                    if (array.length() == 0) {
                        mainHandler.post(() -> callback.onError("Predavanje nije pronađeno."));
                        return;
                    }

                    JSONObject obj = array.getJSONObject(0);
                    Predavanje predavanje = new Predavanje(
                            obj.optString("ucionica"),
                            obj.optString("datum"),
                            obj.optString("kolegij_id"),
                            obj.optString("naslov"),
                            obj.optString("opis")
                    );
                    predavanje.setId(obj.optString("id", null));
                    predavanje.setCreatedAt(obj.optString("created_at", null));

                    mainHandler.post(() -> callback.onSuccess(predavanje));
                } else {
                    Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                    String err = s.hasNext() ? s.next() : "Nepoznata greška";
                    mainHandler.post(() -> callback.onError("Greška " + code + ": " + err));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void fetchStudentIdByIndex(String brojIndexa, StudentIdCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (brojIndexa == null || brojIndexa.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje broj indexa."));
                    return;
                }

                String encoded = URLEncoder.encode(brojIndexa, StandardCharsets.UTF_8.toString());
                String urlString = STUDENTI_ENDPOINT + "?broj_indexa=eq." + encoded + "&select=id";

                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    InputStream is = conn.getInputStream();
                    Scanner s = new Scanner(is).useDelimiter("\\A");
                    String resp = s.hasNext() ? s.next() : "[]";

                    JSONArray arr = new JSONArray(resp);
                    if (arr.length() > 0) {
                        JSONObject obj = arr.getJSONObject(0);
                        String id = obj.optString("id", null);
                        if (id != null && !id.trim().isEmpty()) {
                            mainHandler.post(() -> callback.onSuccess(id));
                            return;
                        }
                    }
                    mainHandler.post(() -> callback.onError("Student nije pronađen."));
                } else {
                    Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                    String err = s.hasNext() ? s.next() : "Nepoznata greska";
                    mainHandler.post(() -> callback.onError("Greška " + code + ": " + err));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void addEvidencija(String predavanjeId, SimpleCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (predavanjeId == null || predavanjeId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje ID predavanja."));
                    return;
                }

                String studentId = getStudentTableId();
                if (studentId == null) {
                    studentId = getCurrentUserId();
                }
                if (studentId == null) {
                    mainHandler.post(() -> callback.onError("Korisnik nije prijavljen."));
                    return;
                }

                String datum = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

                JSONObject payload = new JSONObject();
                // If evidencija.id has FK to predavanja.id, set it explicitly; keep predavanje_id for schemas that use it.
                payload.put("id", predavanjeId);
                payload.put("predavanje_id", predavanjeId);
                payload.put("student_id", studentId);
                payload.put("datum", datum);

                URL url = new URL(EVIDENCIJA_ENDPOINT);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
                conn.setRequestProperty("Prefer", "return=minimal, resolution=ignore-duplicates");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                    String err = s.hasNext() ? s.next() : "Nepoznata greska";
                    String parsed = parseErrorMessage(err);
                    mainHandler.post(() -> callback.onError("Greska " + code + ": " + parsed));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void addEvidencijaZaStudenta(String predavanjeId, String studentId, SimpleCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (predavanjeId == null || predavanjeId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje ID predavanja."));
                    return;
                }
                if (studentId == null || studentId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje ID studenta."));
                    return;
                }

                String datum = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

                JSONObject payload = new JSONObject();
                payload.put("id", predavanjeId);
                payload.put("predavanje_id", predavanjeId);
                payload.put("student_id", studentId);
                payload.put("datum", datum);

                URL url = new URL(EVIDENCIJA_ENDPOINT);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
                conn.setRequestProperty("Prefer", "return=minimal, resolution=ignore-duplicates");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A");
                    String err = s.hasNext() ? s.next() : "Nepoznata greska";
                    String parsed = parseErrorMessage(err);
                    mainHandler.post(() -> callback.onError("Greska " + code + ": " + parsed));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void getPrisutniStudenti(String predavanjeId, AttendanceListCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (predavanjeId == null || predavanjeId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje ID predavanja"));
                    return;
                }

                // select=id,datum,studenti(ime,prezime,broj_indexa) from evidencija where predavanje_id=...
                // URL encoding for select value: *,studenti(ime,prezime,broj_indexa)
                String selectQuery = "id,datum,studenti(ime,prezime,broj_indexa)";
                String encodedSelect = URLEncoder.encode(selectQuery, StandardCharsets.UTF_8.toString());
                
                String urlString = EVIDENCIJA_ENDPOINT + "?predavanje_id=eq." + predavanjeId + "&select=" + encodedSelect;
                URL url = new URL(urlString);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "[]";

                    List<com.example.studentqrscanner.model.AttendanceItem> items = new ArrayList<>();
                    JSONArray array = new JSONArray(response);

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String id = obj.optString("id");
                        String date = obj.optString("datum"); // e.g., 2026-01-24

                        JSONObject studentObj = obj.optJSONObject("studenti");
                        String ime = "", prezime = "", index = "";
                        
                        if (studentObj != null) {
                            ime = studentObj.optString("ime", "");
                            prezime = studentObj.optString("prezime", "");
                            index = studentObj.optString("broj_indexa", "");
                        }

                        String fullName = ime + " " + prezime;
                        if (fullName.trim().isEmpty()) fullName = "Nepoznat student";

                        items.add(new com.example.studentqrscanner.model.AttendanceItem(id, fullName, index, date));
                    }

                    mainHandler.post(() -> callback.onSuccess(items));
                } else {
                    mainHandler.post(() -> callback.onError("Greška " + code));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void deleteEvidencija(String evidencijaId, SimpleCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (evidencijaId == null) {
                    mainHandler.post(() -> callback.onError("Nedostaje ID"));
                    return;
                }

                URL url = new URL(EVIDENCIJA_ENDPOINT + "?id=eq." + evidencijaId);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    mainHandler.post(() -> callback.onError("Greška pri brisanju: " + code));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private String parseErrorMessage(String raw) {
        try {
            JSONObject obj = new JSONObject(raw);
            String message = obj.optString("message", "");
            String details = obj.optString("details", "");
            String code = obj.optString("code", "");

            StringBuilder sb = new StringBuilder();
            if (!code.isEmpty()) sb.append(code).append(": ");
            if (!message.isEmpty()) sb.append(message);
            if (!details.isEmpty()) {
                if (sb.length() > 0) sb.append(" - ");
                sb.append(details);
            }

            if (sb.length() > 0) return sb.toString();
        } catch (Exception ignored) { }
        return raw;
    }
}
