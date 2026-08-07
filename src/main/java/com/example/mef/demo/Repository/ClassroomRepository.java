package com.example.mef.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.example.mef.demo.Model.Classroom;

import java.util.List;

public interface ClassroomRepository extends JpaRepository<Classroom, String> {
    List<Classroom> findByNameContainingIgnoreCase(String name);

    @Query("SELECT DISTINCT c FROM Classroom c LEFT JOIN FETCH c.rooms")
    List<Classroom> findAllWithRooms();
}
