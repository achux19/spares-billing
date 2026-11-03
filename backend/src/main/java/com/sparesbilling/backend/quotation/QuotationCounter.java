package com.sparesbilling.backend.quotation;

import com.sparesbilling.backend.tenant.TenantGuardListener;
import com.sparesbilling.backend.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quotation_counters")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(TenantGuardListener.class)
public class QuotationCounter implements TenantOwned {

    @Id
    @Column(name = "financial_year", length = 7)
    private String financialYear;

    @Column(name = "last_number", nullable = false)
    private int lastNumber;

    @TenantId
    @Column(name = "shop_id", nullable = false, updatable = false)
    private Long shopId;
}
