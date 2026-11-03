package com.sparesbilling.backend.tenant;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** The logged-in user as the rest of the app sees it: who, which shop, which role. */
public class AppPrincipal implements UserDetails {

    private final Long userId;
    private final Long shopId;      // null for the platform Super Admin
    private final String username;
    private final String role;

    public AppPrincipal(Long userId, Long shopId, String username, String role) {
        this.userId = userId;
        this.shopId = shopId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public Long getShopId() { return shopId; }
    public String getRole() { return role; }

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return null; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
