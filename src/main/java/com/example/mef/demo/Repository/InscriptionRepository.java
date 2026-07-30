package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

    List<Inscription> findByStudentId(Long studentId);

    List<Inscription> findByClassroomId(Long classId);

    List<Inscription> findByAnneeScolaireId(Long anneeScolaireId);

}
