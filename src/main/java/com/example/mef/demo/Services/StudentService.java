package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
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
        List<Student> byPhone = studentRepository.findByPhoneContainingIgnoreCase(needle);
        java.util.LinkedHashMap<String, Student> merged = new java.util.LinkedHashMap<>();
        byFirst.forEach(s -> merged.put(s.getId(), s));
        byLast.forEach(s -> merged.put(s.getId(), s));
        byPhone.forEach(s -> merged.put(s.getId(), s));
        return List.copyOf(merged.values());
    }
}