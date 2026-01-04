package com.example.studentqrscanner.model;

public class Student extends BaseUser {
    private String ime;
    private String prezime;
    private String brojIndexa;
    private String studij;
    private int godina;

    @Override
    public String getFullName() {
        return ime + " " + prezime;
    }

    public String getIme() { return ime; }
    public String getPrezime() { return prezime; }
    public String getBrojIndexa() { return brojIndexa; }
    public String getStudij() { return studij; }
    public int getGodina() { return godina; }

    public void setIme(String ime) { this.ime = ime; }
    public void setPrezime(String prezime) { this.prezime = prezime; }
    public void setBrojIndexa(String brojIndexa) { this.brojIndexa = brojIndexa; }
    public void setStudij(String studij) { this.studij = studij; }
    public void setGodina(int godina) { this.godina = godina; }
}
