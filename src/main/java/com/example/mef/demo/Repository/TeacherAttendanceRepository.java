package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.TeacherAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, String> {
    Optional<TeacherAttendance> findByTeacherIdAndDate(String teacherId, LocalDate date);
    List<TeacherAttendance> findByTeacherIdAndDateBetween(String teacherId, LocalDate start, LocalDate end);
}
