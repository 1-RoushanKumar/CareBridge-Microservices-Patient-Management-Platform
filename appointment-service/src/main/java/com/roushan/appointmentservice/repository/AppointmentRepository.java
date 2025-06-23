package com.roushan.appointmentservice.repository;

import com.roushan.appointmentservice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientId(UUID patientId);

    List<Appointment> findByDoctorId(UUID doctorId); // Added this for Doctor role access
    // You might add more specific queries later, e.g.,
    // List<Appointment> findByPatientIdAndStatus(UUID patientId, AppointmentStatus status);
}