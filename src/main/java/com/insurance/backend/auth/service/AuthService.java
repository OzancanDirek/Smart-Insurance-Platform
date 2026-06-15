package com.insurance.backend.auth.service;

import com.insurance.backend.audit.service.AuditLogService;
import com.insurance.backend.auth.dto.LoginRequest;
import com.insurance.backend.auth.dto.LoginResponse;
import com.insurance.backend.config.JwtUtil;
import com.insurance.backend.user.entity.User;
import com.insurance.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService
{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;

    public LoginResponse login(LoginRequest request)
    {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
        {
            auditLogService.log(request.getEmail(), "LOGIN_FAILED", "USER", null, "Hatalı şifre ile giriş denemesi");
            throw new RuntimeException("Şifre hatalı");
        }

        if (!user.isActive())
        {
            throw new RuntimeException("Hesap aktif değil");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        auditLogService.log(user.getEmail(), "LOGIN", "USER", user.getId(), "Kullanıcı giriş yaptı");
        return new LoginResponse(token, user.getEmail(), user.getRole().name());
    }
}