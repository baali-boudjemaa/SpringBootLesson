package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, String> {

    Optional<Student> findByStudentNumber(String studentNumber);

    List<Student> findByFirstNameContainingIgnoreCase(String firstName);

    List<Student> findByLastNameContainingIgnoreCase(String lastName);

    boolean existsByStudentNumber(String studentNumber);

}