package com.example.mef.demo.Services;


import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Repository.ClassroomRepository;
import com.example.mef.demo.Repository.InscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassroomService {

    private final ClassroomRepository classroomRepository;
    private final InscriptionRepository inscriptionRepository;

    public ClassroomService(ClassroomRepository classroomRepository,
                            InscriptionRepository inscriptionRepository) {
        this.classroomRepository = classroomRepository;
        this.inscriptionRepository = inscriptionRepository;
    }

    public List<Classroom> findAll() {
        return classroomRepository.findAll();
    }

    public Classroom save(Classroom classroom) {
        return classroomRepository.save(classroom);
    }

    public void delete(String classroomId) {
        classroomRepository.deleteById(classroomId);
    }

    /** Students currently enrolled in this classroom, via their active Inscription records. */
    public List<Student> getStudentsInClassroom(String classroomId) {
        List<Inscription> inscriptions = inscriptionRepository.findByClassroomId(classroomId);
        return inscriptions.stream()
                .map(Inscription::getStudent)
                .collect(Collectors.toList());
    }

    /** Enrollment count for a classroom, for the card's "X/capacity" display. */
    public int countStudentsInClassroom(String classroomId) {
        return inscriptionRepository.findByClassroomId(classroomId).size();
    }
}