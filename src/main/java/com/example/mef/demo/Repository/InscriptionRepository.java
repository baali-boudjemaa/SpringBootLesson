package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.enums.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface InscriptionRepository extends JpaRepository<Inscription, String> {
    List<Inscription> findByStudentId(String studentId);
    List<Inscription> findByClassroomId(String classId);
    List<Inscription> findByClassroomCategory(Category category);
    List<Inscription> findByAnneeScolaireId(String anneeScolaireId);

    /**
     * Same as findAll(), but eagerly fetches the lazy student/classroom/
     * anneeScolaire/courses associations so the UI (which reads them on the JavaFX
     * thread, after the transaction has closed) doesn't hit a
     * LazyInitializationException.
     */
    @Query("SELECT DISTINCT i FROM Inscription i " +
            "JOIN FETCH i.student " +
            "JOIN FETCH i.classroom " +
            "LEFT JOIN FETCH i.anneeScolaire " +
            "LEFT JOIN FETCH i.courses")
    List<Inscription> findAllWithDetails();

    /**
     * Inscriptions for a set of students, with the (optional) classroom eagerly
     * fetched. Used to resolve each student's current class for display/filtering
     * without hitting a LazyInitializationException or N+1 queries.
     * LEFT JOIN because classroom is optional on Inscription.
     */
    @Query("SELECT DISTINCT i FROM Inscription i "
            + "JOIN FETCH i.student "
            + "LEFT JOIN FETCH i.classroom "
            + "WHERE i.student.id IN :studentIds")
    List<Inscription> findByStudentIdInWithClassroom(@Param("studentIds") List<String> studentIds);
}
