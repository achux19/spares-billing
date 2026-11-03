package com.sparesbilling.backend.eway;

import com.sparesbilling.backend.tenant.TenantGuardListener;
import com.sparesbilling.backend.tenant.TenantOwned;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "eway_details")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(TenantGuardListener.class)
public class EwayDetails implements TenantOwned {

    @Id
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "to_pincode", length = 6)
    private String toPincode;

    @Column(name = "to_place", length = 100)
    private String toPlace;

    @Column(name = "distance_km")
    private Integer distanceKm;

    @Column(name = "trans_mode", nullable = false)
    private short transMode = 1;

    @Column(name = "vehicle_no", length = 15)
    private String vehicleNo;

    @Column(name = "transporter_name", length = 100)
    private String transporterName;

    @Column(name = "transporter_id", length = 15)
    private String transporterId;

    @Column(name = "trans_doc_no", length = 30)
    private String transDocNo;

    @Column(name = "trans_doc_date")
    private LocalDate transDocDate;

    @Column(name = "eway_bill_no", length = 12)
    private String ewayBillNo;

    @Column(name = "eway_bill_date")
    private LocalDate ewayBillDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @TenantId
    @Column(name = "shop_id", nullable = false, updatable = false)
    private Long shopId;
}
