package com.sparesbilling.backend.user;

import com.sparesbilling.backend.common.NotFoundException;
import com.sparesbilling.backend.tenant.AppPrincipal;
import com.sparesbilling.backend.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public List<UserResponse> list() {
        return userRepository.findAllByShopId(TenantContext.requireShopId(), Sort.by("username")).stream().map(UserResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
        String username = req.username().trim();
        if (req.role() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("This role cannot be given to a shop user");
        }
        long shopId = TenantContext.requireShopId();
        if (userRepository.existsByShopIdAndUsername(shopId, username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        User u = new User();
        u.setShopId(shopId);
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setFullName(req.fullName().trim());
        u.setRole(req.role());
        u.setMustChangePassword(true);
        return UserResponse.from(userRepository.save(u));
    }

    @PutMapping("/{id}")
    @Transactional
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req,
                               Authentication auth) {
        if (req.role() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("This role cannot be given to a shop user");
        }
        User u = find(id);
        boolean self = ((AppPrincipal) auth.getPrincipal()).getUserId().equals(u.getId());
        boolean staysActiveOwner = req.active() && req.role() == Role.OWNER;
        if (self && !staysActiveOwner) {
            throw new IllegalArgumentException("You cannot deactivate yourself or remove your own Owner role");
        }
        boolean wasActiveOwner = u.getRole() == Role.OWNER && u.isActive();
        if (wasActiveOwner && !staysActiveOwner
                && userRepository.countByShopIdAndRoleAndActive(TenantContext.requireShopId(), Role.OWNER, true) <= 1) {
            throw new IllegalArgumentException("The shop must keep at least one active Owner");
        }
        u.setFullName(req.fullName().trim());
        u.setRole(req.role());
        u.setActive(req.active());
        return UserResponse.from(u);
    }

    @PostMapping("/{id}/reset-password")
    @Transactional
    public UserResponse resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        User u = find(id);
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        u.setMustChangePassword(true);
        return UserResponse.from(u);
    }

    private User find(Long id) {
        return userRepository.findByIdAndShopId(id, TenantContext.requireShopId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}