package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, String> {

    List<Guardian> findByStudentId(String studentId);

    List<Guardian> findByLastNameContainingIgnoreCase(String lastName);
}