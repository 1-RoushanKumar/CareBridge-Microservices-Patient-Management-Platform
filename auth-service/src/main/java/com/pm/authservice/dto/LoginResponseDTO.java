package com.pm.authservice.dto;

import lombok.*;

@Value
@AllArgsConstructor
public class LoginResponseDTO {
    String token;
}
