package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, String> {
    List<Room> findByNameContainingIgnoreCase(String name);
}