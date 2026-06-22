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
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController
{
    private final IClaimService claimService;

    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody ClaimRequest request, @AuthenticationPrincipal String email, Authentication authentication)
    {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ROLE_CUSTOMER"))
        {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(claimService.createClaim(request, email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id, Authentication authentication)
    {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ROLE_ADMIN"))
        {
            return ResponseEntity.status(403).build();
        }
        claimService.deleteClaim(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ClaimResponse>> getAllClaims()
    {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable Long id, Authentication authentication)
    {
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        ClaimResponse claim = claimService.getClaimById(id);

        if (role.equals("ROLE_CUSTOMER") && !claim.getCustomerEmail().equals(email))
        {
            return ResponseEntity.status(403).build();
        }

        if ((role.equals("ROLE_STAFF") || role.equals("ROLE_EXPERT"))
                && (claim.getAssignedToEmail() == null || !claim.getAssignedToEmail().equals(email)))
        {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(claim);
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
    public ResponseEntity<ClaimResponse> updateStatus(@PathVariable Long id, @RequestParam ClaimStatus status, Authentication authentication)
    {
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (status == ClaimStatus.APPROVED || status == ClaimStatus.REJECTED)
        {
            if (!role.equals("ROLE_MANAGER") && !role.equals("ROLE_ADMIN"))
            {
                return ResponseEntity.status(403).build();
            }
        }
        else if (status == ClaimStatus.IN_REVIEW)
        {
            if (!role.equals("ROLE_STAFF") && !role.equals("ROLE_EXPERT") && !role.equals("ROLE_MANAGER") && !role.equals("ROLE_ADMIN"))
            {
                return ResponseEntity.status(403).build();
            }
        }
        else if (status == ClaimStatus.PENDING)
        {
            if (!role.equals("ROLE_CUSTOMER") && !role.equals("ROLE_STAFF") && !role.equals("ROLE_EXPERT") && !role.equals("ROLE_ADMIN"))
            {
                return ResponseEntity.status(403).build();
            }
        }

        return ResponseEntity.ok(claimService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ClaimResponse> assignClaim(@PathVariable Long id, @RequestParam Long userId, Authentication authentication)
    {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ROLE_MANAGER") && !role.equals("ROLE_ADMIN"))
        {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(claimService.assignClaim(id, userId));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<ClaimResponse>> getAllPaged(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Authentication authentication)
    {
        String email = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if (role.equals("ROLE_CUSTOMER"))
        {
            return ResponseEntity.ok(claimService.getClaimsByCustomerPaged(email, page, size));
        }
        if (role.equals("ROLE_STAFF") || role.equals("ROLE_EXPERT"))
        {
            return ResponseEntity.ok(claimService.getClaimsByAssignedStaffPaged(email, page, size));
        }
        return ResponseEntity.ok(claimService.getAllClaimsPaged(page, size));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats(Authentication authentication)
    {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ROLE_ADMIN") && !role.equals("ROLE_MANAGER"))
        {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(claimService.getStats());
    }

    @GetMapping("/stats/me")
    public ResponseEntity<Map<String, Long>> getMyStats(Authentication authentication)
    {
        return ResponseEntity.ok(claimService.getStatsByCustomer(authentication.getName()));
    }

    @GetMapping("/stats/assigned")
    public ResponseEntity<Map<String, Long>> getAssignedStats(Authentication authentication)
    {
        return ResponseEntity.ok(claimService.getStatsByAssignedStaff(authentication.getName()));
    }
}