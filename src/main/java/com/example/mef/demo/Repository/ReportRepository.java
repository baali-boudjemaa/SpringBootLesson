package com.example.mef.demo.Repository;


import com.example.mef.demo.Model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, String> {

    List<Report> findAllByOrderByCreatedAtDesc();
}