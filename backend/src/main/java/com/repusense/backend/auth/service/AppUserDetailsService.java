package com.repusense.backend.auth.service;

import com.repusense.backend.model.AppUser;
import com.repusense.backend.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        AppUser appUser = appUserRepository.findByEmailIgnoreCase(email)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No se encontró un usuario activo."
                ));

        return User.withUsername(appUser.getEmail())
                .password(appUser.getPasswordHash())
                .authorities("USER")
                .build();
    }
}
