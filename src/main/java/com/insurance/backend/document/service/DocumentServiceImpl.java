package com.insurance.backend.document.service;

import com.insurance.backend.claim.entity.Claim;
import com.insurance.backend.claim.repository.ClaimRepository;
import com.insurance.backend.document.dto.DocumentResponse;
import com.insurance.backend.document.entity.Document;
import com.insurance.backend.document.enums.DocumentType;
import com.insurance.backend.document.repository.DocumentRepository;
import com.insurance.backend.document.search.DocumentSearchDocument;
import com.insurance.backend.document.search.DocumentSearchRepository;
import com.insurance.backend.user.entity.User;
import com.insurance.backend.user.repository.UserRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentServiceImpl implements IDocumentService
{
    private final DocumentRepository documentRepository;
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;
    private final MinioClient minioClient;
    private final DocumentSearchRepository documentSearchRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public DocumentResponse uploadDocument(MultipartFile file, Long claimId, String email)
    {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Hasar kaydı bulunamadı: " + claimId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // MinIO'ya yükle
        uploadToMinio(file, fileName);

        // OCR işlemi
        String ocrText = extractTextWithOcr(file);

        // Belge türü tespiti
        DocumentType documentType = detectDocumentType(ocrText);

        // Risk skoru
        Integer riskScore = calculateRiskScore(ocrText, claimId);

        Document document = Document.builder()
                .fileName(file.getOriginalFilename())
                .filePath(bucketName + "/" + fileName)
                .fileType(file.getContentType())
                .documentType(documentType)
                .ocrText(ocrText)
                .riskScore(riskScore)
                .claim(claim)
                .uploadedBy(user)
                .build();

        // sqle kaydet
        Document saved = documentRepository.save(document);

        // Elasticsearch'e kaydet
        DocumentSearchDocument searchDoc = DocumentSearchDocument.builder()
                .id(String.valueOf(saved.getId()))
                .ocrText(ocrText)
                .fileName(file.getOriginalFilename())
                .documentType(documentType.name())
                .claimId(claimId)
                .documentId(saved.getId())
                .riskScore(riskScore)
                .build();
        documentSearchRepository.save(searchDoc);

        return toResponse(saved);
    }

    @Override
    public List<DocumentResponse> getDocumentsByClaimId(Long claimId)
    {
        return documentRepository.findByClaimId(claimId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentResponse getDocumentById(Long id)
    {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Belge bulunamadı: " + id));

        return toResponse(document);
    }

    @Override
    public List<DocumentResponse> getDocumentsByType(DocumentType documentType)
    {
        return documentRepository.findByDocumentType(documentType)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void uploadToMinio(MultipartFile file, String fileName)
    {
        try
        {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists)
            {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException("Dosya yuklenemedi: " + e.getMessage());
        }
    }

    private String extractTextWithOcr(MultipartFile file)
    {
        try
        {
            File tempFile = File.createTempFile("ocr_", "_" + file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile))
            {
                fos.write(file.getBytes());
            }

            Tesseract tesseract = new Tesseract();

            tesseract.setDatapath("/opt/homebrew/share/tessdata");
            tesseract.setLanguage("tur+eng");

            String text = tesseract.doOCR(tempFile);
            Files.deleteIfExists(tempFile.toPath());
            return text;
        }
        catch (TesseractException e)
        {
            return "";
        }
        catch (Exception e)
        {
            return "";
        }
    }

    private static final Map<DocumentType, List<String>> KEYWORDS = Map.of(
            DocumentType.IDENTITY, List.of("kimlik", "tc.", "t.c.", "nüfus"),
            DocumentType.VEHICLE_LICENSE, List.of("tescil", "ruhsat", "plaka", "araç", "arac"),
            DocumentType.EXPERT_REPORT, List.of("eksper", "hasar tespiti"),
            DocumentType.INSURANCE_POLICY, List.of("police", "sigorta"),
            DocumentType.ACCIDENT_REPORT, List.of("tutanak", "kaza tutanağı", "tespit"),
            DocumentType.ACCIDENT_PHOTO, List.of("kaza", "foto", "fotoğraf", "hasar", "konum")
    );

    private DocumentType detectDocumentType(String text)
    {
        if (text == null || text.isEmpty()) return DocumentType.OTHER;

        String lower = text.toLowerCase().replaceAll("\\s+", " ");

        return KEYWORDS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(lower::contains))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(DocumentType.OTHER);
    }

    private Integer calculateRiskScore(String ocrText, Long claimId)
    {
        // OCR metni boş geldi: +20 puan,
        // Aynı claim'e 10'dan fazla belge yüklendi: +15 puan
        // OCR metni çok kısa (10 karakterden az): +25 puan
        int score = 0;

        if (ocrText == null || ocrText.trim().isEmpty())
        {
            score += 20;
        }

        List<Document> existingDocs = documentRepository.findByClaimId(claimId);
        if (existingDocs.size() > 10)
        {
            score += 15;
        }

        if (ocrText != null && ocrText.length() < 10)
        {
            score += 25;
        }

        return Math.min(score, 100);
    }

    public List<DocumentResponse> searchByText(String text)
    {
        List<DocumentSearchDocument> searchResults = documentSearchRepository.findByOcrTextContaining(text);
        List<DocumentResponse> responses = new ArrayList<>();

        for (DocumentSearchDocument searchDoc : searchResults)
        {
            Optional<Document> document = documentRepository.findById(searchDoc.getDocumentId());
            if (document.isPresent())
            {
                DocumentResponse response = toResponse(document.get());
                responses.add(response);
            }
        }

        return responses;
    }

    private DocumentResponse toResponse(Document document)
    {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .documentType(document.getDocumentType())
                .ocrText(document.getOcrText())
                .riskScore(document.getRiskScore())
                .claimId(document.getClaim().getId())
                .uploadedById(document.getUploadedBy().getId())
                .uploadedByFullName(document.getUploadedBy().getFirstName() + " " + document.getUploadedBy().getLastName())
                .createdAt(document.getCreatedAt())
                .build();
    }
}