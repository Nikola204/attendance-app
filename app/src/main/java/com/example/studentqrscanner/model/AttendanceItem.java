package com.example.studentqrscanner.model;

public class AttendanceItem {
    private String evidencijaId;
    private String studentFirstName;
    private String studentLastName;
    private String studentIndex;
    private String timestamp;

    public AttendanceItem(String evidencijaId, String studentFirstName, String studentLastName, String studentIndex, String timestamp) {
        this.evidencijaId = evidencijaId;
        this.studentFirstName = studentFirstName;
        this.studentLastName = studentLastName;
        this.studentIndex = studentIndex;
        this.timestamp = timestamp;
    }

    public String getEvidencijaId() {
        return evidencijaId;
    }

    public String getStudentName() {
        String first = studentFirstName != null ? studentFirstName : "";
        String last = studentLastName != null ? studentLastName : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? first + last : combined;
    }

    public String getStudentFirstName() {
        return studentFirstName;
    }

    public String getStudentLastName() {
        return studentLastName;
    }

    public String getStudentIndex() {
        return studentIndex;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
}
