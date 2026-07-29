package com.example.mef.demo.Repository;

import com.example.mef.demo.enums.LogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyLogRepository extends JpaRepository<DailyLog, String> {

    List<DailyLog> findByStudentId(String studentId);

    List<DailyLog> findByEmployeeId(String employeeId);

    List<DailyLog> findByType(LogType type);

}