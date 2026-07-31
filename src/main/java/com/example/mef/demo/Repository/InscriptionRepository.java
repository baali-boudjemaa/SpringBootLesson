package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface InscriptionRepository extends JpaRepository<Inscription, String> {
    List<Inscription> findByStudentId(String studentId);
    List<Inscription> findByClassroomId(String classId);
    List<Inscription> findByAnneeScolaireId(String anneeScolaireId);
}
