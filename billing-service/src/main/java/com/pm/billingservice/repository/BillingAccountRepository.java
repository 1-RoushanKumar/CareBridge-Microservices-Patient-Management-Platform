package com.pm.billingservice.repository;

import com.pm.billingservice.model.BillingAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillingAccountRepository extends JpaRepository<BillingAccount, UUID> {
    // Find a billing account by the patient's UUID
    Optional<BillingAccount> findByPatientId(UUID patientId);
}
