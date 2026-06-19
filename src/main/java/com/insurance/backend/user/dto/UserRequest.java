package com.insurance.backend.user.dto;

import com.insurance.backend.user.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest
{
    @NotBlank(message = "Email boş bırakılamaz")
    @Email(message = "Geçerli bir email adresi girin")
    private String email;

    @NotBlank(message = "Şifre boş bırakılamaz")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalı")
    private String password;

    @NotBlank(message = "Ad boş bırakılamaz")
    @Size(min = 2, max = 50, message = "Ad 2-50 karakter arasında olmalı")
    private String firstName;

    @NotBlank(message = "Soyad boş bırakılamaz")
    @Size(min = 2, max = 50, message = "Soyad 2-50 karakter arasında olmalı")
    private String lastName;

    @NotNull(message = "Rol seçilmelidir")
    private Role role;
}