package com.example.studentqrscanner.model;

public abstract class BaseUser {
    protected String email;
    protected UserRole role = UserRole.STUDENT;

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public abstract String getFullName();
}
