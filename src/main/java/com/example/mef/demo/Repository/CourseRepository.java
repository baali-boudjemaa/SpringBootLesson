package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {

    List<Course> findByNameContainingIgnoreCase(String name);

    List<Course> findByClassroomId(String classroomId);

    List<Course> findByTeacherId(String teacherId);
}