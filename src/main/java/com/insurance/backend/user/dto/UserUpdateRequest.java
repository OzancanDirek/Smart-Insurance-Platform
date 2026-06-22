package com.insurance.backend.user.dto;

import com.insurance.backend.user.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest
{
    @NotBlank(message = "Ad bos bırakılamaz")
    private String firstName;

    @NotBlank(message = "Soyad bos birakilamaz")
    private String lastName;

    @NotNull(message = "Rol secilmelidir")
    private Role role;
}