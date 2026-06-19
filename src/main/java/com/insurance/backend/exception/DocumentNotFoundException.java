package com.insurance.backend.exception;

public class DocumentNotFoundException extends RuntimeException
{
    public DocumentNotFoundException(Long id)
    {
        super("Belge bulunamadı: " + id);
    }
}