package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Absence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    List<Absence> findByStudentId(Long studentId);

}
