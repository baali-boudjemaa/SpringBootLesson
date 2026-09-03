package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Repository.ClassroomRepository;
import com.example.mef.demo.Repository.CourseRepository;
import com.example.mef.demo.Repository.EmployeeRepository;
import com.example.mef.demo.dashboard.courses.ScheduleValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return courseRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public Optional<Course> findById(String id) {
        return courseRepository.findById(id);
    }

    /** Saves the course, resolving teacherId/classroomId to real entities (either may be null). */
    public Course save(Course course, String teacherId, String classroomId) {
        String previousClassroomId = course.getId() == null ? null : courseRepository.findById(course.getId())
                .map(Course::getClassroom).map(Classroom::getId).orElse(null);
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
        Course saved = courseRepository.save(course);
        syncClassroomOccupancy(previousClassroomId);
        syncClassroomOccupancy(classroomId);
        return saved;
    }

    public void delete(String id) {
        String classroomId = courseRepository.findById(id)
                .map(Course::getClassroom).map(Classroom::getId).orElse(null);
        courseRepository.deleteById(id);
        syncClassroomOccupancy(classroomId);
    }

    /** Makes the section's room-occupancy timetable follow the actual schedules of its courses. */
    private void syncClassroomOccupancy(String classroomId) {
        if (classroomId == null || classroomId.isBlank()) return;
        classroomRepository.findById(classroomId).ifPresent(classroom -> {
            List<Course> courses = courseRepository.findByClassroomId(classroomId);
            String schedule = courses.stream()
                    .flatMap(this::scheduledSlots)
                    .distinct()
                    .collect(Collectors.joining("; "));
            classroom.setOccupancySchedule(schedule);
            classroomRepository.save(classroom);
        });
    }

    private Stream<String> scheduledSlots(Course course) {
        if (course.getScheduleSlots() != null && !course.getScheduleSlots().isEmpty()) {
            return course.getScheduleSlots().stream()
                    .map(slot -> slot.getDayOfWeek() + " " + slot.getStartTime() + "-" + slot.getEndTime());
        }
        return ScheduleValidator.parse(course.getSchedule()).stream().map(slot -> slot.day() + " "
                + String.format("%02d:%02d", slot.startMinutes() / 60, slot.startMinutes() % 60) + "-"
                + String.format("%02d:%02d", slot.endMinutes() / 60, slot.endMinutes() % 60));
    }

    @Transactional(readOnly = true)
    public List<Course> search(String needle) {
        if (needle == null || needle.isBlank()) {
            return findAll();
        }
        return courseRepository.findByNameContainingIgnoreCase(needle);
    }
}
