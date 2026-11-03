package com.sparesbilling.backend.customer;

import com.sparesbilling.backend.tenant.TenantGuardListener;
import com.sparesbilling.backend.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(TenantGuardListener.class)
public class Customer implements TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 15)
    private String gstin;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "credit_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "opening_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @TenantId
    @Column(name = "shop_id", nullable = false, updatable = false)
    private Long shopId;
}
