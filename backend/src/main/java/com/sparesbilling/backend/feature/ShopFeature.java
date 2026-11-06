package com.sparesbilling.backend.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One on/off switch for one shop. Not shop-filtered: read by the feature check and by Super Admin. */
@Entity
@Table(name = "shop_features")
@IdClass(ShopFeatureId.class)
@Getter
@Setter
@NoArgsConstructor
public class ShopFeature {

    @Id
    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Id
    @Column(name = "feature_key", nullable = false, length = 40)
    private String featureKey;

    @Column(nullable = false)
    private boolean enabled = true;
}
