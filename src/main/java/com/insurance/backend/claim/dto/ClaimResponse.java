package com.insurance.backend.claim.dto;

import com.insurance.backend.claim.enums.ClaimStatus;
import com.insurance.backend.claim.enums.ClaimType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ClaimResponse
{
    private Long id;
    private String title;
    private String description;
    private ClaimStatus status;
    private Long customerId;
    private String customerFullName;
    private Long assignedToId;
    private String assignedToFullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ClaimType claimType;

}