// src/main/java/com/roushan/doctorservice/mapper/DoctorSlotMapper.java
package com.roushan.doctorservice.mapper;

import com.roushan.doctorservice.dtos.DoctorSlotDTO;
import com.roushan.doctorservice.model.DoctorSlot;

public class DoctorSlotMapper {

    public static DoctorSlot toEntity(DoctorSlotDTO dto) {
        DoctorSlot slot = new DoctorSlot();
        // ID is usually generated or set from path, not from DTO for creation
        slot.setDoctorId(dto.getDoctorId());
        slot.setStartTime(dto.getStartTime());
        slot.setEndTime(dto.getEndTime());
        slot.setBooked(dto.isBooked());
        slot.setAppointmentId(dto.getAppointmentId());
        return slot;
    }

    public static DoctorSlotDTO toDTO(DoctorSlot slot) {
        DoctorSlotDTO dto = new DoctorSlotDTO();
        dto.setId(slot.getId());
        dto.setDoctorId(slot.getDoctorId());
        dto.setStartTime(slot.getStartTime());
        dto.setEndTime(slot.getEndTime());
        dto.setBooked(slot.isBooked());
        dto.setAppointmentId(slot.getAppointmentId());
        return dto;
    }
}