package com.insurance.backend.document.controller;

import com.insurance.backend.claim.service.IClaimService;
import com.insurance.backend.document.dto.DocumentResponse;
import com.insurance.backend.document.enums.DocumentType;
import com.insurance.backend.document.service.IDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.insurance.backend.claim.dto.ClaimResponse;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController
{
    private final IDocumentService documentService;
    private final IClaimService claimService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, Authentication authentication)
    {
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        DocumentResponse doc = documentService.getDocumentById(id);
        ClaimResponse claim = claimService.getClaimById(doc.getClaimId(), authentication.getName());

        if (role.equals("ROLE_CUSTOMER") && !claim.getCustomerEmail().equals(email))
        {
            return ResponseEntity.status(403).build();
        }

        if ((role.equals("ROLE_STAFF") || role.equals("ROLE_EXPERT"))
                && (claim.getAssignedToEmail() == null || !claim.getAssignedToEmail().equals(email)))
        {
            return ResponseEntity.status(403).build();
        }

        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload/{claimId}")
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file,
                                                           @PathVariable Long claimId,
                                                           @AuthenticationPrincipal String email)
    {
        return ResponseEntity.ok(documentService.uploadDocument(file, claimId, email));
    }

    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByClaimId(@PathVariable Long claimId, Authentication authentication)
    {
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        ClaimResponse claim = claimService.getClaimById(claimId, authentication.getName());
        if (role.equals("ROLE_CUSTOMER") && !claim.getCustomerEmail().equals(email))
        {
            return ResponseEntity.status(403).build();
        }

        if ((role.equals("ROLE_STAFF") || role.equals("ROLE_EXPERT"))
                && (claim.getAssignedToEmail() == null || !claim.getAssignedToEmail().equals(email)))
        {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(documentService.getDocumentsByClaimId(claimId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id, Authentication authentication)
    {
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        DocumentResponse doc = documentService.getDocumentById(id);
        ClaimResponse claim = claimService.getClaimById(doc.getClaimId(), email);

        if (role.equals("ROLE_CUSTOMER") && !claim.getCustomerEmail().equals(email))
        {
            return ResponseEntity.status(403).build();
        }

        if ((role.equals("ROLE_STAFF") || role.equals("ROLE_EXPERT"))
                && (claim.getAssignedToEmail() == null || !claim.getAssignedToEmail().equals(email)))
        {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(doc);
    }

    @GetMapping("/type/{documentType}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByType(@PathVariable DocumentType documentType)
    {
        return ResponseEntity.ok(documentService.getDocumentsByType(documentType));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponse>> search(@RequestParam String q, Authentication authentication)
    {
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        List<DocumentResponse> results = documentService.searchByText(q);

        if (role.equals("ROLE_STAFF") || role.equals("ROLE_EXPERT"))
        {
            results = results.stream()
                    .filter(doc -> {
                        ClaimResponse claim = claimService.getClaimById(doc.getClaimId(), email);
                        return claim.getAssignedToEmail() != null && claim.getAssignedToEmail().equals(email);
                    })
                    .collect(Collectors.toList());
        }
        else if (role.equals("ROLE_CUSTOMER"))
        {
            results = results.stream()
                    .filter(doc -> {
                        ClaimResponse claim = claimService.getClaimById(doc.getClaimId(), email);
                        return claim.getCustomerEmail().equals(email);
                    })
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<DocumentResponse>> getAllPaged(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Authentication authentication)
    {
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (role.equals("ROLE_STAFF") || role.equals("ROLE_EXPERT"))
        {
            return ResponseEntity.ok(documentService.getDocumentsByAssignedStaffPaged(email, page, size));
        }
        return ResponseEntity.ok(documentService.getAllDocumentsPaged(page, size));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id, Authentication authentication)
    {
        try
        {
            String email = authentication.getName();
            String role = authentication.getAuthorities().iterator().next().getAuthority();

            DocumentResponse doc = documentService.getDocumentById(id);
            ClaimResponse claim = claimService.getClaimById(doc.getClaimId(), authentication.getName());
            if (role.equals("ROLE_CUSTOMER") && !claim.getCustomerEmail().equals(email))
            {
                return ResponseEntity.status(403).build();
            }

            if ((role.equals("ROLE_STAFF") || role.equals("ROLE_EXPERT"))
                    && (claim.getAssignedToEmail() == null || !claim.getAssignedToEmail().equals(email)))
            {
                return ResponseEntity.status(403).build();
            }

            byte[] data = documentService.downloadDocument(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(doc.getFileType()));
            headers.setContentDisposition(
                    ContentDisposition.inline().filename(doc.getFileName()).build()
            );
            return ResponseEntity.ok().headers(headers).body(data);
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }
}