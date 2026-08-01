package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {

    @Query("SELECT DISTINCT c FROM Course c " +
           "LEFT JOIN FETCH c.teacher " +
           "LEFT JOIN FETCH c.classroom " +
           "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Course> findByNameContainingIgnoreCase(@Param("name") String name);

    List<Course> findByClassroomId(String classroomId);

    List<Course> findByTeacherId(String teacherId);

    /**
     * Same as findAll(), but eagerly fetches the lazy teacher/classroom
     * associations so the UI (which reads them on the JavaFX thread, after
     * the transaction has closed) doesn't hit a LazyInitializationException.
     */
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.teacher LEFT JOIN FETCH c.classroom")
    List<Course> findAllWithDetails();
}