package com.insurance.backend.document.controller;

import com.insurance.backend.document.dto.DocumentResponse;
import com.insurance.backend.document.enums.DocumentType;
import com.insurance.backend.document.service.IDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController
{
    private final IDocumentService documentService;

    @PostMapping("/upload/{claimId}")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long claimId,
            @AuthenticationPrincipal String email)
    {
        return ResponseEntity.ok(documentService.uploadDocument(file, claimId, email));
    }

    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByClaimId(@PathVariable Long claimId)
    {
        return ResponseEntity.ok(documentService.getDocumentsByClaimId(claimId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id)
    {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    @GetMapping("/type/{documentType}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByType(@PathVariable DocumentType documentType)
    {
        return ResponseEntity.ok(documentService.getDocumentsByType(documentType));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponse>> search(@RequestParam String q)
    {
        return ResponseEntity.ok(documentService.searchByText(q));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<DocumentResponse>> getAllPaged(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size)
    {
        return ResponseEntity.ok(documentService.getAllDocumentsPaged(page, size));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id)
    {
        try
        {
            DocumentResponse doc = documentService.getDocumentById(id);
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