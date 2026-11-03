package com.sparesbilling.backend.purchase;

import com.sparesbilling.backend.tenant.TenantGuardListener;
import com.sparesbilling.backend.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import org.hibernate.annotations.TenantId;

import com.sparesbilling.backend.common.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_payments")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(TenantGuardListener.class)
public class PurchasePayment implements TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_id", nullable = false)
    private Long purchaseId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentMethod method;

    @Column(length = 100)
    private String reference;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @TenantId
    @Column(name = "shop_id", nullable = false, updatable = false)
    private Long shopId;
}
