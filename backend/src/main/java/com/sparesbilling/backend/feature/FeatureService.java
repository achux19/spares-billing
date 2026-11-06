package com.sparesbilling.backend.feature;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeatureService {

    private final ShopFeatureRepository repository;

    /** A feature with no row for the shop counts as OFF. */
    @Transactional(readOnly = true)
    public Set<Feature> enabledFor(Long shopId) {
        Set<Feature> on = EnumSet.noneOf(Feature.class);
        for (ShopFeature f : repository.findByShopId(shopId)) {
            if (!f.isEnabled()) continue;
            try {
                on.add(Feature.valueOf(f.getFeatureKey()));
            } catch (IllegalArgumentException ignored) {
                // a key from a newer or older version: not a known feature, ignore it
            }
        }
        return on;
    }

    /** Sets exactly these features on and all others off. */
    @Transactional
    public void setFeatures(Long shopId, Set<Feature> wanted) {
        Map<String, ShopFeature> existing = new HashMap<>();
        for (ShopFeature f : repository.findByShopId(shopId)) existing.put(f.getFeatureKey(), f);
        for (Feature feature : Feature.values()) {
            ShopFeature row = existing.get(feature.name());
            if (row == null) {
                row = new ShopFeature();
                row.setShopId(shopId);
                row.setFeatureKey(feature.name());
            }
            row.setEnabled(wanted.contains(feature));
            repository.save(row);
        }
    }
}
