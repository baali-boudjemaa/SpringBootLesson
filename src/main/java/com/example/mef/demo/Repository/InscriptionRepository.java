package com.example.mef.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscriptionRepository extends JpaRepository<Inscription, String> {

    List<Inscription> findByStudentId(String studentId);

    List<Inscription> findByClassId(String classId);

    List<Inscription> findByAnneeScolaireId(String anneeScolaireId);

}