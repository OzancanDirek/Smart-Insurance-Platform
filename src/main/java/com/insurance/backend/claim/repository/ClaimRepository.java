package com.insurance.backend.claim.repository;

import com.insurance.backend.claim.entity.Claim;
import com.insurance.backend.claim.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long>
{
    List<Claim> findByCustomerId(Long customerId); //musterinin kendi basvuruları

    List<Claim> findByStatus(ClaimStatus status); // duruma gore filtreleme

    List<Claim> findByAssignedToId(Long userId); //dashboard istatistikler

    Page<Claim> findAll(Pageable pageable);

    Page<Claim> findByCustomerId(Long customerId, Pageable pageable);

    Page<Claim> findByAssignedToId(Long userId, Pageable pageable); // pagination
}