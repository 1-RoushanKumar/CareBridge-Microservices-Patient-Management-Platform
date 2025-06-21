package com.roushan.appointmentservice.repository;

import com.roushan.appointmentservice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    // Custom method to find all appointments for a specific patient
    List<Appointment> findByPatientId(UUID patientId);

    // You might add more specific queries later, e.g.,
    // List<Appointment> findByDoctorId(UUID doctorId);
    // List<Appointment> findByPatientIdAndStatus(UUID patientId, AppointmentStatus status);
}
