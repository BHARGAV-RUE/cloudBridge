package com.bhargav.cloudbridge.config;

import com.bhargav.cloudbridge.entity.Admin;
import com.bhargav.cloudbridge.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2) // runs after StorageCapacityInitializer (Order 1)
public class AdminDataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${cloudbridge.admin.username}")
    private String adminUsername;

    @Value("${cloudbridge.admin.password}")
    private String adminPassword;

    @Value("${cloudbridge.admin.email}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        if (adminRepository.existsByUsername(adminUsername)) {
            log.info("[AdminInitializer] Admin already exists — skipping.");
            return;
        }

        Admin admin = Admin.builder()
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .email(adminEmail)
                .isActive(true)
                .build();

        adminRepository.save(admin);
        log.info("[AdminInitializer] Admin account created: {}", adminUsername);
    }
}