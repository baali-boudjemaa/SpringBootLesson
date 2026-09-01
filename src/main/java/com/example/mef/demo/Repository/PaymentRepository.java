package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByInscriptionId(String inscriptionId);

    /**
     * Bulk-load payments for a set of inscription IDs in a single query.
     * Used by MonthlyBillingService to avoid an N+1 select when computing dues.
     */
    List<Payment> findByInscriptionIdIn(List<String> inscriptionIds);

    /**
     * Same as findAll(), but eagerly fetches the lazy inscription ->
     * student/classroom chain so the UI (which reads them on the JavaFX
     * thread, after the transaction has closed) doesn't hit a
     * LazyInitializationException.
     */
    @Query("SELECT DISTINCT p FROM Payment p " +
           "JOIN FETCH p.inscription i " +
           "JOIN FETCH i.student " +
           "JOIN FETCH i.classroom")
    List<Payment> findAllWithDetails();

}
