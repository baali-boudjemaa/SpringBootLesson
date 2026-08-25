package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Repository.CourseRepository;
import com.example.mef.demo.Repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentCourseService studentCourseService;
    private final CourseRepository courseRepository;
    public StudentService(StudentRepository studentRepository, StudentCourseService studentCourseService, CourseRepository courseRepository) {
        this.studentRepository = studentRepository;
        this.studentCourseService = studentCourseService;
        this.courseRepository = courseRepository;
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Optional<Student> findById(String id) {
        return studentRepository.findById(id);
    }

    public Student save(Student student) {
        return studentRepository.save(student);
    }

    public void delete(String id) {
        studentRepository.deleteById(id);
    }

    public List<Student> search(String needle) {
        if (needle == null || needle.isBlank()) {
            return findAll();
        }
        List<Student> byFirst = studentRepository.findByFirstNameContainingIgnoreCase(needle);
        List<Student> byLast = studentRepository.findByLastNameContainingIgnoreCase(needle);
        java.util.LinkedHashMap<String, Student> merged = new java.util.LinkedHashMap<>();
        byFirst.forEach(s -> merged.put(s.getId(), s));
        byLast.forEach(s -> merged.put(s.getId(), s));
        return List.copyOf(merged.values());
    }
    @Transactional
    public List<Course> getStudentCourses(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return student.getActiveCourses();
    }

    @Transactional
    public void enrollStudentInCourse(String studentId, String courseId, String semester) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        studentCourseService.enrollStudentInCourse(student, course, semester);
    }

    @Transactional
    public void dropStudentFromCourse(String studentId, String courseId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        studentCourseService.dropStudentFromCourse(student, course);
    }
}