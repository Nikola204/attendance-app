package com.example.studentqrscanner.model;

public class User {
    private String id;
    private String email;
    private UserRole role;
    private String fullName;

    public User() {
    }

    public User(String id, String email, UserRole role, String fullName) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
