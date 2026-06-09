package com.insurance.backend.document.service;

import com.insurance.backend.document.dto.DocumentResponse;
import com.insurance.backend.document.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IDocumentService
{
    DocumentResponse uploadDocument(MultipartFile file, Long claimId, String email);

    List<DocumentResponse> getDocumentsByClaimId(Long claimId);

    DocumentResponse getDocumentById(Long id);

    List<DocumentResponse> getDocumentsByType(DocumentType documentType);

    List<DocumentResponse> searchByText(String text);
}