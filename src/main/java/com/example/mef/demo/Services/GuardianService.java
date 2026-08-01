package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Guardian;
import com.example.mef.demo.Repository.GuardianRepository;
import com.example.mef.demo.Repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final StudentRepository studentRepository;

    public GuardianService(GuardianRepository guardianRepository, StudentRepository studentRepository) {
        this.guardianRepository = guardianRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<Guardian> findAll() {
        return guardianRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Guardian> findById(String id) {
        return guardianRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Guardian> findByStudentId(String studentId) {
        return guardianRepository.findByStudentId(studentId);
    }

    /** Saves the guardian, linking it to the student with the given id (may be null to unlink). */
    public Guardian save(Guardian guardian, String studentId) {
        if (studentId == null || studentId.isBlank()) {
            guardian.setStudent(null);
        } else {
            guardian.setStudent(studentRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("No student found with id " + studentId)));
        }
        return guardianRepository.save(guardian);
    }

    public void delete(String id) {
        guardianRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Guardian> search(String needle) {
        if (needle == null || needle.isBlank()) {
            return findAll();
        }
        return guardianRepository.findByLastNameContainingIgnoreCase(needle);
    }
}