package com.sparesbilling.backend.enquiry;

import java.time.LocalDate;
import java.util.List;

public record EnquiryResponse(
        Long id, String enquiryNo, LocalDate enquiryDate,
        Long supplierId, String supplierName, String notes,
        List<Line> items
) {
    public record Line(Long productId, String partNumber, String description, int quantity, String unit) {}

    public static EnquiryResponse from(Enquiry e) {
        return new EnquiryResponse(e.getId(), e.getEnquiryNo(), e.getEnquiryDate(),
                e.getSupplierId(), e.getSupplierName(), e.getNotes(),
                e.getItems().stream()
                        .map(i -> new Line(i.getProductId(), i.getPartNumber(), i.getDescription(),
                                i.getQuantity(), i.getUnit()))
                        .toList());
    }
}
