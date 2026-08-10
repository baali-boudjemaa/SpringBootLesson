package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Outcoming;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutcomingRepository extends JpaRepository<Outcoming, String> {

    /** Real expense rows only (excludes recurring templates), most recent first. */
    List<Outcoming> findAllByRecurringFalseOrderByDateOutcomeDesc();

    /** Recurring templates only. */
    List<Outcoming> findAllByRecurringTrueOrderByLabelAsc();
}