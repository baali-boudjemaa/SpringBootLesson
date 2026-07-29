package com.example.mef.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, String> {

    List<Absence> findByStudentId(String studentId);

}