package com.example.mef.demo.Services;

import com.example.mef.demo.Model.AnneeScolaire;
import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Repository.AnneeScolaireRepository;
import com.example.mef.demo.Repository.ClassroomRepository;
import com.example.mef.demo.Repository.CourseRepository;
import com.example.mef.demo.Repository.InscriptionRepository;
import com.example.mef.demo.Repository.StudentRepository;
import com.example.mef.demo.enums.EnrollmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Typed service backing the "enrollments" module (Inscription entity). */
@Service
@Transactional
public class EnrollmentService {

    private final InscriptionRepository inscriptionRepository;
    private final StudentRepository studentRepository;
    private final ClassroomRepository classroomRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final CourseRepository courseRepository;

    public EnrollmentService(InscriptionRepository inscriptionRepository,
                             StudentRepository studentRepository,
                             ClassroomRepository classroomRepository,
                             AnneeScolaireRepository anneeScolaireRepository,
                             CourseRepository courseRepository) {
        this.inscriptionRepository = inscriptionRepository;
        this.studentRepository = studentRepository;
        this.classroomRepository = classroomRepository;
        this.anneeScolaireRepository = anneeScolaireRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<Inscription> findAll() {
        return inscriptionRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public Optional<Inscription> findById(String id) {
        return inscriptionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Inscription> findByStudentId(String studentId) {
        return inscriptionRepository.findByStudentId(studentId);
    }

    /** Creates or updates an enrollment for the given student/classroom, in the current school year. */
    public Inscription save(Inscription inscription, String studentId, String classroomId) {
        return save(inscription, studentId, classroomId, null);
    }

    /**
     * Creates or updates an enrollment for the given student/classroom/academic year.
     * When {@code anneeScolaireId} is null or blank, falls back to the current school year
     * (creating it if it doesn't exist yet).
     */
    public Inscription save(Inscription inscription, String studentId, String classroomId, String anneeScolaireId) {
        return save(inscription, studentId, classroomId, anneeScolaireId, null);
    }

    /**
     * Same as {@link #save(Inscription, String, String, String)}, additionally attaching the
     * given support-course selections. For SOUTIEN enrollments the student follows individual
     * courses instead of a fixed classroom, so {@code classroomId} may be null/blank in that
     * case — each selected course already carries its own classroom/teacher/schedule.
     */
    public Inscription save(Inscription inscription, String studentId, String classroomId, String anneeScolaireId,
                            List<String> courseIds) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("No student found with id " + studentId));

        boolean hasCourses = courseIds != null && !courseIds.isEmpty();
        Classroom classroom = null;
        if (classroomId != null && !classroomId.isBlank()) {
            classroom = classroomRepository.findById(classroomId)
                    .orElseThrow(() -> new IllegalArgumentException("No classroom found with id " + classroomId));
        } else if (!hasCourses) {
            // No classroom AND no courses selected: nothing anchors this enrollment.
            throw new IllegalArgumentException("A classroom or at least one course is required.");
        }

        AnneeScolaire schoolYear = (anneeScolaireId == null || anneeScolaireId.isBlank())
                ? findOrCreateCurrentSchoolYear()
                : anneeScolaireRepository.findById(anneeScolaireId)
                .orElseThrow(() -> new IllegalArgumentException("No school year found with id " + anneeScolaireId));

        inscription.setStudent(student);
        inscription.setClassroom(classroom);
        inscription.setAnneeScolaire(schoolYear);
        if (inscription.getDateInscription() == null) {
            inscription.setDateInscription(LocalDateTime.now());
        }
        if (inscription.getStatus() == null) {
            inscription.setStatus(EnrollmentStatus.ACTIVE);
        }
        inscription.setCourses((courseIds == null || courseIds.isEmpty())
                ? new ArrayList<>()
                : courseRepository.findAllById(courseIds));
        return inscriptionRepository.save(inscription);
    }

    public void delete(String id) {
        inscriptionRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AnneeScolaire> findAllSchoolYears() {
        return anneeScolaireRepository.findAll();
    }

    /** Creates a new academic year (e.g. "2027-2028"), or returns the existing one if the label is already used. */
    public AnneeScolaire createSchoolYear(String label) {
        return anneeScolaireRepository.findByLibelleAnneesc(label)
                .orElseGet(() -> {
                    AnneeScolaire schoolYear = new AnneeScolaire();
                    schoolYear.setLibelleAnneesc(label);
                    return anneeScolaireRepository.save(schoolYear);
                });
    }

    public AnneeScolaire findOrCreateCurrentSchoolYear() {
        String label = currentSchoolYearLabel();
        return anneeScolaireRepository.findByLibelleAnneesc(label)
                .orElseGet(() -> {
                    AnneeScolaire schoolYear = new AnneeScolaire();
                    schoolYear.setLibelleAnneesc(label);
                    return anneeScolaireRepository.save(schoolYear);
                });
    }

    private static String currentSchoolYearLabel() {
        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        return startYear + "-" + (startYear + 1);
    }
}