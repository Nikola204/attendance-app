package com.example.studentqrscanner.model;

public class AttendanceRecord {
    private String subject;
    private String dateDay;
    private String dateMonth;
    private String time;
    private String room;
    private boolean isPresent;

    public AttendanceRecord(String subject, String dateDay, String dateMonth, String time, String room, boolean isPresent) {
        this.subject = subject;
        this.dateDay = dateDay;
        this.dateMonth = dateMonth;
        this.time = time;
        this.room = room;
        this.isPresent = isPresent;
    }

    public String getSubject() {
        return subject;
    }

    public String getDateDay() {
        return dateDay;
    }

    public String getDateMonth() {
        return dateMonth;
    }

    public String getTime() {
        return time;
    }

    public String getRoom() {
        return room;
    }

    public boolean isPresent() {
        return isPresent;
    }
}
