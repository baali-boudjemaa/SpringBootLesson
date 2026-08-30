package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.StudentCourseSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentCourseSubscriptionRepository extends JpaRepository<StudentCourseSubscription, String> {
    @Query("SELECT s FROM StudentCourseSubscription s JOIN FETCH s.course WHERE s.inscription.id = :inscriptionId")
    List<StudentCourseSubscription> findByInscriptionIdWithCourse(String inscriptionId);
}
