package com.insurance.backend.claim.service;

import com.insurance.backend.claim.dto.ClaimRequest;
import com.insurance.backend.claim.dto.ClaimResponse;
import com.insurance.backend.claim.enums.ClaimStatus;

import java.util.List;

public interface IClaimService
{
    ClaimResponse createClaim(ClaimRequest request, String email);

    ClaimResponse getClaimById(Long id); //istenilen tek basvuruyu getir

    List<ClaimResponse> getAllClaims(); //Sistemdeki tüm başvuruları getirir.

    List<ClaimResponse> getClaimsByCustomer(String email); //Müşterinin kendi başvurularını görmesi için

    List<ClaimResponse> getClaimsByStatus(ClaimStatus status);//Duruma göre filtreleme.

    ClaimResponse updateStatus(Long id, ClaimStatus status);

    ClaimResponse assignClaim(Long claimId, Long userId);//Başvuruyu bir çalışana atar.
}