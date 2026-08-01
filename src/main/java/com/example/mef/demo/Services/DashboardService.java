package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Payment;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.dashboard.DashboardStats;

import java.util.List;

public interface DashboardService {

    DashboardStats getDashboardStats();

    List<Student> getRecentStudents();

    List<Payment> getRecentPayments();

}