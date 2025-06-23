package com.roushan.appointmentservice.repository;

import com.roushan.appointmentservice.model.PatientDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientDetailsRepository extends JpaRepository<PatientDetails, UUID> {
    Optional<PatientDetails> findByEmail(String email);
}