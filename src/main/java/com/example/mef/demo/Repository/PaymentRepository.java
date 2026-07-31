package com.example.mef.demo.Repository;

import com.example.mef.demo.Model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByInscriptionId(String inscriptionId);

}
