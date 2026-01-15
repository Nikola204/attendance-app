package com.example.studentqrscanner.model;

public class Kolegij {
    private String id;
    private String created_at;
    private String naziv;
    private int godina;
    private String studij;
    private String profesor_id;

    public Kolegij() {}

    public Kolegij(String naziv, int godina, String studij, String profesor_id) {
        this.naziv = naziv;
        this.godina = godina;
        this.studij = studij;
        this.profesor_id = profesor_id;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCreatedAt() { return created_at; }
    public void setCreatedAt(String created_at) { this.created_at = created_at; }

    public String getNaziv() { return naziv; }
    public void setNaziv(String naziv) { this.naziv = naziv; }

    public int getGodina() { return godina; }
    public void setGodina(int godina) { this.godina = godina; }

    public String getStudij() { return studij; }
    public void setStudij(String studij) { this.studij = studij; }

    public String getProfesorId() { return profesor_id; }
    public void setProfesorId(String profesor_id) { this.profesor_id = profesor_id; }
}