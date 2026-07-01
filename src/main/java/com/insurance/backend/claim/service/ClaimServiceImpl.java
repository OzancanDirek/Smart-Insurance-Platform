package com.insurance.backend.claim.service;

import com.insurance.backend.audit.service.AuditLogService;
import com.insurance.backend.claim.dto.ClaimRequest;
import com.insurance.backend.claim.dto.ClaimResponse;
import com.insurance.backend.claim.entity.Claim;
import com.insurance.backend.claim.enums.ClaimStatus;
import com.insurance.backend.claim.enums.ClaimType;
import com.insurance.backend.claim.repository.ClaimRepository;
import com.insurance.backend.document.entity.Document;
import com.insurance.backend.document.enums.DocumentType;
import com.insurance.backend.document.repository.DocumentRepository;
import com.insurance.backend.document.search.DocumentSearchRepository;
import com.insurance.backend.document.service.DocumentValidationService;
import com.insurance.backend.exception.ClaimNotFoundException;
import com.insurance.backend.exception.MissingDocumentsException;
import com.insurance.backend.exception.UserNotFoundException;
import com.insurance.backend.notification.service.EmailService;
import com.insurance.backend.user.entity.User;
import com.insurance.backend.user.repository.UserRepository;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClaimServiceImpl implements IClaimService
{
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final DocumentValidationService documentValidationService;
    private final DocumentRepository documentRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final MinioClient minioClient;
    private final DocumentSearchRepository documentSearchRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public ClaimResponse createClaim(ClaimRequest request, String email)
    {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

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
    public ClaimResponse updateStatus(Long id, ClaimStatus status, String performedBy)
    {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

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
                throw new MissingDocumentsException(missingDocs);
            }
        }

        ClaimStatus oldStatus = claim.getStatus();
        claim.setStatus(status);
        Claim saved = claimRepository.save(claim);

        auditLogService.log(
                performedBy,  // işlemi yapan kişinin emaili
                "STATUS_UPDATED",
                "CLAIM",
                saved.getId(),
                "Durum güncellendi: " + oldStatus.name() + " → " + status.name()
        );

        if (status == ClaimStatus.APPROVED || status == ClaimStatus.REJECTED)
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
    public Page<ClaimResponse> getAllClaimsPaged(int page, int size)
    {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return claimRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public ClaimResponse getClaimById(Long id, String performedBy)
    {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        auditLogService.log(
                performedBy,  // işlemi yapan kişinin emaili
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        List<Claim> claims = claimRepository.findByCustomerId(user.getId());

        auditLogService.log(
                user.getEmail(),
                "CLAIM_LIST_VIEWED",
                "USER",
                user.getId(),
                "Müşteri kendi başvuru listesini görüntüledi"
        );

        return claims.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getStatsByCustomer(String email)//sadece giris yapan customerın statları
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        List<Claim> claims = claimRepository.findByCustomerId(user.getId());
        return Map.of(
                "total", (long) claims.size(),
                "draft", claims.stream().filter(c -> c.getStatus() == ClaimStatus.DRAFT).count(),
                "pending", claims.stream().filter(c -> c.getStatus() == ClaimStatus.PENDING).count(),
                "approved", claims.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count(),
                "rejected", claims.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count()
        );
    }

    @Override
    public Page<ClaimResponse> getClaimsByCustomerPaged(String email, int page, int size)// Müşterinin kendi başvuruları paginationla getir
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        auditLogService.log(
                user.getEmail(),
                "SELF_CLAIM_VIEWED_",
                "USER",
                user.getId(),
                "Musteri kendi basvuru listesini goruntuledi"
        );
        return claimRepository.findByCustomerId(user.getId(), pageable).map(this::toResponse);
    }


    @Override
    public List<ClaimResponse> getClaimsByStatus(ClaimStatus status)//Belirli bir duruma sahip başvuruları filtreler
    {
        return claimRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    @Override
    public ClaimResponse assignClaim(Long claimId, Long userId, String performedBy)
    {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));

        User newAssignee = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String previousAssignee = claim.getAssignedTo() != null
                ? claim.getAssignedTo().getFirstName() + " " + claim.getAssignedTo().getLastName()
                : "Atanmamış";

        claim.setAssignedTo(newAssignee);
        Claim saved = claimRepository.save(claim);

        auditLogService.log(
                performedBy,  // işlemi yapan MANAGER/ADMIN'in emaili
                "CLAIM_ASSIGNED",
                "CLAIM",
                saved.getId(),
                "Başvuru atandı: " + previousAssignee + " → " + newAssignee.getFirstName() + " " + newAssignee.getLastName()
        );

        return toResponse(saved);
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
    public Map<String, Long> getStatsByAssignedStaff(String email)// Personele atanmış başvuruların istatistiklerini hesaplar
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        List<Claim> claimsAssingToStaff = claimRepository.findByAssignedToId(user.getId());
        return Map.of(
                "total", (long) claimsAssingToStaff.size(),
                "pending", claimsAssingToStaff.stream().filter(c -> c.getStatus() == ClaimStatus.PENDING).count(),
                "inReview", claimsAssingToStaff.stream().filter(c -> c.getStatus() == ClaimStatus.IN_REVIEW).count(),
                "approved", claimsAssingToStaff.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count(),
                "rejected", claimsAssingToStaff.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count()
        );
    }


    @Override
    public Page<ClaimResponse> getClaimsByAssignedStaffPaged(String email, int page, int size)// Personel başvuruları paginationla getir
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        auditLogService.log(
                user.getEmail(),
                "SELF_CLAIM_WATCHED",
                "USER",
                user.getId(),
                "Personel kendi atanan başvuru listesini görüntüledi"

        );
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return claimRepository.findByAssignedToId(user.getId(), pageable).map(this::toResponse);
    }


    @Override
    public void deleteClaim(Long id, String performedBy)
    {
        Claim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));

        // Önce claim'e ait tüm belgelerin silinmesi icin
        List<Document> documents = documentRepository.findByClaimId(id);
        for (Document doc : documents)
        {
            try
            {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(doc.getFilePath().replace(bucketName + "/", ""))
                        .build());
            }
            catch (Exception e)
            {
                // MinIO'dan silinemese bile devam edecek
            }
            documentSearchRepository.deleteById(String.valueOf(doc.getId()));
        }

        auditLogService.log(
                performedBy,
                "CLAIM_DELETED",
                "CLAIM",
                id,
                "Başvuru silindi: " + claim.getTitle() + " (" + documents.size() + " belge ile birlikte)"
        );

        claimRepository.delete(claim);
    }

    @Override
    public byte[] exportClaimsToExcel(String email, String role) throws Exception
    {
        List<ClaimResponse> claims;

        if (role.equals("ROLE_CUSTOMER"))
        {
            claims = getClaimsByCustomer(email);
        }
        else if (role.equals("ROLE_STAFF") || role.equals("ROLE_EXPERT"))
        {
            User staffUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException(email));

            claims = claimRepository.findByAssignedToId(staffUser.getId())
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        else
        {
            claims = getAllClaims();
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Başvurular");

        // Başlık stili
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Başlık satırı
        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID", "Başlık", "Tür", "Durum", "Müşteri", "Atanan Personel", "Oluşturulma Tarihi"};
        for (int i = 0; i < columns.length; i++)
        {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 5000);
        }

        // Veri satırları
        Map<String, String> statusLabels = Map.of(
                "DRAFT", "Taslak",
                "PENDING", "Bekliyor",
                "IN_REVIEW", "İncelemede",
                "APPROVED", "Onaylandı",
                "REJECTED", "Reddedildi");

        Map<String, String> typeLabels = Map.of(
                "TRAFFIC_ACCIDENT", "Trafik Kazası",
                "THEFT", "Hırsızlık",
                "NATURAL_DISASTER", "Doğal Afet",
                "OTHER", "Diğer");

        int rowNum = 1;
        for (ClaimResponse claim : claims)
        {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(claim.getId());
            row.createCell(1).setCellValue(claim.getTitle());
            row.createCell(2).setCellValue(typeLabels.getOrDefault(claim.getClaimType() != null ? claim.getClaimType().name() : "OTHER", "Diğer"));
            row.createCell(3).setCellValue(statusLabels.getOrDefault(claim.getStatus().name(), claim.getStatus().name()));
            row.createCell(4).setCellValue(claim.getCustomerFullName());
            row.createCell(5).setCellValue(claim.getAssignedToFullName() != null ? claim.getAssignedToFullName() : "Atanmamış");
            row.createCell(6).setCellValue(claim.getCreatedAt().toString());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        System.out.println("Excel dosyasi basariyla olusturuldu");
        return outputStream.toByteArray();
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
                .customerEmail(claim.getCustomer().getEmail())
                .assignedToEmail(claim.getAssignedTo() != null ? claim.getAssignedTo().getEmail() : null)
                .build();
    }
}
