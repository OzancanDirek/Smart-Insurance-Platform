package com.insurance.backend.exception;

public class UserNotFoundException extends RuntimeException
{
    public UserNotFoundException(String email)
    {
        super("Kullanıcı bulunamadı: " + email);
    }

    public UserNotFoundException(Long id)
    {
        super("Kullanıcı bulunamadı: " + id);
    }
}