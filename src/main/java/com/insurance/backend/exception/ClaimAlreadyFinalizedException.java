package com.insurance.backend.exception;

public class ClaimAlreadyFinalizedException extends RuntimeException
{
    public ClaimAlreadyFinalizedException(Long claimId)
    {
        super("Başvuru #" + claimId + " sonuçlandırılmıştır, yeni belge yüklenemez.");
    }
}