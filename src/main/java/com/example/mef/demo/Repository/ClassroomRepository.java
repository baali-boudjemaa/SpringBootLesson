package com.example.mef.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomRepository extends JpaRepository<Class, String> {

    List<Class> findByNameContainingIgnoreCase(String name);

}