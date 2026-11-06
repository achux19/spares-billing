package com.sparesbilling.backend.feature;

import com.sparesbilling.backend.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Which modules the logged-in shop has. The screens use this to hide menu entries. */
@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;

    @GetMapping
    public List<String> mine() {
        Long shopId = TenantContext.currentShopId();
        if (shopId == null) return List.of();
        return featureService.enabledFor(shopId).stream().map(Enum::name).sorted().toList();
    }
}
