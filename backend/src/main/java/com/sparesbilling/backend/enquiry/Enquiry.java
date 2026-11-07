package com.sparesbilling.backend.enquiry;

import com.sparesbilling.backend.tenant.TenantGuardListener;
import com.sparesbilling.backend.tenant.TenantOwned;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_enquiries")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(TenantGuardListener.class)
public class Enquiry implements TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enquiry_no", nullable = false, length = 30)
    private String enquiryNo;

    @Column(name = "enquiry_date", nullable = false)
    private LocalDate enquiryDate;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name", length = 150)
    private String supplierName;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "enquiry_id", nullable = false)
    @OrderBy("lineNo ASC")
    private List<EnquiryItem> items = new ArrayList<>();

    @TenantId
    @Column(name = "shop_id", nullable = false, updatable = false)
    private Long shopId;
}
