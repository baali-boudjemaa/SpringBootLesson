package com.example.mef.demo.Services;

import com.example.mef.demo.Services.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private PaymentService paymentService=null;
    public void passOrder(){
        
    }
    @Autowired
    public void ServicePayment(PaymentService paymentService){
        this.paymentService=paymentService;
    }
    public void placeOrder(){
        // Unused dead-code path: PaymentService is now a typed CRUD service
        // (see Services/PaymentService.java) and no longer exposes a
        // no-arg processPayment() stub. Nothing in the app calls OrderService.
    }
    }
