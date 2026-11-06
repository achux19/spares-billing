package com.sparesbilling.backend.feature;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ShopFeatureId implements Serializable {
    private Long shopId;
    private String featureKey;

    public ShopFeatureId(Long shopId, String featureKey) {
        this.shopId = shopId;
        this.featureKey = featureKey;
    }
}
