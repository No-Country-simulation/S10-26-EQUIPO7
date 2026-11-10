package com.repusense.backend.auth.dto;

import com.repusense.backend.model.UserRole;

public record UserResponse(Long id, String name, String email, UserRole role) {
}
