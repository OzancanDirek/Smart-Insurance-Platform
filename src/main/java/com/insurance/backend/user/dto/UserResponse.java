package com.insurance.backend.user.dto;

import com.insurance.backend.user.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse
{
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
}