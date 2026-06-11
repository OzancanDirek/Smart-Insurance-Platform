package com.insurance.backend.claim.service;

import com.insurance.backend.claim.dto.ClaimRequest;
import com.insurance.backend.claim.dto.ClaimResponse;
import com.insurance.backend.claim.entity.Claim;
import com.insurance.backend.claim.enums.ClaimStatus;
import com.insurance.backend.claim.enums.ClaimType;
import com.insurance.backend.claim.repository.ClaimRepository;
import com.insurance.backend.document.enums.DocumentType;
import com.insurance.backend.document.repository.DocumentRepository;
import com.insurance.backend.document.service.DocumentValidationService;
import com.insurance.backend.user.entity.User;
import com.insurance.backend.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class ClaimServiceImpl implements IClaimService
{
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final DocumentValidationService documentValidationService;
    private final DocumentRepository documentRepository;

    @Override
    public ClaimResponse createClaim(ClaimRequest request, String email)
    {
        User customer = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        Claim claim = Claim.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .claimType(request.getClaimType() != null ? request.getClaimType() : ClaimType.OTHER)
                .customer(customer)
                .build();
        return toResponse(claimRepository.save(claim));
    }

    @Override
    public ClaimResponse getClaimById(Long id)
    {
        Claim claim = claimRepository.findById(id).orElseThrow(() -> new RuntimeException("Hasar kaydı bulunamadi: " + id));
        return toResponse(claim);
    }

    @Override
    public List<ClaimResponse> getAllClaims()
    {
        return claimRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimResponse> getClaimsByCustomer(String email)
    {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Kullanici bulunamadi"));
        return claimRepository.findByCustomerId(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClaimResponse> getClaimsByStatus(ClaimStatus status)
    {
        return claimRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClaimResponse updateStatus(Long id, ClaimStatus status)
    {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hasar kaydı bulunamadı: " + id));

        // DRAFTTAN PENDING geçişinde belge kontrolü yap
        if (status == ClaimStatus.PENDING && claim.getStatus() == ClaimStatus.DRAFT)
        {
            ClaimType claimType = claim.getClaimType() != null ? claim.getClaimType() : ClaimType.OTHER;

            List<DocumentType> uploadedTypes = documentRepository.findByClaimId(id)
                    .stream()
                    .map(doc -> doc.getDocumentType())
                    .collect(Collectors.toList());

            List<String> missingDocs = documentValidationService.getMissingDocuments(claimType, uploadedTypes);

            if (!missingDocs.isEmpty())
            {
                throw new RuntimeException("Eksik belgeler: " + String.join(", ", missingDocs));
            }
        }


        claim.setStatus(status);
        return toResponse(claimRepository.save(claim));
    }

    @Override
    public ClaimResponse assignClaim(Long claimId, Long userId)
    {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Hasar kaydı bulunamadı: " + claimId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userId));

        claim.setAssignedTo(user);
        return toResponse(claimRepository.save(claim));
    }

    private ClaimResponse toResponse(Claim claim)
    {
        return ClaimResponse.builder()
                .id(claim.getId())
                .title(claim.getTitle())
                .description(claim.getDescription())
                .status(claim.getStatus())
                .customerId(claim.getCustomer().getId())
                .customerFullName(claim.getCustomer().getFirstName() + " " + claim.getCustomer().getLastName())
                .assignedToId(claim.getAssignedTo() != null ? claim.getAssignedTo().getId() : null)
                .assignedToFullName(claim.getAssignedTo() != null ?
                        claim.getAssignedTo().getFirstName() + " " + claim.getAssignedTo().getLastName() : null)
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .claimType(claim.getClaimType())
                .build();
    }
}
