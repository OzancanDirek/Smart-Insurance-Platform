package com.insurance.backend.claim.repository;

import com.insurance.backend.claim.entity.Claim;
import com.insurance.backend.claim.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long>
{
    List<Claim> findByCustomerId(Long customerId);

    List<Claim> findByStatus(ClaimStatus status);

    List<Claim> findByAssignedToId(Long userId);
}