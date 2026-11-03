package com.sparesbilling.backend.common;

import com.sparesbilling.backend.tenant.AppPrincipal;
import com.sparesbilling.backend.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

//who is logged in right now; used for audit columns and role checks
@Component
public class CurrentUser {

    public Long id() {
        AppPrincipal p = TenantContext.principal();
        return p == null ? null : p.getUserId();
    }

    public boolean isOwner() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_OWNER"));
    }
}
