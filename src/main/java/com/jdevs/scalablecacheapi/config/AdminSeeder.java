package com.jdevs.scalablecacheapi.config;

import com.jdevs.scalablecacheapi.user.AppUser;
import com.jdevs.scalablecacheapi.user.AppUserRepository;
import com.jdevs.scalablecacheapi.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (appUserRepository.existsByEmail(adminEmail)) {
            return;
        }

        AppUser admin = AppUser.builder()
                .fullName("System Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        appUserRepository.save(admin);
    }
}
