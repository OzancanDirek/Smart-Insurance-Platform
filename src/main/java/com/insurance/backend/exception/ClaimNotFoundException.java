package com.insurance.backend.exception;

public class ClaimNotFoundException extends RuntimeException
{
    public ClaimNotFoundException(Long id)
    {
        super("Hasar kaydı bulunamadı: " + id);
    }
}