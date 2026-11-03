package com.sparesbilling.backend.quotation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuotationCounterRepository extends JpaRepository<QuotationCounter, String> {

    @Modifying
    @Query(value = "INSERT INTO quotation_counters (shop_id, financial_year, last_number) VALUES (:shopId, :fy, 0) "
            + "ON CONFLICT (shop_id, financial_year) DO NOTHING", nativeQuery = true)
    void ensureExistsForShop(@Param("shopId") Long shopId, @Param("fy") String financialYear);

    /** Creates this year's counter for the logged-in shop if it is missing. */
    default void ensureExists(String financialYear) {
        ensureExistsForShop(com.sparesbilling.backend.tenant.TenantContext.requireShopId(), financialYear);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from QuotationCounter c where c.financialYear = :fy")
    Optional<QuotationCounter> findByIdForUpdate(@Param("fy") String financialYear);
}