package com.example.studentqrscanner.model;

public class StudentAttendance {
    private final String studentId;
    private final String ime;
    private final String prezime;
    private final String brojIndexa;
    private final int attendanceCount;

    public StudentAttendance(String studentId, String ime, String prezime, String brojIndexa, int attendanceCount) {
        this.studentId = studentId;
        this.ime = ime;
        this.prezime = prezime;
        this.brojIndexa = brojIndexa;
        this.attendanceCount = attendanceCount;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getIme() {
        return ime;
    }

    public String getPrezime() {
        return prezime;
    }

    public String getBrojIndexa() {
        return brojIndexa;
    }

    public int getAttendanceCount() {
        return attendanceCount;
    }

    public String getFullNameWithIndex() {
        StringBuilder sb = new StringBuilder();
        if (ime != null) sb.append(ime);
        if (prezime != null && !prezime.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(prezime);
        }
        if (brojIndexa != null && !brojIndexa.isEmpty()) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(brojIndexa);
        }
        return sb.toString();
    }
}
