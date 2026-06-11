package com.insurance.backend.document.service;

import com.insurance.backend.claim.enums.ClaimType;
import com.insurance.backend.document.enums.DocumentType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocumentValidationService
{

    private static final Map<ClaimType, List<DocumentType>> REQUIRED_DOCUMENTS = Map.of(
            ClaimType.TRAFFIC_ACCIDENT, List.of(
                    DocumentType.IDENTITY,
                    DocumentType.VEHICLE_LICENSE,
                    DocumentType.ACCIDENT_PHOTO,
                    DocumentType.ACCIDENT_REPORT
            ),
            ClaimType.THEFT, List.of(
                    DocumentType.IDENTITY,
                    DocumentType.VEHICLE_LICENSE,
                    DocumentType.INSURANCE_POLICY
            ),
            ClaimType.NATURAL_DISASTER, List.of(
                    DocumentType.IDENTITY,
                    DocumentType.INSURANCE_POLICY,
                    DocumentType.ACCIDENT_PHOTO
            ),
            ClaimType.OTHER, List.of(
                    DocumentType.IDENTITY
            )
    );

    private static final Map<DocumentType, String> DOCUMENT_NAMES = Map.of(
            DocumentType.IDENTITY, "Kimlik Belgesi",
            DocumentType.VEHICLE_LICENSE, "Araç Ruhsatı",
            DocumentType.ACCIDENT_PHOTO, "Kaza Fotoğrafı",
            DocumentType.ACCIDENT_REPORT, "Kaza Tutanağı",
            DocumentType.INSURANCE_POLICY, "Sigorta Poliçesi",
            DocumentType.EXPERT_REPORT, "Eksper Raporu",
            DocumentType.OTHER, "Diğer Belge"
    );

    public List<String> getMissingDocuments(ClaimType claimType, List<DocumentType> uploadedTypes)
    {
        List<DocumentType> required = REQUIRED_DOCUMENTS.getOrDefault(claimType, List.of());
        Set<DocumentType> uploaded = Set.copyOf(uploadedTypes);

        return required.stream()
                .filter(doc -> !uploaded.contains(doc))
                .map(doc -> DOCUMENT_NAMES.getOrDefault(doc, doc.name()))
                .collect(Collectors.toList());
    }

    public boolean isComplete(ClaimType claimType, List<DocumentType> uploadedTypes)
    {
        return getMissingDocuments(claimType, uploadedTypes).isEmpty();
    }

    public List<DocumentType> getRequiredDocuments(ClaimType claimType)
    {
        return REQUIRED_DOCUMENTS.getOrDefault(claimType, List.of());
    }
}