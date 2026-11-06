package com.sparesbilling.backend.admin;

import com.sparesbilling.backend.admin.AdminDtos.*;
import com.sparesbilling.backend.tenant.ShopStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The platform Super Admin's API. Shops are never deleted here, only suspended. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminShopController {

    private final AdminShopService service;

    @GetMapping("/shops")
    public List<ShopRow> list() { return service.list(); }

    @GetMapping("/features")
    public List<FeatureInfo> features() { return service.featureCatalog(); }

    @PostMapping("/shops")
    @ResponseStatus(HttpStatus.CREATED)
    public ShopRow create(@Valid @RequestBody CreateShopRequest req) { return service.create(req); }

    @PutMapping("/shops/{id}")
    public ShopRow update(@PathVariable Long id, @Valid @RequestBody UpdateShopRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/shops/{id}/suspend")
    public ShopRow suspend(@PathVariable Long id) { return service.setStatus(id, ShopStatus.SUSPENDED); }

    @PostMapping("/shops/{id}/activate")
    public ShopRow activate(@PathVariable Long id) { return service.setStatus(id, ShopStatus.ACTIVE); }

    @PutMapping("/shops/{id}/features")
    public ShopRow setFeatures(@PathVariable Long id, @Valid @RequestBody FeaturesRequest req) {
        return service.setFeatures(id, req.features());
    }

    @GetMapping("/shops/{id}/users")
    public List<ShopUserRow> users(@PathVariable Long id) { return service.users(id); }

    @PostMapping("/shops/{shopId}/users/{userId}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long shopId, @PathVariable Long userId,
                              @Valid @RequestBody ResetPasswordRequest req) {
        service.resetPassword(shopId, userId, req.newPassword());
    }
}
