package com.insurance.backend.claim.dto;

import com.insurance.backend.claim.enums.ClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClaimRequest
{
    @NotBlank(message = "Başlık boş bırakılamaz")
    @Size(min = 3, max = 200, message = "Başlık 3-200 karakter arasında olmalı")
    private String title;

    @Size(max = 1000, message = "Açıklama 1000 karakteri geçemez")
    private String description;

    private ClaimType claimType;
}