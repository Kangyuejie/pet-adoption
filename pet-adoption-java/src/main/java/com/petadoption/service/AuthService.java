package com.petadoption.service;

import com.petadoption.dto.request.LoginRequest;
import com.petadoption.dto.request.RegisterRequest;
import com.petadoption.dto.response.AuthResponse;
import com.petadoption.dto.response.UserResponse;
import com.petadoption.entity.User;
import com.petadoption.enums.UserRole;
import com.petadoption.exception.BadRequestException;
import com.petadoption.repository.UserRepository;
import com.petadoption.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already taken");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.fromValue(request.getRole()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        user = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("bearer")
                .user(UserResponse.fromEntity(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        String token = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("bearer")
                .user(UserResponse.fromEntity(user))
                .build();
    }
}
