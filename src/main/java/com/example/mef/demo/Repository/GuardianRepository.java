package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, String> {

    List<Guardian> findByStudentId(String studentId);

    @Query("SELECT DISTINCT g FROM Guardian g LEFT JOIN FETCH g.student " +
           "WHERE LOWER(g.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))")
    List<Guardian> findByLastNameContainingIgnoreCase(@Param("lastName") String lastName);

    /**
     * Same as findAll(), but eagerly fetches the lazy student association
     * so the UI (which reads it on the JavaFX thread, after the transaction
     * has closed) doesn't hit a LazyInitializationException.
     */
    @Query("SELECT DISTINCT g FROM Guardian g LEFT JOIN FETCH g.student")
    List<Guardian> findAllWithDetails();
}