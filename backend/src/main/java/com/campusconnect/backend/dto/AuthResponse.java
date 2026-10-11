package com.campusconnect.backend.dto;

public record AuthResponse(String token, Long userId, String fullName, String email, String role) {}
