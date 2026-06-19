package com.insurance.backend.exception;

public class EmailAlreadyExistsException extends RuntimeException
{
    public EmailAlreadyExistsException(String email)
    {
        super("Bu email zaten kayıtlı: " + email);
    }
}