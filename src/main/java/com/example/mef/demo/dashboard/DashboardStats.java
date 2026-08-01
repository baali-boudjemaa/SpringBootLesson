package com.example.mef.demo.dashboard;

public class DashboardStats {

    private long totalStudents;
    private long totalTeachers;
    private long totalClassrooms;
    private long totalPayments;
    private double attendanceRate;

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalTeachers() {
        return totalTeachers;
    }

    public void setTotalTeachers(long totalTeachers) {
        this.totalTeachers = totalTeachers;
    }

    public long getTotalClassrooms() {
        return totalClassrooms;
    }

    public void setTotalClassrooms(long totalClassrooms) {
        this.totalClassrooms = totalClassrooms;
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }
}
