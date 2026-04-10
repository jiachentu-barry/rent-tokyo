package com.jiachentu.rent_tokyo.service;

import com.jiachentu.rent_tokyo.dto.AuthResponse;
import com.jiachentu.rent_tokyo.dto.LoginRequest;
import com.jiachentu.rent_tokyo.dto.RegisterRequest;
import com.jiachentu.rent_tokyo.entity.User;
import com.jiachentu.rent_tokyo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName().trim())
                .build();

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId(), saved.getEmail());
        return buildResponse(saved, token);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return buildResponse(user, token);
    }

    private AuthResponse buildResponse(User user, String token) {
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .token(token)
                .build();
    }
}
