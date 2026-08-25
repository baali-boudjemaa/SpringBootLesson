package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Model.StudentCourse;
import com.example.mef.demo.Repository.StudentCourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StudentCourseService {

    @Autowired
    private StudentCourseRepository repository;

    /**
     * Enroll student in a course
     */
    public StudentCourse enrollStudentInCourse(Student student, Course course, String semester) {
        // Check if already enrolled
        Optional<StudentCourse> existing = repository.findByStudentAndCourse(student, course);
        if (existing.isPresent()) {
            StudentCourse sc = existing.get();
            if (!sc.isActive()) {
                // Re-activate if was dropped
                sc.setActive(true);
                sc.setEnrollmentStatus("ACTIVE");
                return repository.save(sc);
            }
            return sc; // Already active
        }

        // Check course capacity
        if (!course.hasCapacity()) {
            throw new RuntimeException("Course is full: " + course.getName());
        }

        StudentCourse enrollment = StudentCourse.builder()
                .student(student)
                .course(course)
                .semester(semester)
                .enrollmentStatus("ACTIVE")
                .active(true)
                .build();

        return repository.save(enrollment);
    }

    /**
     * Drop student from course
     */
    public void dropStudentFromCourse(Student student, Course course) {
        Optional<StudentCourse> enrollment = repository.findByStudentAndCourse(student, course);
        enrollment.ifPresent(sc -> {
            sc.setActive(false);
            sc.setEnrollmentStatus("DROPPED");
            repository.save(sc);
        });
    }

    /**
     * Get all courses for a student
     */
    public List<StudentCourse> getStudentCourses(Student student) {
        return repository.findByStudent(student);
    }

    /**
     * Get all active courses for a student
     */
    public List<StudentCourse> getStudentActiveCourses(Student student) {
        return repository.findByStudentAndActiveTrue(student);
    }

    /**
     * Get all students in a course
     */
    public List<StudentCourse> getCourseStudents(Course course) {
        return repository.findByCourse(course);
    }

    /**
     * Get all active students in a course
     */
    public List<StudentCourse> getCourseActiveStudents(Course course) {
        return repository.findByCourseAndActiveTrue(course);
    }

    /**
     * Check if student is enrolled in course
     */
    public boolean isEnrolledInCourse(Student student, Course course) {
        return repository.isEnrolled(student, course);
    }

    /**
     * Update enrollment status
     */
    public StudentCourse updateEnrollmentStatus(String enrollmentId, String status) {
        StudentCourse enrollment = repository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setEnrollmentStatus(status);
        return repository.save(enrollment);
    }
}