package com.example.mef.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.mef.demo.Model.Classroom;

import java.util.List;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    List<Classroom> findByNameContainingIgnoreCase(String name);

}
