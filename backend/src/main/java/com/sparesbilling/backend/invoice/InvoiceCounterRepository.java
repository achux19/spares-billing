package com.sparesbilling.backend.invoice;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounter, com.sparesbilling.backend.tenant.CounterId> {

    @Modifying
    @Query(value = "INSERT INTO invoice_counters (shop_id, financial_year, last_number) VALUES (:shopId, :fy, 0) "
            + "ON CONFLICT (shop_id, financial_year) DO NOTHING", nativeQuery = true)
    void ensureExistsForShop(@Param("shopId") Long shopId, @Param("fy") String financialYear);

    /** Creates this year's counter for the logged-in shop if it is missing. */
    default void ensureExists(String financialYear) {
        ensureExistsForShop(com.sparesbilling.backend.tenant.TenantContext.requireShopId(), financialYear);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from InvoiceCounter c where c.shopId = :shopId and c.financialYear = :fy")
    Optional<InvoiceCounter> findForUpdate(@Param("shopId") Long shopId, @Param("fy") String financialYear);

    /** This year's counter of the logged-in shop, locked until the transaction ends. */
    default Optional<InvoiceCounter> findByIdForUpdate(String financialYear) {
        return findForUpdate(com.sparesbilling.backend.tenant.TenantContext.requireShopId(), financialYear);
    }
}
