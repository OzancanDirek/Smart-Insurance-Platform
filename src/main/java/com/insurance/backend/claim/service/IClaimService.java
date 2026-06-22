package com.insurance.backend.claim.service;

import com.insurance.backend.claim.dto.ClaimRequest;
import com.insurance.backend.claim.dto.ClaimResponse;
import com.insurance.backend.claim.entity.Claim;
import com.insurance.backend.claim.enums.ClaimStatus;
import org.springframework.data.domain.Page;


import java.util.List;
import java.util.Map;

public interface IClaimService
{
    ClaimResponse createClaim(ClaimRequest request, String email);

    ClaimResponse getClaimById(Long id); //istenilen tek basvuruyu getir

    List<ClaimResponse> getAllClaims(); //Sistemdeki tüm başvuruları getirir.

    List<ClaimResponse> getClaimsByCustomer(String email); //Müşterinin kendi başvurularını görmesi için

    List<ClaimResponse> getClaimsByStatus(ClaimStatus status);//Duruma göre filtreleme.

    ClaimResponse updateStatus(Long id, ClaimStatus status);

    ClaimResponse assignClaim(Long claimId, Long userId);//Başvuruyu bir çalışana atar.

    Page<ClaimResponse> getAllClaimsPaged(int page, int size); //tüm başvuruları sayfalı getirir

    Page<ClaimResponse> getClaimsByCustomerPaged(String email, int page, int size); // Müşteri için sadece kendinkini getir

    Map<String, Long> getStats();

    Map<String, Long> getStatsByCustomer(String email);

    Map<String, Long> getStatsByAssignedStaff(String email);

    Page<ClaimResponse> getClaimsByAssignedStaffPaged(String email, int page, int size);

    public void  deleteClaim(Long id);
}