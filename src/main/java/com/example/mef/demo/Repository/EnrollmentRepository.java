package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {

    List<Enrollment> findByStudentId(String studentId);

    List<Enrollment> findByAcademicYear(String academicYear);

    @Query("select distinct e.academicYear from Enrollment e where e.academicYear is not null order by e.academicYear desc")
    List<String> findDistinctAcademicYears();

    @Query("select distinct e.classSection from Enrollment e where e.classSection is not null order by e.classSection")
    List<String> findDistinctClassSections();

    boolean existsByStudentIdAndAcademicYear(@Param("studentId") String studentId, @Param("academicYear") String academicYear);
}
