package com.example.mef.demo.Repository;



import com.example.mef.demo.enums.SessionName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionPricingRepository extends JpaRepository<SessionPricing, String> {

    Optional<SessionPricing> findBySession(SessionName session);

}