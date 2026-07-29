package com.example.mef.demo.Model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, String> {

    List<Attendance> findByStudentId(String studentId);

    List<Attendance> findByDate(LocalDateTime date);

}