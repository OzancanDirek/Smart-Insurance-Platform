package com.insurance.backend.document.entity;

import com.insurance.backend.claim.entity.Claim;
import com.insurance.backend.document.enums.DocumentType;
import com.insurance.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileType;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Column(columnDefinition = "TEXT")
    private String ocrText;

    private Integer riskScore;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate()
    {
        createdAt = LocalDateTime.now();
    }
}