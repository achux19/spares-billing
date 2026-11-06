package com.sparesbilling.backend.feature;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopFeatureRepository extends JpaRepository<ShopFeature, ShopFeatureId> {
    List<ShopFeature> findByShopId(Long shopId);
}
