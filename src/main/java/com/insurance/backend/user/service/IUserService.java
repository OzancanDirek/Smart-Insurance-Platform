package com.insurance.backend.user.service;

import com.insurance.backend.user.dto.ChangePasswordRequest;
import com.insurance.backend.user.dto.UserRequest;
import com.insurance.backend.user.dto.UserResponse;
import com.insurance.backend.user.dto.UserUpdateRequest;

import java.util.List;

public interface IUserService
{
    UserResponse createUser(UserRequest request);

    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long id, UserUpdateRequest request);

    void deleteUser(Long id);

    void changePassword(String email, ChangePasswordRequest request);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
    UserResponse toggleUserActive(Long id, String performedBy);
}
