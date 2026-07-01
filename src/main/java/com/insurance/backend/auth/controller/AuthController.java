package com.insurance.backend.auth.controller;

import com.insurance.backend.auth.dto.LoginRequest;
import com.insurance.backend.auth.dto.LoginResponse;
import com.insurance.backend.auth.service.AuthService;
import com.insurance.backend.user.service.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController
{
    private final AuthService authService;
    private final UserServiceImpl userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request)
    {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody Map<String, String> request)
    {
        userService.forgotPassword(request.get("email"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody Map<String, String> request)
    {
        userService.resetPassword(request.get("token"), request.get("newPassword"));
        return ResponseEntity.ok().build();
    }
}