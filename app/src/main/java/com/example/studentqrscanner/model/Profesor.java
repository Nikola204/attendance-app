package com.example.studentqrscanner.model;

public class Profesor extends BaseUser {
    private String ime;
    private String prezime;

    public Profesor() {
        this.role = UserRole.PROFESOR;
    }

    @Override
    public String getFullName() {
        return ime + " " + prezime;
    }

    public String getIme() { return ime; }
    public void setIme(String ime) { this.ime = ime; }

    public String getPrezime() { return prezime; }
    public void setPrezime(String prezime) { this.prezime = prezime; }
}