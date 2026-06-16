package com.insurance.backend.claim.service;

import com.insurance.backend.audit.service.AuditLogService;
import com.insurance.backend.claim.dto.ClaimRequest;
import com.insurance.backend.claim.dto.ClaimResponse;
import com.insurance.backend.claim.entity.Claim;
import com.insurance.backend.claim.enums.ClaimStatus;
import com.insurance.backend.claim.enums.ClaimType;
import com.insurance.backend.claim.repository.ClaimRepository;
import com.insurance.backend.document.enums.DocumentType;
import com.insurance.backend.document.repository.DocumentRepository;
import com.insurance.backend.document.service.DocumentValidationService;
import com.insurance.backend.notification.service.EmailService;
import com.insurance.backend.user.entity.User;
import com.insurance.backend.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Override
    public ClaimResponse createClaim(ClaimRequest request, String email)
    {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        Claim claim = Claim.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .claimType(request.getClaimType() != null ? request.getClaimType() : ClaimType.OTHER)
                .customer(customer)
                .build();
        Claim saved = claimRepository.save(claim);

        auditLogService.log(email, "CLAIM_CREATED", "CLAIM", saved.getId(),
                "Yeni başvuru oluşturuldu: " + saved.getTitle());

        return toResponse(saved);
    }

    @Override
    public ClaimResponse getClaimById(Long id)
    {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hasar kaydı bulunamadı: " + id));

        auditLogService.log(
                claim.getCustomer().getEmail(),
                "CLAIM_VIEWED",
                "CLAIM",
                id,
                "Başvuru görüntülendi: " + claim.getTitle()
        );
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
        Claim saved = claimRepository.save(claim);

        auditLogService.log(
                saved.getCustomer().getEmail(),
                "STATUS_UPDATED",
                "CLAIM",
                saved.getId(),
                "Durum güncellendi: " + status.name()
        );

        if (status == ClaimStatus.APPROVED || status == ClaimStatus.REJECTED) // Email bildirimi — sadece onay veya red durumunda
        {
            try
            {
                emailService.sendClaimStatusEmail(
                        saved.getCustomer().getEmail(),
                        saved.getCustomer().getFirstName() + " " + saved.getCustomer().getLastName(),
                        saved.getTitle(),
                        status.name()
                );
            }
            catch (Exception e)
            {
                // Email gönderilemese bile işlem devam etsin
            }
        }

        return toResponse(saved);
    }

    @Override
    public ClaimResponse assignClaim(Long claimId, Long userId)
    {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Hasar kaydı bulunamadı: " + claimId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userId));


        claim.setAssignedTo(user);
        Claim saved = claimRepository.save(claim);
        auditLogService.log(
                user.getEmail(),
                "CLAIM_ASSIGNED",
                "CLAIM",
                saved.getId(),
                "Başvuru atandı: " + user.getFirstName() + " " + user.getLastName()
        );
        return toResponse(saved);
    }

    @Override
    public Page<ClaimResponse> getAllClaimsPaged(int page, int size)
    {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return claimRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public Page<ClaimResponse> getClaimsByCustomerPaged(String email, int page, int size)
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return claimRepository.findByCustomerId(user.getId(), pageable).map(this::toResponse);
    }

    @Override
    public Map<String, Long> getStats()
    {
        List<Claim> allClaims = claimRepository.findAll();

        return Map.of(
                "total", (long) allClaims.size(),
                "draft", allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.DRAFT).count(),
                "pending", allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.PENDING).count(),
                "inReview", allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.IN_REVIEW).count(),
                "approved", allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count(),
                "rejected", allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count(),
                "trafficAccident", allClaims.stream().filter(c -> c.getClaimType() == ClaimType.TRAFFIC_ACCIDENT).count(),
                "theft", allClaims.stream().filter(c -> c.getClaimType() == ClaimType.THEFT).count(),
                "naturalDisaster", allClaims.stream().filter(c -> c.getClaimType() == ClaimType.NATURAL_DISASTER).count(),
                "other", allClaims.stream().filter(c -> c.getClaimType() == null || c.getClaimType() == ClaimType.OTHER).count()
        );
    }

    @Override
    public Map<String, Long> getStatsByCustomer(String email)//sadece giris yapan customerın statları
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanici bulunamadi"));

        List<Claim> claims = claimRepository.findByCustomerId(user.getId());
        return Map.of(
                "total", (long) claims.size(),
                "draft", claims.stream().filter(c -> c.getStatus() == ClaimStatus.DRAFT).count(),
                "pending", claims.stream().filter(c -> c.getStatus() == ClaimStatus.PENDING).count(),
                "approved", claims.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count(),
                "rejected", claims.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count()
        );
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
