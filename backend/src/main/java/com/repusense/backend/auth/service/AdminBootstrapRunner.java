package com.repusense.backend.auth.service;

import com.repusense.backend.model.AppUser;
import com.repusense.backend.model.UserRole;
import com.repusense.backend.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrapRunner(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.bootstrap-email:}") String email,
            @Value("${app.admin.bootstrap-password:}") String password) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.isBlank() || password.length() < 12) {
            throw new IllegalStateException(
                    "Configurá APP_ADMIN_BOOTSTRAP_EMAIL y una "
                            + "APP_ADMIN_BOOTSTRAP_PASSWORD de al menos 12 caracteres."
            );
        }

        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> createBootstrapUser());
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalStateException(
                    "La cuenta configurada para el administrador inicial ya existe "
                            + "con otra contraseña. No se modificó su rol."
            );
        }
        if (!user.isActive()) {
            throw new IllegalStateException(
                    "La cuenta configurada para el administrador inicial está inactiva. "
                            + "No se modificó su estado ni su rol."
            );
        }

        user.setRole(UserRole.ADMIN);
        appUserRepository.save(user);
    }

    private AppUser createBootstrapUser() {
        AppUser user = new AppUser();
        user.setName("Administrator");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setActive(true);
        user.setRole(UserRole.ADMIN);
        return appUserRepository.save(user);
    }
}
