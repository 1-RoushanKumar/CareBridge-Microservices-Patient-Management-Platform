package com.pm.authservice.mapper;

import com.pm.authservice.dto.UserResponseDTO;
import com.pm.authservice.model.User;

public class UserMapper {

    public static UserResponseDTO toDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        return dto;
    }
}