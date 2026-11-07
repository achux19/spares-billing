package com.sparesbilling.backend.enquiry;

import com.sparesbilling.backend.tenant.TenantGuardListener;
import com.sparesbilling.backend.tenant.TenantOwned;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "purchase_enquiry_items")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(TenantGuardListener.class)
public class EnquiryItem implements TenantOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "part_number", length = 60)
    private String partNumber;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 10)
    private String unit = "NOS";

    @TenantId
    @Column(name = "shop_id", nullable = false, updatable = false)
    private Long shopId;
}
