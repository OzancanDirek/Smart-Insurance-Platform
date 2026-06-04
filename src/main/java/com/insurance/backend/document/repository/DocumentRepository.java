package com.insurance.backend.document.repository;

import com.insurance.backend.document.entity.Document;
import com.insurance.backend.document.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long>
{
    List<Document> findByClaimId(Long claimId);

    List<Document> findByDocumentType(DocumentType documentType);

    List<Document> findByUploadedById(Long userId);
}