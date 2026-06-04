package com.insurance.backend.document.dto;

import com.insurance.backend.document.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DocumentResponse
{
    private Long id;
    private String fileName;
    private String fileType;
    private DocumentType documentType;
    private String ocrText;
    private Integer riskScore;
    private Long claimId;
    private Long uploadedById;
    private String uploadedByFullName;
    private LocalDateTime createdAt;
}