package com.roushan.appointmentservice.repository;

import com.roushan.appointmentservice.model.PatientDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientDetailsRepository extends JpaRepository<PatientDetails, UUID> {
    // You might add custom finder methods here if needed, e.g.,
    Optional<PatientDetails> findByEmail(String email);
}