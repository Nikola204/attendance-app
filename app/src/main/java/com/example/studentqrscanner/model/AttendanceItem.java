package com.example.studentqrscanner.model;

public class AttendanceItem {
    private String evidencijaId;
    private String studentName;
    private String studentIndex;
    private String timestamp;

    public AttendanceItem(String evidencijaId, String studentName, String studentIndex, String timestamp) {
        this.evidencijaId = evidencijaId;
        this.studentName = studentName;
        this.studentIndex = studentIndex;
        this.timestamp = timestamp;
    }

    public String getEvidencijaId() {
        return evidencijaId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentIndex() {
        return studentIndex;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
}
