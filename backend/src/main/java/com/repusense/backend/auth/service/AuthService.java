package com.repusense.backend.auth.service;

import com.repusense.backend.auth.dto.AuthResponse;
import com.repusense.backend.auth.dto.LoginRequest;
import com.repusense.backend.auth.dto.RegisterRequest;
import com.repusense.backend.auth.dto.UserResponse;
import com.repusense.backend.model.AppUser;
import com.repusense.backend.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una cuenta registrada con ese email."
            );
        }

        AppUser appUser = new AppUser();
        appUser.setName(request.name().trim());
        appUser.setEmail(email);
        appUser.setPasswordHash(passwordEncoder.encode(request.password()));
        appUser.setActive(true);

        AppUser savedUser = appUserRepository.save(appUser);
        return toUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser appUser = appUserRepository
                .findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(AppUser::isActive)
                .orElseThrow(AuthService::invalidCredentials);

        if (!passwordEncoder.matches(
                request.password(),
                appUser.getPasswordHash()
        )) {
            throw invalidCredentials();
        }

        return new AuthResponse(
                jwtService.generateToken(appUser),
                toUserResponse(appUser)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toUserResponse(AppUser appUser) {
        return new UserResponse(
                appUser.getId(),
                appUser.getName(),
                appUser.getEmail()
        );
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Email o contraseña incorrectos."
        );
    }
}
