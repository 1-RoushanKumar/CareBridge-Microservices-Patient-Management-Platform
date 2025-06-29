// src/main/java/com/roushan/doctorservice/repository/DoctorSlotRepository.java
package com.roushan.doctorservice.repository;

import com.roushan.doctorservice.model.DoctorSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, UUID> {

    // Find available slots for a specific doctor within a time range
    List<DoctorSlot> findByDoctorIdAndStartTimeBetweenAndIsBookedFalse(UUID doctorId, LocalDateTime start, LocalDateTime end);

    // Find any available slot for doctors with specific IDs within a time range
    @Query("SELECT ds FROM DoctorSlot ds WHERE ds.doctorId IN :doctorIds AND ds.isBooked = FALSE AND ds.startTime >= :start AND ds.endTime <= :end")
    List<DoctorSlot> findAvailableSlotsForDoctorsInTimeRange(@Param("doctorIds") List<UUID> doctorIds,
                                                             @Param("start") LocalDateTime start,
                                                             @Param("end") LocalDateTime end);

    Optional<DoctorSlot> findByIdAndDoctorId(UUID slotId, UUID doctorId);
}