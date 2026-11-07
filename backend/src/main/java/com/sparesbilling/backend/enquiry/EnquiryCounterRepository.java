package com.sparesbilling.backend.enquiry;

import com.sparesbilling.backend.tenant.CounterId;
import com.sparesbilling.backend.tenant.TenantContext;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EnquiryCounterRepository extends JpaRepository<EnquiryCounter, CounterId> {

    @Modifying
    @Query(value = "INSERT INTO enquiry_counters (shop_id, financial_year, last_number) VALUES (:shopId, :fy, 0) "
            + "ON CONFLICT (shop_id, financial_year) DO NOTHING", nativeQuery = true)
    void ensureExistsForShop(@Param("shopId") Long shopId, @Param("fy") String financialYear);

    default void ensureExists(String financialYear) {
        ensureExistsForShop(TenantContext.requireShopId(), financialYear);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EnquiryCounter c where c.shopId = :shopId and c.financialYear = :fy")
    Optional<EnquiryCounter> findForUpdate(@Param("shopId") Long shopId, @Param("fy") String financialYear);

    default Optional<EnquiryCounter> findByIdForUpdate(String financialYear) {
        return findForUpdate(TenantContext.requireShopId(), financialYear);
    }
}
