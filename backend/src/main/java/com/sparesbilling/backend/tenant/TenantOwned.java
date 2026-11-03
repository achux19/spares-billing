package com.sparesbilling.backend.tenant;

/** Implemented by every entity whose rows belong to one shop. */
public interface TenantOwned {
    Long getShopId();
}
