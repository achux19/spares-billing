package com.sparesbilling.backend.enquiry;

import java.time.LocalDate;

public record EnquirySummary(Long id, String enquiryNo, LocalDate enquiryDate,
                             String supplierName, int itemCount) {
    public static EnquirySummary from(Enquiry e) {
        return new EnquirySummary(e.getId(), e.getEnquiryNo(), e.getEnquiryDate(),
                e.getSupplierName(), e.getItems().size());
    }
}
