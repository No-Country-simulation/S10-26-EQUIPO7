package com.repusense.backend.auth;

import com.repusense.backend.auth.dto.AuthResponse;
import com.repusense.backend.auth.dto.LoginRequest;
import com.repusense.backend.auth.dto.RegisterRequest;
import com.repusense.backend.auth.dto.UserResponse;
import com.repusense.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "La cuenta fue creada. Ya podés iniciar sesión.",
                "user", user
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
