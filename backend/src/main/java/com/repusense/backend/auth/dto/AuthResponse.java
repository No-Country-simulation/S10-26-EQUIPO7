package com.repusense.backend.auth.dto;

public record AuthResponse(String token, UserResponse user) {
}
