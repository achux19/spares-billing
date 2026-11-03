package com.sparesbilling.backend.common;

import com.sparesbilling.backend.tenant.Shop;
import com.sparesbilling.backend.tenant.ShopRepository;
import com.sparesbilling.backend.user.Role;
import com.sparesbilling.backend.user.User;
import com.sparesbilling.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-owner.username}")
    private String ownerUsername;

    @Value("${app.initial-owner.password}")
    private String ownerPassword;

    @Value("${app.super-admin.username:}")
    private String superUsername;

    @Value("${app.super-admin.password:}")
    private String superPassword;

    @Override
    public void run(String... args) {
        // a brand new database: the first shop (created by migration V13) gets its owner
        Shop first = shopRepository.findAll().stream().findFirst().orElse(null);
        if (first != null && userRepository.count() == 0) {
            User owner = new User();
            owner.setShopId(first.getId());
            owner.setUsername(ownerUsername);
            owner.setPasswordHash(passwordEncoder.encode(ownerPassword));
            owner.setFullName("Shop Owner");
            owner.setRole(Role.OWNER);
            owner.setMustChangePassword(true);
            userRepository.save(owner);
            log.warn("Created initial OWNER user '{}' for shop '{}'. Change this password immediately!",
                    ownerUsername, first.getCode());
        }

        // the platform Super Admin (no shop), created once from the environment
        if (userRepository.countByRole(Role.SUPER_ADMIN) == 0) {
            if (superUsername.isBlank() || superPassword.isBlank()) {
                log.warn("No Super Admin exists and SUPER_ADMIN_USERNAME / SUPER_ADMIN_PASSWORD are not set.");
                return;
            }
            User su = new User();
            su.setShopId(null);
            su.setUsername(superUsername.trim());
            su.setPasswordHash(passwordEncoder.encode(superPassword));
            su.setFullName("Platform Admin");
            su.setRole(Role.SUPER_ADMIN);
            su.setMustChangePassword(true);
            userRepository.save(su);
            log.warn("Created Super Admin '{}'. Change this password immediately!", superUsername);
        }
    }
}
