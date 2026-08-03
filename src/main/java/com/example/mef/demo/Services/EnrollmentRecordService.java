package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Enrollment;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Repository.EnrollmentRepository;
import com.example.mef.demo.Repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Backs the {@link Enrollment} entity (academic year / class section /
 * registration fee) used by the {@code EnrollmentWizard} 3-step flow —
 * distinct from {@link EnrollmentService}, which actually backs the
 * "enrollments" module table (an {@code Inscription}).
 */
@Service
@Transactional
public class EnrollmentRecordService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    public EnrollmentRecordService(EnrollmentRepository enrollmentRepository, StudentRepository studentRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<Enrollment> findByStudentId(String studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    /** Academic year suggestions: the current + next school year, plus any already used. */
    @Transactional(readOnly = true)
    public List<String> academicYearOptions() {
        Set<String> years = new LinkedHashSet<>();
        years.add(currentSchoolYearLabel());
        years.add(nextSchoolYearLabel());
        years.addAll(enrollmentRepository.findDistinctAcademicYears());
        return List.copyOf(years);
    }

    @Transactional(readOnly = true)
    public List<String> classSectionOptions() {
        return enrollmentRepository.findDistinctClassSections();
    }

    /** Saves the enrollment for the given student, whether new or existing. */
    public Enrollment save(Enrollment enrollment, String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("No student found with id " + studentId));
        enrollment.setStudent(student);
        if (enrollment.getEnrollmentDate() == null) {
            enrollment.setEnrollmentDate(LocalDateTime.now());
        }
        if (enrollment.getStatus() == null || enrollment.getStatus().isBlank()) {
            enrollment.setStatus("ACTIVE");
        }
        return enrollmentRepository.save(enrollment);
    }

    public static String currentSchoolYearLabel() {
        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        return startYear + "-" + (startYear + 1);
    }

    public static String nextSchoolYearLabel() {
        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        return (startYear + 1) + "-" + (startYear + 2);
    }
}
