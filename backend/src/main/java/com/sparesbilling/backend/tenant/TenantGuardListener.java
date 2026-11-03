package com.sparesbilling.backend.tenant;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Last line of defence. Hibernate already filters by shop; this makes sure that even if a
 * row of another shop were ever loaded or saved, the request fails instead of leaking data.
 */
public class TenantGuardListener {

    @PostLoad
    @PrePersist
    @PreUpdate
    public void check(Object entity) {
        if (!(entity instanceof TenantOwned t)) return;
        Long mine = TenantContext.currentShopId();
        if (mine == null) {
            // nobody / Super Admin: allowed only while the row has no shop yet (a brand new insert is refused by the database)
            if (t.getShopId() != null) {
                throw new IllegalStateException("Tenant guard: shop data touched outside a shop");
            }
            return;
        }
        if (t.getShopId() != null && !t.getShopId().equals(mine)) {
            throw new IllegalStateException("Tenant guard: row belongs to another shop");
        }
    }
}
