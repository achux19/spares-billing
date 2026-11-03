package com.sparesbilling.backend.auth;

import com.sparesbilling.backend.security.JwtService;
import com.sparesbilling.backend.tenant.AppPrincipal;
import com.sparesbilling.backend.tenant.Shop;
import com.sparesbilling.backend.tenant.ShopRepository;
import com.sparesbilling.backend.tenant.ShopStatus;
import com.sparesbilling.backend.user.User;
import com.sparesbilling.backend.user.UserRepository;
import com.sparesbilling.backend.user.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /** A real hash, used to keep the response time the same when the shop or user does not exist. */
    private static final String DUMMY_HASH =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("not-a-real-password");

    @PostMapping("/login")
    @Transactional
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        String code = req.shopCode() == null ? "" : req.shopCode().trim().toLowerCase();
        String username = req.username().trim();

        Shop shop = null;
        User u;
        if (code.isEmpty()) {
            u = userRepository.findByShopIdIsNullAndUsername(username).orElse(null);   // Super Admin
        } else {
            shop = shopRepository.findByCode(code).orElse(null);
            u = shop == null ? null : userRepository.findByShopIdAndUsername(shop.getId(), username).orElse(null);
        }

        // same work whether or not the account exists, and the same message for every failure
        boolean passwordOk = passwordEncoder.matches(req.password(), u == null ? DUMMY_HASH : u.getPasswordHash());
        if (u == null || !passwordOk || !u.isActive()) {
            throw new BadCredentialsException("Invalid shop code, username or password");
        }
        if (shop != null && shop.getStatus() != ShopStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This shop is suspended. Please contact support.");
        }

        u.setLastLoginAt(LocalDateTime.now());
        String token = jwtService.generateToken(u.getId(), u.getShopId(), u.getUsername(), u.getRole().name());
        return new LoginResponse(token, u.getUsername(), u.getFullName(), u.getRole().name(),
                u.isMustChangePassword(), shop == null ? null : shop.getCode(), shop == null ? null : shop.getName());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return UserResponse.from(current(auth));
    }

    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<Void> changePassword(Authentication auth,
                                               @Valid @RequestBody ChangePasswordRequest req) {
        User u = current(auth);
        if (!passwordEncoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(req.newPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current one");
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        u.setMustChangePassword(false);
        userRepository.save(u);
        return ResponseEntity.noContent().build();
    }

    private User current(Authentication auth) {
        AppPrincipal p = (AppPrincipal) auth.getPrincipal();
        return userRepository.findById(p.getUserId()).orElseThrow();
    }
}
