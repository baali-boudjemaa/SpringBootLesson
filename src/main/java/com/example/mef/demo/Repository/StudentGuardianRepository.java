package com.example.mef.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, String> {

    List<StudentGuardian> findByStudentId(String studentId);

    List<StudentGuardian> findByGuardianId(String guardianId);

}