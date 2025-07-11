// src/main/java/com/pm/billingservice/repository/BillRepository.java
package com.pm.billingservice.repository;

import com.pm.billingservice.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {
    Optional<Bill> findByAppointmentId(UUID appointmentId);
    // Add other query methods as needed, e.g., findByBillingAccountId, findByPatientId
}