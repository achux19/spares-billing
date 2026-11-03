package com.sparesbilling.backend.tenant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Which shop is the current request working for? Always taken from the verified login token. */
public final class TenantContext {

    /** Used when nobody is logged in, or for the platform Super Admin. Matches no shop's data. */
    public static final long NO_TENANT = -1L;

    private TenantContext() {}

    public static AppPrincipal principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AppPrincipal p) return p;
        return null;
    }

    /** The shop of the logged-in user, or null (not logged in, or Super Admin). */
    public static Long currentShopId() {
        AppPrincipal p = principal();
        return p == null ? null : p.getShopId();
    }

    public static boolean hasTenant() {
        return currentShopId() != null;
    }

    /** For code that must act inside a shop. Fails loudly instead of guessing. */
    public static long requireShopId() {
        Long id = currentShopId();
        if (id == null) throw new IllegalStateException("No shop in the current request");
        return id;
    }

    /** What Hibernate filters on. */
    static long forHibernate() {
        Long id = currentShopId();
        return id == null ? NO_TENANT : id;
    }
}
