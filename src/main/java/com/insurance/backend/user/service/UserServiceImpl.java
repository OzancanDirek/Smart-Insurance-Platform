package com.insurance.backend.user.service;

import com.insurance.backend.notification.service.EmailService;
import com.insurance.backend.user.dto.ChangePasswordRequest;
import com.insurance.backend.audit.service.AuditLogService;
import com.insurance.backend.exception.EmailAlreadyExistsException;
import com.insurance.backend.exception.InvalidCredentialsException;
import com.insurance.backend.exception.UserNotFoundException;
import com.insurance.backend.user.dto.UserRequest;
import com.insurance.backend.user.dto.UserResponse;
import com.insurance.backend.user.dto.UserUpdateRequest;
import com.insurance.backend.user.entity.User;
import com.insurance.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Override
    public UserResponse createUser(UserRequest request)
    {
        if (userRepository.existsByEmail(request.getEmail()))
        {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole())
                .active(true)
                .build();

        User saved = userRepository.save(user);
        auditLogService.log(saved.getEmail(), "USER_CREATED", "USER", saved.getId(),
                "Yeni kullanıcı oluşturuldu: " + saved.getFirstName() + " " + saved.getLastName() + " (" + saved.getRole() + ")");
        return toResponse(saved);
    }

    @Override
    public UserResponse getUserById(Long id)
    {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toResponse(user);
    }


    @Override
    public List<UserResponse> getAllUsers()
    {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request)
    {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());

        User updated = userRepository.save(user);
        auditLogService.log(updated.getEmail(), "USER_UPDATED", "USER", updated.getId(),
                "Kullanıcı güncellendi: " + updated.getFirstName() + " " + updated.getLastName());

        return toResponse(updated);
    }

    @Override
    public void deleteUser(Long id)
    {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        auditLogService.log(user.getEmail(), "USER_DELETED", "USER", id,
                "Kullanıcı silindi: " + user.getFirstName() + " " + user.getLastName());

        userRepository.deleteById(id);
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request)
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
        {
            throw new InvalidCredentialsException("Mevcut şifre hatalı");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log(email, "PASSWORD_CHANGED", "USER", user.getId(), "Şifre değiştirildi");
    }

    @Override
    public void forgotPassword(String email)
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));//1 saatlik gcerli
        userRepository.save(user);

        auditLogService.log(
                email,
                "PASSWORD_RESET_REQUESTED",
                "USER",
                user.getId(),
                "Sifre yenileme talebi"
        );
        emailService.sendPasswordResetEmail(email, user.getFirstName(), token);
    }

    @Override
    public void resetPassword(String token, String newPassword)
    {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new UserNotFoundException(token));
        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now()))
        {
            throw new RuntimeException("Token süresi dolmuş");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        auditLogService.log(
                user.getEmail(),
                "PASSWORD_RESET",
                "USER",
                user.getId(),
                "Sifre sifirlandi"
        );
    }

    @Override
    public UserResponse toggleUserActive(Long id, String performedBy)
    {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setActive(!user.isActive());
        User saved = userRepository.save(user);

        auditLogService.log(
                performedBy,
                "USER_STATUS_CHANGED",
                "USER",
                id,
                "Kullanıcı durumu değiştirildi: " + saved.getEmail() + " → " + (saved.isActive() ? "Aktif" : "Pasif"));

        return toResponse(saved);
    }

    private UserResponse toResponse(User user)
    {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}