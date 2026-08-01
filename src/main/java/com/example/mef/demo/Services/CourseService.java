package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Repository.ClassroomRepository;
import com.example.mef.demo.Repository.CourseRepository;
import com.example.mef.demo.Repository.EmployeeRepository;
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

    public CourseService(CourseRepository courseRepository,
                         EmployeeRepository employeeRepository,
                         ClassroomRepository classroomRepository) {
        this.courseRepository = courseRepository;
        this.employeeRepository = employeeRepository;
        this.classroomRepository = classroomRepository;
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return courseRepository.findAll();
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
}