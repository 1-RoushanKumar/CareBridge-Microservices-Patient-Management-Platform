// src/main/java/com/roushan/doctorservice/repository/DoctorRepository.java
package com.roushan.doctorservice.repository;

import com.roushan.doctorservice.model.Doctor;
import com.roushan.doctorservice.model.DoctorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    List<Doctor> findBySpecializationAndStatus(String specialization, DoctorStatus status);
    List<Doctor> findByStatus(DoctorStatus status);
}