package com.campusconnect.backend.controller;

import com.campusconnect.backend.dto.AuthResponse;
import com.campusconnect.backend.dto.Dtos.LoginRequest;
import com.campusconnect.backend.dto.Dtos.RegisterRequest;
import com.campusconnect.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }
}
