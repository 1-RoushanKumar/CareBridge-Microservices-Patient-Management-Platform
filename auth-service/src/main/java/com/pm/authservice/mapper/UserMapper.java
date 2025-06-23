package com.pm.authservice.mapper;

import com.pm.authservice.dto.UserResponseDTO;
import com.pm.authservice.model.User;

public class UserMapper {

    public static UserResponseDTO toDTO(User user) {
        // Using Lombok's @Builder for more concise DTO creation
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}