package com.sparesbilling.backend.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Tells Hibernate which shop to use for every query and insert on entities marked with @TenantId.
 * Registered in application.yml (spring.jpa.properties.hibernate.tenant_identifier_resolver).
 */
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.forHibernate();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
