package com.example.studentqrscanner.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.example.studentqrscanner.model.BaseUser;
import com.example.studentqrscanner.model.Kolegij;
import com.example.studentqrscanner.model.Predavanje;
import com.example.studentqrscanner.model.Profesor;
import com.example.studentqrscanner.model.Student;
import com.example.studentqrscanner.model.UserRole;
import com.example.studentqrscanner.model.StudentAttendance;

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
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
    private static final String SIGNUP_ENDPOINT = SUPABASE_URL + "/auth/v1/signup";
    private static final String STUDENTI_ENDPOINT = SUPABASE_URL + "/rest/v1/studenti";
    private static final String PROFESORI_ENDPOINT = SUPABASE_URL + "/rest/v1/profesori";
    private static final String KOLEGIJ_ENDPOINT = SUPABASE_URL + "/rest/v1/kolegij";
    private static final String STUDENT_KOLEGIJ_ENDPOINT = SUPABASE_URL + "/rest/v1/student_kolegij";

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

    public String getStudentTableId() {
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

    public interface AttendanceRecordsCallback {
        void onSuccess(List<com.example.studentqrscanner.model.AttendanceRecord> records);
        void onError(String error);
    }

    public interface StudentAttendanceCallback {
        void onSuccess(List<StudentAttendance> items);
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
                    String errorMsg = errorResponse.optString("error_description", "Greška pri loginu");
                    postError(callback, errorMsg);
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Sustav: " + e.getMessage()));
                e.printStackTrace();
                postError(callback, "Greška pri konekciji: " + e.getMessage());
            }
        });
    }

    public void signUpUser(String email,
                           String password,
                           boolean isStudent,
                           String ime,
                           String prezime,
                           String brojIndexa,
                           String studij,
                           Integer godina,
                           AuthCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("email", email);
                payload.put("password", password);

                URL url = new URL(SIGNUP_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                BufferedReader reader;
                if (code >= 200 && code < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                if (code >= 200 && code < 300) {
                    String respStr = response.toString();
                    if (respStr == null || respStr.trim().isEmpty()) {
                        postError(callback, "Prazan odgovor pri registraciji");
                        return;
                    }
                    JSONObject resp;
                    try {
                        resp = new JSONObject(respStr);
                    } catch (Exception je) {
                        postError(callback, "Nevaljan JSON odgovor: " + respStr);
                        return;
                    }

                    String accessToken = resp.optString("access_token", null);
                    JSONObject sessionObj = resp.optJSONObject("session");
                    if (accessToken == null && sessionObj != null) {
                        accessToken = sessionObj.optString("access_token", null);
                    }

                    JSONObject userObj = resp.optJSONObject("user");
                    if (userObj == null && sessionObj != null) {
                        userObj = sessionObj.optJSONObject("user");
                    }
                    String userId = userObj != null ? userObj.optString("id", null) : null;

                    if (accessToken == null) {
                        postError(callback, "Nedostaje access token (signup)");
                        return;
                    }
                    if (userId == null || userId.trim().isEmpty()) {
                        postError(callback, "Nedostaje user id (signup)");
                        return;
                    }

                    saveAccessToken(accessToken);
                    saveUserId(userId);

                    String profileError;
                    if (isStudent) {
                        profileError = insertStudentProfile(userId, email, ime, prezime, brojIndexa, studij, godina, accessToken);
                    } else {
                        profileError = insertProfessorProfile(userId, email, ime, prezime, accessToken);
                    }

                    if (profileError != null) {
                        postError(callback, "Greška pri spremanju profila: " + profileError);
                        return;
                    }

                    if (isStudent) {
                        saveStudentTableId(userId);
                        Student student = new Student();
                        student.setId(userId);
                        student.setEmail(email);
                        student.setIme(ime);
                        student.setPrezime(prezime);
                        student.setBrojIndexa(brojIndexa);
                        student.setStudij(studij);
                        student.setGodina(godina != null ? godina : 1);
                        student.setRole(UserRole.STUDENT);
                        postSuccess(callback, student);
                    } else {
                        Profesor profesor = new Profesor();
                        profesor.setEmail(email);
                        profesor.setIme(ime);
                        profesor.setPrezime(prezime);
                        profesor.setRole(UserRole.PROFESOR);
                        postSuccess(callback, profesor);
                    }
                } else {
                    String respStr = response.toString();
                    String msg = "Neuspješna registracija";
                    try {
                        JSONObject err = new JSONObject(respStr);
                        msg = err.optString("msg", err.optString("error_description", msg));
                    } catch (Exception ignored) {
                        if (respStr != null && !respStr.isEmpty()) msg = respStr;
                    }
                    postError(callback, msg);
                }
            } catch (Exception e) {
                postError(callback, "Greška: " + e.getMessage());
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
                    student.setEmail(j.optString("mail", j.optString("email", "")));
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

                    List<Kolegij> listaKolegija = parseKolegijArray(jsonResponse);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(listaKolegija));
                } else {
                    if (handleAuthExpired(responseCode, null, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                        return;
                    }
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
                // Do not force id to predavanjeId; let DB generate unique id so multiple students can be inserted.
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
                    if (handleAuthExpired(code, parsed, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                        return;
                    }
                    mainHandler.post(() -> callback.onError("Greška " + code + ": " + parsed));
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
                // Do not set id manually; allow multiple students per lecture.
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
                    if (handleAuthExpired(code, parsed, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                        return;
                    }
                    mainHandler.post(() -> callback.onError("Greška " + code + ": " + parsed));
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

                // Step 1: fetch attendance rows (id, datum, student_id) for this lecture.
                String selectQuery = URLEncoder.encode("id,datum,student_id", StandardCharsets.UTF_8.toString());
                String urlString = EVIDENCIJA_ENDPOINT + "?predavanje_id=eq." + predavanjeId + "&select=" + selectQuery;
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
                    JSONArray rawArray = new JSONArray(response);

                    List<JSONObject> rawRows = new ArrayList<>();
                    Set<String> studentIds = new HashSet<>();

                    for (int i = 0; i < rawArray.length(); i++) {
                        JSONObject obj = rawArray.getJSONObject(i);
                        rawRows.add(obj);
                        String sid = obj.optString("student_id", "");
                        if (sid != null && !sid.trim().isEmpty()) {
                            studentIds.add(sid.trim());
                        }
                    }

                    Map<String, JSONObject> students = fetchStudentsByIds(studentIds);

                    for (JSONObject obj : rawRows) {
                        String id = obj.optString("id");
                        String date = obj.optString("datum"); // e.g., 2026-01-24
                        String sid = obj.optString("student_id", "");

                        JSONObject studentObj = students.get(sid);
                        String ime = "", prezime = "", index = "";

                        if (studentObj != null) {
                            ime = studentObj.optString("ime", "");
                            prezime = studentObj.optString("prezime", "");
                            index = studentObj.optString("broj_indexa", "");
                        }

                        items.add(new com.example.studentqrscanner.model.AttendanceItem(id, ime, prezime, index, date));
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

    public void getAllEvidencije(AttendanceRecordsCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String selectQuery = URLEncoder.encode("id,datum,predavanje_id,student_id", StandardCharsets.UTF_8.toString());
                String urlString = EVIDENCIJA_ENDPOINT + "?select=" + selectQuery;
                URL url = new URL(urlString);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "[]";

                    JSONArray rawArray = new JSONArray(response);
                    List<JSONObject> rawRows = new ArrayList<>();
                    Set<String> studentIds = new HashSet<>();
                    Set<String> predavanjeIds = new HashSet<>();

                    for (int i = 0; i < rawArray.length(); i++) {
                        JSONObject obj = rawArray.getJSONObject(i);
                        rawRows.add(obj);

                        String sid = obj.optString("student_id", "");
                        if (sid != null && !sid.trim().isEmpty()) {
                            studentIds.add(sid.trim());
                        }

                        String pid = obj.optString("predavanje_id", "");
                        if (pid != null && !pid.trim().isEmpty()) {
                            predavanjeIds.add(pid.trim());
                        }
                    }

                    Map<String, JSONObject> students = fetchStudentsByIds(studentIds);
                    Map<String, JSONObject> predavanja = fetchPredavanjaByIds(predavanjeIds);

                    List<com.example.studentqrscanner.model.AttendanceRecord> items = new ArrayList<>();
                    for (JSONObject obj : rawRows) {
                        String id = obj.optString("id");
                        String date = obj.optString("datum");
                        String sid = obj.optString("student_id", "");
                        String pid = obj.optString("predavanje_id", "");

                        JSONObject studentObj = students.get(sid);
                        JSONObject predavanjeObj = predavanja.get(pid);

                        String lectureTitle = "";
                        String lectureRoom = "";
                        String lectureDate = date;
                        if (predavanjeObj != null) {
                            lectureTitle = predavanjeObj.optString("naslov", "");
                            lectureRoom = predavanjeObj.optString("ucionica", "");
                            String pd = predavanjeObj.optString("datum", "");
                            if (pd != null && !pd.trim().isEmpty()) {
                                lectureDate = pd;
                            }
                        }

                        String studentIndex = "";
                        String studentFullName = "";
                        if (studentObj != null) {
                            studentIndex = studentObj.optString("broj_indexa", "");
                            String ime = studentObj.optString("ime", "");
                            String prezime = studentObj.optString("prezime", "");
                            studentFullName = (ime + " " + prezime).trim();
                        }

                        String combinedRoom = lectureRoom;

                        java.util.Date parsed = null;
                        try {
                            parsed = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(lectureDate);
                        } catch (Exception ignored) { }

                        String day = "";
                        String month = "";
                        if (parsed != null) {
                            day = new java.text.SimpleDateFormat("dd", java.util.Locale.getDefault()).format(parsed);
                            month = new java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(parsed).toUpperCase(java.util.Locale.getDefault());
                        }

                        // time field reused to carry index to the UI; subject shows lecture title; room field carries classroom only.
                        com.example.studentqrscanner.model.AttendanceRecord record =
                                new com.example.studentqrscanner.model.AttendanceRecord(
                                        lectureTitle,
                                        day,
                                        month,
                                        buildStudentLabel(studentFullName, studentIndex),
                                        combinedRoom,
                                        true
                                );
                        items.add(record);
                    }

                    mainHandler.post(() -> callback.onSuccess(items));
                } else {
                    mainHandler.post(() -> callback.onError("GreÅ¡ka " + code));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void getEvidencijeForProfesor(String profesorId, AttendanceRecordsCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (profesorId == null || profesorId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje ID profesora"));
                    return;
                }

                // 1) Fetch courses for this professor.
                String kolegijUrl = KOLEGIJ_ENDPOINT + "?profesor_id=eq." + profesorId + "&select=id";
                conn = (HttpURLConnection) new URL(kolegijUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                final int codeKolegij = conn.getResponseCode();
                if (codeKolegij < 200 || codeKolegij >= 300) {
                    if (handleAuthExpired(codeKolegij, null, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                        return;
                    }
                    mainHandler.post(() -> callback.onError("Greška " + codeKolegij + " pri dohvaćanju kolegija"));
                    return;
                }

                Scanner sK = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String kolegijResp = sK.hasNext() ? sK.next() : "[]";
                JSONArray kolegijArr = new JSONArray(kolegijResp);
                Set<String> kolegijIds = new HashSet<>();
                for (int i = 0; i < kolegijArr.length(); i++) {
                    JSONObject obj = kolegijArr.getJSONObject(i);
                    String kid = obj.optString("id", "");
                    if (kid != null && !kid.trim().isEmpty()) {
                        kolegijIds.add(kid.trim());
                    }
                }
                conn.disconnect();
                conn = null;

                if (kolegijIds.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                    return;
                }

                // 2) Fetch lectures for these courses.
                String predavanjaUrl = PREDAVANJE_ENDPOINT + "?kolegij_id=in.(" + String.join(",", kolegijIds) + ")&select=id,naslov,ucionica,datum";
                conn = (HttpURLConnection) new URL(predavanjaUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                final int codePredavanja = conn.getResponseCode();
                if (codePredavanja < 200 || codePredavanja >= 300) {
                    if (handleAuthExpired(codePredavanja, null, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                        return;
                    }
                    mainHandler.post(() -> callback.onError("Greška " + codePredavanja + " pri dohvaćanju predavanja"));
                    return;
                }

                Scanner sP = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String predavanjaResp = sP.hasNext() ? sP.next() : "[]";
                JSONArray predavanjaArr = new JSONArray(predavanjaResp);
                Map<String, JSONObject> predavanjaMap = new HashMap<>();
                Set<String> predavanjeIds = new HashSet<>();
                for (int i = 0; i < predavanjaArr.length(); i++) {
                    JSONObject obj = predavanjaArr.getJSONObject(i);
                    String pid = obj.optString("id", "");
                    if (pid != null && !pid.trim().isEmpty()) {
                        predavanjeIds.add(pid.trim());
                        predavanjaMap.put(pid.trim(), obj);
                    }
                }
                conn.disconnect();
                conn = null;

                if (predavanjeIds.isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                    return;
                }

                // 3) Fetch attendance rows for those lectures.
                String evidencijaUrl = EVIDENCIJA_ENDPOINT + "?predavanje_id=in.(" + String.join(",", predavanjeIds) + ")&select=id,datum,predavanje_id,student_id";
                conn = (HttpURLConnection) new URL(evidencijaUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                final int codeEvidencija = conn.getResponseCode();
                if (codeEvidencija < 200 || codeEvidencija >= 300) {
                    if (handleAuthExpired(codeEvidencija, null, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                        return;
                    }
                    mainHandler.post(() -> callback.onError("Greška " + codeEvidencija + " pri dohvaćanju evidencija"));
                    return;
                }

                Scanner sE = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String evidencijaResp = sE.hasNext() ? sE.next() : "[]";
                JSONArray rawArray = new JSONArray(evidencijaResp);
                List<JSONObject> rawRows = new ArrayList<>();
                Set<String> studentIds = new HashSet<>();

                for (int i = 0; i < rawArray.length(); i++) {
                    JSONObject obj = rawArray.getJSONObject(i);
                    rawRows.add(obj);

                    String sid = obj.optString("student_id", "");
                    if (sid != null && !sid.trim().isEmpty()) {
                        studentIds.add(sid.trim());
                    }
                }

                Map<String, JSONObject> students = fetchStudentsByIds(studentIds);

                List<com.example.studentqrscanner.model.AttendanceRecord> items = new ArrayList<>();
                for (JSONObject obj : rawRows) {
                    String date = obj.optString("datum");
                    String sid = obj.optString("student_id", "");
                    String pid = obj.optString("predavanje_id", "");

                    JSONObject studentObj = students.get(sid);
                    JSONObject predavanjeObj = predavanjaMap.get(pid);

                    String lectureTitle = "";
                    String lectureRoom = "";
                    String lectureDate = date;
                    if (predavanjeObj != null) {
                        lectureTitle = predavanjeObj.optString("naslov", "");
                        lectureRoom = predavanjeObj.optString("ucionica", "");
                        String pd = predavanjeObj.optString("datum", "");
                        if (pd != null && !pd.trim().isEmpty()) {
                            lectureDate = pd;
                        }
                    }

                    String studentIndex = "";
                    String studentFullName = "";
                    if (studentObj != null) {
                        studentIndex = studentObj.optString("broj_indexa", "");
                        String ime = studentObj.optString("ime", "");
                        String prezime = studentObj.optString("prezime", "");
                        studentFullName = (ime + " " + prezime).trim();
                    }

                    String combinedRoom = lectureRoom;

                    java.util.Date parsed = null;
                    try {
                        parsed = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(lectureDate);
                    } catch (Exception ignored) { }

                    String day = "";
                    String month = "";
                    if (parsed != null) {
                        day = new java.text.SimpleDateFormat("dd", java.util.Locale.getDefault()).format(parsed);
                        month = new java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(parsed).toUpperCase(java.util.Locale.getDefault());
                    }

                    com.example.studentqrscanner.model.AttendanceRecord record =
                            new com.example.studentqrscanner.model.AttendanceRecord(
                                    lectureTitle,
                                    day,
                                    month,
                                    buildStudentLabel(studentFullName, studentIndex),
                                    combinedRoom,
                                    true
                            );
                    items.add(record);
                }

                mainHandler.post(() -> callback.onSuccess(items));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void getAllKolegiji(KolegijiCallback callback) {
        executor.execute(() -> {
            try {
                URL url = new URL(KOLEGIJ_ENDPOINT + "?select=*");
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

                    List<Kolegij> listaKolegija = parseKolegijArray(response.toString());
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(listaKolegija));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Greška: " + responseCode));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getStudentAttendanceForKolegij(String kolegijId, List<String> predavanjeIds, StudentAttendanceCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (kolegijId == null || kolegijId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje kolegij ID."));
                    return;
                }

                String encodedKolegijId = URLEncoder.encode(kolegijId, StandardCharsets.UTF_8.toString());
                String urlString = STUDENT_KOLEGIJ_ENDPOINT + "?kolegij_id=eq." + encodedKolegijId;
                URL url = new URL(urlString);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    String err = "";
                    try (Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                        err = s.hasNext() ? s.next() : "";
                    } catch (Exception ignored) {}
                    if (handleAuthExpired(code, err, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                        return;
                    }
                    String parsed = parseErrorMessage(err);
                    mainHandler.post(() -> callback.onError("Greška: " + (parsed.isEmpty() ? code : parsed)));
                    return;
                }

                String response = "";
                try (Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A")) {
                    response = s.hasNext() ? s.next() : "[]";
                }

                JSONArray arr = new JSONArray(response);
                Set<String> studentIds = new HashSet<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String sid = obj.optString("student_id", "");
                    if (sid != null && !sid.trim().isEmpty()) {
                        studentIds.add(sid.trim());
                    }
                }

                Map<String, JSONObject> studentsMap = fetchStudentsByIds(studentIds);

                Map<String, Integer> attendanceCounts = new HashMap<>();
                if (predavanjeIds != null && !predavanjeIds.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String id : predavanjeIds) {
                        if (id == null || id.trim().isEmpty()) continue;
                        if (sb.length() > 0) sb.append(",");
                        sb.append(id.trim());
                    }

                    if (sb.length() > 0) {
                        HttpURLConnection evConn = null;
                        try {
                            String evidUrl = EVIDENCIJA_ENDPOINT + "?predavanje_id=in.(" + sb + ")&select=student_id";
                            URL eUrl = new URL(evidUrl);
                            evConn = (HttpURLConnection) eUrl.openConnection();
                            evConn.setRequestMethod("GET");
                            evConn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                            evConn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                            int evCode = evConn.getResponseCode();
                            if (evCode >= 200 && evCode < 300) {
                                String evResp = "";
                                try (Scanner s = new Scanner(evConn.getInputStream()).useDelimiter("\\A")) {
                                    evResp = s.hasNext() ? s.next() : "[]";
                                }

                                JSONArray evArr = new JSONArray(evResp);
                                for (int i = 0; i < evArr.length(); i++) {
                                    JSONObject obj = evArr.getJSONObject(i);
                                    String sid = obj.optString("student_id", "");
                                    if (sid != null && !sid.trim().isEmpty()) {
                                        String key = sid.trim();
                                        attendanceCounts.put(key, attendanceCounts.getOrDefault(key, 0) + 1);
                                    }
                                }
                            } else {
                                String err = "";
                                try (Scanner s = new Scanner(evConn.getErrorStream()).useDelimiter("\\A")) {
                                    err = s.hasNext() ? s.next() : "";
                                } catch (Exception ignored) {}
                                if (handleAuthExpired(evCode, err, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                                    return;
                                }
                                String parsed = parseErrorMessage(err);
                                mainHandler.post(() -> callback.onError("Greška: " + (parsed.isEmpty() ? evCode : parsed)));
                                return;
                            }
                        } finally {
                            if (evConn != null) evConn.disconnect();
                        }
                    }
                }

                List<StudentAttendance> result = new ArrayList<>();
                for (String sid : studentIds) {
                    JSONObject sObj = studentsMap.get(sid);
                    String ime = "";
                    String prezime = "";
                    String index = "";
                    if (sObj != null) {
                        ime = sObj.optString("ime", "");
                        prezime = sObj.optString("prezime", "");
                        index = sObj.optString("broj_indexa", "");
                    }
                    int count = attendanceCounts.getOrDefault(sid, 0);
                    result.add(new StudentAttendance(sid, ime, prezime, index, count));
                }

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void getEvidencijeForStudent(String studentId, AttendanceRecordsCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (studentId == null || studentId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje ID studenta"));
                    return;
                }

                String selectQuery = URLEncoder.encode("id,datum,predavanje_id,student_id", StandardCharsets.UTF_8.toString());
                String urlString = EVIDENCIJA_ENDPOINT + "?student_id=eq." + studentId + "&select=" + selectQuery;
                URL url = new URL(urlString);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "[]";

                    JSONArray rawArray = new JSONArray(response);
                    List<JSONObject> rawRows = new ArrayList<>();
                    Set<String> studentIds = new HashSet<>();
                    Set<String> predavanjeIds = new HashSet<>();

                    for (int i = 0; i < rawArray.length(); i++) {
                        JSONObject obj = rawArray.getJSONObject(i);
                        rawRows.add(obj);

                        String sid = obj.optString("student_id", "");
                        if (sid != null && !sid.trim().isEmpty()) {
                            studentIds.add(sid.trim());
                        }

                        String pid = obj.optString("predavanje_id", "");
                        if (pid != null && !pid.trim().isEmpty()) {
                            predavanjeIds.add(pid.trim());
                        }
                    }

                    Map<String, JSONObject> students = fetchStudentsByIds(studentIds);
                    Map<String, JSONObject> predavanja = fetchPredavanjaByIds(predavanjeIds);

                    List<com.example.studentqrscanner.model.AttendanceRecord> items = new ArrayList<>();
                    for (JSONObject obj : rawRows) {
                        String date = obj.optString("datum");
                        String sid = obj.optString("student_id", "");
                        String pid = obj.optString("predavanje_id", "");

                        JSONObject studentObj = students.get(sid);
                        JSONObject predavanjeObj = predavanja.get(pid);

                        String lectureTitle = "";
                        String lectureRoom = "";
                        String lectureDate = date;
                        if (predavanjeObj != null) {
                            lectureTitle = predavanjeObj.optString("naslov", "");
                            lectureRoom = predavanjeObj.optString("ucionica", "");
                            String pd = predavanjeObj.optString("datum", "");
                            if (pd != null && !pd.trim().isEmpty()) {
                                lectureDate = pd;
                            }
                        }

                        String studentIndex = "";
                        String studentFullName = "";
                        if (studentObj != null) {
                            studentIndex = studentObj.optString("broj_indexa", "");
                            String ime = studentObj.optString("ime", "");
                            String prezime = studentObj.optString("prezime", "");
                            studentFullName = (ime + " " + prezime).trim();
                        }

                        String combinedRoom = lectureRoom;

                        java.util.Date parsed = null;
                        try {
                            parsed = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(lectureDate);
                        } catch (Exception ignored) { }

                        String day = "";
                        String month = "";
                        if (parsed != null) {
                            day = new java.text.SimpleDateFormat("dd", java.util.Locale.getDefault()).format(parsed);
                            month = new java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault()).format(parsed).toUpperCase(java.util.Locale.getDefault());
                        }

                        com.example.studentqrscanner.model.AttendanceRecord record =
                                new com.example.studentqrscanner.model.AttendanceRecord(
                                        lectureTitle,
                                        day,
                                        month,
                                        buildStudentLabel(studentFullName, studentIndex),
                                        combinedRoom,
                                        true
                                );
                        items.add(record);
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

    public void getKolegijiForStudent(String studentId, KolegijiCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (studentId == null || studentId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje ID studenta"));
                    return;
                }

                String urlString = STUDENT_KOLEGIJ_ENDPOINT + "?student_id=eq." + studentId + "&select=kolegij_id";
                URL url = new URL(urlString);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = s.hasNext() ? s.next() : "[]";

                    Set<String> kolegijIds = new HashSet<>();
                    JSONArray arr = new JSONArray(response);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String kid = obj.optString("kolegij_id", "");
                        if (kid != null && !kid.trim().isEmpty()) {
                            kolegijIds.add(kid.trim());
                        }
                    }

                    List<Kolegij> lista = fetchKolegijiByIds(kolegijIds);
                    mainHandler.post(() -> callback.onSuccess(lista));
                } else {
                    String err = "";
                    try (Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                        err = s.hasNext() ? s.next() : "";
                    } catch (Exception ignored) {}
                    if (handleAuthExpired(code, err, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                        return;
                    }
                    mainHandler.post(() -> callback.onError("Greška: " + code));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void addStudentToKolegij(String studentId, String kolegijId, SimpleCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (studentId == null || studentId.trim().isEmpty() || kolegijId == null || kolegijId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje student ili kolegij ID."));
                    return;
                }

                JSONObject payload = new JSONObject();
                payload.put("student_id", studentId);
                payload.put("kolegij_id", kolegijId);

                URL url = new URL(STUDENT_KOLEGIJ_ENDPOINT);
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
                    String err = "";
                    try (Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                        err = s.hasNext() ? s.next() : "";
                    } catch (Exception ignored) {}
                    if (handleAuthExpired(code, err, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                        return;
                    }
                    mainHandler.post(() -> callback.onError("Greška: " + code));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Sustav: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    public void removeStudentFromKolegij(String studentId, String kolegijId, SimpleCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                if (studentId == null || studentId.trim().isEmpty() || kolegijId == null || kolegijId.trim().isEmpty()) {
                    mainHandler.post(() -> callback.onError("Nedostaje student ili kolegij ID."));
                    return;
                }

                String urlString = STUDENT_KOLEGIJ_ENDPOINT + "?student_id=eq." + studentId + "&kolegij_id=eq." + kolegijId;
                URL url = new URL(urlString);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    String err = "";
                    try (Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                        err = s.hasNext() ? s.next() : "";
                    } catch (Exception ignored) {}
                    if (handleAuthExpired(code, err, () -> callback.onError("Sesija je istekla. Prijavite se ponovo."))) {
                        return;
                    }
                    mainHandler.post(() -> callback.onError("Greška: " + code));
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

    private String insertStudentProfile(String userId, String email, String ime, String prezime,
                                        String brojIndexa, String studij, Integer godina, String accessToken) {
        if (userId == null || userId.trim().isEmpty()) {
            return "Nedostaje user id (profile student)";
        }
        JSONObject payload = new JSONObject();
        safePut(payload, "id", userId);
        safePut(payload, "mail", safeStr(email)); // studenti table uses 'mail' like profesori
        safePut(payload, "ime", safeStr(ime));
        safePut(payload, "prezime", safeStr(prezime));
        safePut(payload, "broj_indexa", safeStr(brojIndexa));
        safePut(payload, "studij", safeStr(studij));
        safePut(payload, "godina", godina != null ? godina : 1);

        return insertProfile(STUDENTI_ENDPOINT, payload, accessToken);
    }

    private String insertProfessorProfile(String userId, String email, String ime, String prezime, String accessToken) {
        if (userId == null || userId.trim().isEmpty()) {
            return "Nedostaje user id (profile profesor)";
        }
        JSONObject payload = new JSONObject();
        safePut(payload, "id", userId);
        safePut(payload, "mail", safeStr(email));
        safePut(payload, "ime", safeStr(ime));
        safePut(payload, "prezime", safeStr(prezime));
        return insertProfile(PROFESORI_ENDPOINT, payload, accessToken);
    }

    private String safeStr(String value) {
        return value == null ? "" : value;
    }

    private void safePut(JSONObject obj, String key, Object value) {
        try {
            obj.put(key, value == null ? JSONObject.NULL : value);
        } catch (Exception ignored) {
        }
    }

    /**
     * Posts the given JSON payload to a table endpoint using both apikey and session token.
     * Returns null on success, otherwise an error message string.
     */
    private String insertProfile(String endpoint, JSONObject payload, String accessToken) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Prefer", "return=minimal");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return null;
            }

            String err = "";
            try (Scanner s = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                err = s.hasNext() ? s.next() : "";
            } catch (Exception ignored) {}

            return "HTTP " + code + " " + err;
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private Map<String, JSONObject> fetchStudentsByIds(Set<String> studentIds) {
        Map<String, JSONObject> result = new HashMap<>();
        if (studentIds == null || studentIds.isEmpty()) {
            return result;
        }

        HttpURLConnection conn = null;
        try {
            String inList = String.join(",", studentIds);
            String urlString = STUDENTI_ENDPOINT + "?id=in.(" + inList + ")&select=id,ime,prezime,broj_indexa";
            URL url = new URL(urlString);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String resp = s.hasNext() ? s.next() : "[]";

                JSONArray arr = new JSONArray(resp);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String id = obj.optString("id", "");
                    if (id != null && !id.trim().isEmpty()) {
                        result.put(id.trim(), obj);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return result;
    }

    private Map<String, JSONObject> fetchPredavanjaByIds(Set<String> predavanjeIds) {
        Map<String, JSONObject> result = new HashMap<>();
        if (predavanjeIds == null || predavanjeIds.isEmpty()) {
            return result;
        }

        HttpURLConnection conn = null;
        try {
            String inList = String.join(",", predavanjeIds);
            String urlString = PREDAVANJE_ENDPOINT + "?id=in.(" + inList + ")&select=id,naslov,opis,ucionica,datum";
            URL url = new URL(urlString);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String resp = s.hasNext() ? s.next() : "[]";

                JSONArray arr = new JSONArray(resp);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String id = obj.optString("id", "");
                    if (id != null && !id.trim().isEmpty()) {
                        result.put(id.trim(), obj);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return result;
    }

    private List<Kolegij> fetchKolegijiByIds(Set<String> kolegijIds) {
        List<Kolegij> lista = new ArrayList<>();
        if (kolegijIds == null || kolegijIds.isEmpty()) return lista;

        HttpURLConnection conn = null;
        try {
            String inList = String.join(",", kolegijIds);
            String urlString = KOLEGIJ_ENDPOINT + "?id=in.(" + inList + ")&select=*";
            URL url = new URL(urlString);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String resp = s.hasNext() ? s.next() : "[]";
                lista = parseKolegijArray(resp);
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return lista;
    }

    private List<Kolegij> parseKolegijArray(String jsonResponse) {
        List<Kolegij> listaKolegija = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject obj = jsonArray.getJSONObject(i);

                    String naziv = obj.optString("naziv", "");
                    String studij = obj.optString("studij", "");
                    String profId = obj.optString("profesor_id", "");
                    String id = obj.optString("id", null);
                    int godina = obj.optInt("godina", 0);

                    Kolegij k = new Kolegij(naziv, godina, studij, profId);
                    k.setId(id);
                    k.setCreatedAt(obj.optString("created_at", null));

                    listaKolegija.add(k);
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        return listaKolegija;
    }

    private String buildStudentLabel(String fullName, String index) {
        StringBuilder sb = new StringBuilder();
        if (fullName != null && !fullName.trim().isEmpty()) {
            sb.append(fullName.trim());
        }
        if (index != null && !index.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(index.trim());
        }
        return sb.toString();
    }

    private boolean handleAuthExpired(int code, String rawError, Runnable onUnauthorized) {
        String lowered = rawError != null ? rawError.toLowerCase(Locale.US) : "";
        boolean expired = code == 401
                || lowered.contains("jwt expired")
                || lowered.contains("token expired")
                || lowered.contains("invalid token")
                || lowered.contains("invalid jwt");
        if (expired) {
            signOut();
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                Toast.makeText(context, "Sesija je istekla. Prijavite se ponovo.", Toast.LENGTH_LONG).show();
                if (onUnauthorized != null) {
                    onUnauthorized.run();
                }
            });
            return true;
        }
        return false;
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
