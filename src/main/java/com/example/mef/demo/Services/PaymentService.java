package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Payment;
import com.example.mef.demo.Repository.InscriptionRepository;
import com.example.mef.demo.Repository.PaymentRepository;
import com.example.mef.demo.enums.PaymentStatus;
import com.example.mef.demo.enums.PaymentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Typed service backing the "payments" module. */
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InscriptionRepository inscriptionRepository;

    public PaymentService(PaymentRepository paymentRepository, InscriptionRepository inscriptionRepository) {
        this.paymentRepository = paymentRepository;
        this.inscriptionRepository = inscriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findById(String id) {
        return paymentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Payment> findByInscriptionId(String inscriptionId) {
        return paymentRepository.findByInscriptionId(inscriptionId);
    }

    /** Records a payment against the given enrollment. */
    public Payment save(Payment payment, String inscriptionId) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("No enrollment found with id " + inscriptionId));
        payment.setInscription(inscription);
        if (payment.getDatePay() == null) {
            payment.setDatePay(LocalDateTime.now());
        }
        if (payment.getPaymentMethod() == null) {
            payment.setPaymentMethod(PaymentType.CASH);
        }
        if (payment.getStatus() == null) {
            payment.setStatus(PaymentStatus.PAID);
        }
        return paymentRepository.save(payment);
    }

    public void delete(String id) {
        paymentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public double totalIncomeBetween(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getDatePay() != null
                        && !p.getDatePay().isBefore(start)
                        && !p.getDatePay().isAfter(end))
                .mapToDouble(p -> p.getAmount() == null ? 0.0 : p.getAmount())
                .sum();
    }
}