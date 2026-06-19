package com.insurance.backend.user.service;

import com.insurance.backend.audit.service.AuditLogService;
import com.insurance.backend.exception.EmailAlreadyExistsException;
import com.insurance.backend.exception.UserNotFoundException;
import com.insurance.backend.user.dto.UserRequest;
import com.insurance.backend.user.dto.UserResponse;
import com.insurance.backend.user.entity.User;
import com.insurance.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

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
    public UserResponse getUserByEmail(String email)
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
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
    public UserResponse updateUser(Long id, UserRequest request)
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