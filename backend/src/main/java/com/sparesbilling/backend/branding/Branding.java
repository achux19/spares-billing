package com.sparesbilling.backend.branding;

import com.sparesbilling.backend.tenant.TenantGuardListener;
import com.sparesbilling.backend.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "branding")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(TenantGuardListener.class)
public class Branding implements TenantOwned {

    @Id
    private Integer id = 1;

    @Column(name = "app_name", nullable = false, length = 60)
    private String appName;

    @Column(name = "favicon_data", columnDefinition = "bytea")
    private byte[] faviconData;

    @Column(name = "favicon_type", length = 50)
    private String faviconType;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @TenantId
    @Column(name = "shop_id", nullable = false, updatable = false)
    private Long shopId;
}
