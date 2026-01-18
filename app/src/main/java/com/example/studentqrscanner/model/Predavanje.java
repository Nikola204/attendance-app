package com.example.studentqrscanner.model;

public class Predavanje {
    private String id;
    private String created_at;
    private String ucionica;
    private String datum;
    private String kolegij_id;

    public Predavanje() {}

    public Predavanje(String ucionica, String datum, String kolegij_id) {
        this.ucionica = ucionica;
        this.datum = datum;
        this.kolegij_id = kolegij_id;
    }

    public void setId(String id) { this.id = id; }

    public String getCreatedAt() { return created_at; }
    public void setCreatedAt(String created_at) { this.created_at = created_at; }

    public String getUcionica() { return ucionica; }
    public void setUcionica(String ucionica) { this.ucionica = ucionica; }

    public String getDatum() { return datum; }
    public void setDatum(String datum) { this.datum = datum; }

    public String getKolegijId() { return kolegij_id; }
    public void setKolegijId(String kolegij_id) { this.kolegij_id = kolegij_id; }
}