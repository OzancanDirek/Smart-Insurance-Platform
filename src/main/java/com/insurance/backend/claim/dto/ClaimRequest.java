package com.insurance.backend.claim.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClaimRequest
{
    @NotBlank
    private String title;

    private String description;
}