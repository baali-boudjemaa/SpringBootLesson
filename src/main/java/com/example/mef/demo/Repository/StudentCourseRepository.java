package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Model.StudentCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, String> {

    /**
     * Find enrollment of specific student in specific course
     */
    Optional<StudentCourse> findByStudentAndCourse(Student student, Course course);

    /**
     * Find all courses for a student
     */
    List<StudentCourse> findByStudent(Student student);

    /**
     * Find all active courses for a student
     */
    List<StudentCourse> findByStudentAndActiveTrue(Student student);

    /**
     * Find all students in a course
     */
    List<StudentCourse> findByCourse(Course course);

    /**
     * Find all active students in a course
     */
    List<StudentCourse> findByCourseAndActiveTrue(Course course);

    /**
     * Check if student is enrolled in course
     */
    @Query("SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END " +
            "FROM StudentCourse sc WHERE sc.student = :student AND sc.course = :course AND sc.active = true")
    boolean isEnrolled(@Param("student") Student student, @Param("course") Course course);

    /**
     * Find all enrollments for a semester
     */
    List<StudentCourse> findByStudentAndSemester(Student student, String semester);

    /**
     * Count active enrollments in a course
     */
    @Query("SELECT COUNT(sc) FROM StudentCourse sc WHERE sc.course = :course AND sc.active = true")
    long countActiveByCourse(@Param("course") Course course);
}