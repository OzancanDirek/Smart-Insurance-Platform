package com.insurance.backend.user.service;

import com.insurance.backend.user.dto.UserRequest;
import com.insurance.backend.user.dto.UserResponse;
import com.insurance.backend.user.dto.UserUpdateRequest;

import java.util.List;

public interface IUserService
{
    UserResponse createUser(UserRequest request);

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long id, UserUpdateRequest request);

    void deleteUser(Long id);
}
