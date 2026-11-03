package com.sparesbilling.backend.security;

import com.sparesbilling.backend.tenant.AppPrincipal;
import com.sparesbilling.backend.tenant.Shop;
import com.sparesbilling.backend.tenant.ShopRepository;
import com.sparesbilling.backend.tenant.ShopStatus;
import com.sparesbilling.backend.user.User;
import com.sparesbilling.backend.user.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Turns a valid token into a logged-in user. The database is the truth: the user must still exist,
 * be active, still be in the same shop, and the shop must not be suspended.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtService.parseOrNull(header.substring(7));
            if (claims != null) {
                AppPrincipal principal = load(claims);
                if (principal != null) {
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }

    private AppPrincipal load(Claims claims) {
        Long uid = ((Number) claims.get("uid")).longValue();
        Long sid = claims.get("sid") == null ? null : ((Number) claims.get("sid")).longValue();

        User u = userRepository.findById(uid).orElse(null);
        if (u == null || !u.isActive()) return null;
        if (!Objects.equals(u.getShopId(), sid)) return null;           // token and database disagree

        if (u.getShopId() != null) {
            Shop shop = shopRepository.findById(u.getShopId()).orElse(null);
            if (shop == null || shop.getStatus() != ShopStatus.ACTIVE) return null;
        }
        return new AppPrincipal(u.getId(), u.getShopId(), u.getUsername(), u.getRole().name());
    }
}
