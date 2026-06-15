package com.insurance.backend.claim.controller;

import com.insurance.backend.claim.dto.ClaimRequest;
import com.insurance.backend.claim.dto.ClaimResponse;
import com.insurance.backend.claim.enums.ClaimStatus;
import com.insurance.backend.claim.service.IClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController
{
    private final IClaimService claimService;

    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody ClaimRequest request, @AuthenticationPrincipal String email)
    {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(claimService.createClaim(request, email));
    }

    @GetMapping
    public ResponseEntity<List<ClaimResponse>> getAllClaims()
    {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable Long id)
    {
        return ResponseEntity.ok(claimService.getClaimById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ClaimResponse>> getMyClaims(@AuthenticationPrincipal String email)
    {
        return ResponseEntity.ok(claimService.getClaimsByCustomer(email));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ClaimResponse>> getClaimsByStatus(@PathVariable ClaimStatus status)
    {
        return ResponseEntity.ok(claimService.getClaimsByStatus(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ClaimResponse> updateStatus(@PathVariable Long id, @RequestParam ClaimStatus status)
    {
        return ResponseEntity.ok(claimService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ClaimResponse> assignClaim(@PathVariable Long id, @RequestParam Long userId)
    {
        return ResponseEntity.ok(claimService.assignClaim(id, userId));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<ClaimResponse>> getAllPaged(@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "10") int size,Authentication authentication)
    {
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (role.equals("CUSTOMER"))
        {
            return ResponseEntity.ok(claimService.getClaimsByCustomerPaged(email, page, size));
        }
        return ResponseEntity.ok(claimService.getAllClaimsPaged(page, size));
    }
}