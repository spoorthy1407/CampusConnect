package com.campusconnect.backend.service;

import com.campusconnect.backend.dto.AuthResponse;
import com.campusconnect.backend.dto.Dtos.LoginRequest;
import com.campusconnect.backend.dto.Dtos.RegisterRequest;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.UserRepository;
import com.campusconnect.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    @Value("${app.allowed-email-domain:}")
    private String allowedDomain;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtUtil jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (!allowedDomain.isBlank() && !email.endsWith("@" + allowedDomain))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please use your college email (@" + allowedDomain + ")");
        if (users.existsByEmail(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.fullName = r.fullName().trim();
        u.email = email;
        u.passwordHash = encoder.encode(r.password());
        u.role = User.Role.STUDENT;
        return toResponse(users.save(u));
    }

    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.passwordHash))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return toResponse(u);
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.generate(u.email, u.role.name()), u.id, u.fullName, u.email, u.role.name());
    }
}
