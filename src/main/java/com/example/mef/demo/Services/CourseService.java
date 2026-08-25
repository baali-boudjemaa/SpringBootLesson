package com.example.mef.demo.Services;

import com.example.mef.demo.Model.*;
import com.example.mef.demo.Repository.ClassroomRepository;
import com.example.mef.demo.Repository.CourseRepository;
import com.example.mef.demo.Repository.EmployeeRepository;
import com.example.mef.demo.Repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final EmployeeRepository employeeRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentRepository studentRepository;
    private final StudentCourseService studentCourseService;

    public CourseService(CourseRepository courseRepository,
                         EmployeeRepository employeeRepository,
                         ClassroomRepository classroomRepository,
                         StudentRepository studentRepository,
                         StudentCourseService studentCourseService) {
        this.courseRepository = courseRepository;
        this.employeeRepository = employeeRepository;
        this.classroomRepository = classroomRepository;
        this.studentRepository = studentRepository;
        this.studentCourseService = studentCourseService;
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return courseRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public Optional<Course> findById(String id) {
        return courseRepository.findById(id);
    }

    /** Saves the course, resolving teacherId/classroomId to real entities (either may be null). */
    public Course save(Course course, String teacherId, String classroomId) {
        Employee teacher = null;
        if (teacherId != null && !teacherId.isBlank()) {
            teacher = employeeRepository.findById(teacherId)
                    .orElseThrow(() -> new IllegalArgumentException("No teacher found with id " + teacherId));
        }
        Classroom classroom = null;
        if (classroomId != null && !classroomId.isBlank()) {
            classroom = classroomRepository.findById(classroomId)
                    .orElseThrow(() -> new IllegalArgumentException("No classroom found with id " + classroomId));
        }
        course.setTeacher(teacher);
        course.setClassroom(classroom);
        return courseRepository.save(course);
    }

    public void delete(String id) {
        courseRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Course> search(String needle) {
        if (needle == null || needle.isBlank()) {
            return findAll();
        }
        return courseRepository.findByNameContainingIgnoreCase(needle);
    }

    @Transactional
    public void addStudentToCourse(String courseId, String studentId, String semester) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        studentCourseService.enrollStudentInCourse(student, course, semester);
    }

    @Transactional
    public void removeStudentFromCourse(String courseId, String studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        studentCourseService.dropStudentFromCourse(student, course);
    }

    @Transactional(readOnly = true)
    public List<StudentCourse> getCourseStudents(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        return studentCourseService.getCourseActiveStudents(course);
    }

    @Transactional(readOnly = true)
    public int getCourseCapacity(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        return course.getStudentCount();
    }
}