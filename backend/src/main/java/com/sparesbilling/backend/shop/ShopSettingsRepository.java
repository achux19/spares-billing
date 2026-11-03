package com.sparesbilling.backend.shop;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopSettingsRepository extends JpaRepository<ShopSettings, Integer> {
    /** The logged-in shop's settings row (the shop filter leaves only one row). */
    java.util.Optional<ShopSettings> findFirstByOrderByIdAsc();
}