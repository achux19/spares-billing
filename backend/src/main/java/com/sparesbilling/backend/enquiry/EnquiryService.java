package com.sparesbilling.backend.enquiry;

import com.sparesbilling.backend.common.*;
import com.sparesbilling.backend.supplier.Supplier;
import com.sparesbilling.backend.supplier.SupplierRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnquiryService {

    private static final ZoneId IST = ZoneId.of("Asia/Calcutta");

    private final EnquiryRepository enquiryRepository;
    private final EnquiryCounterRepository counterRepository;
    private final SupplierRepository supplierRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public PageResponse<EnquirySummary> search(String q, int page, int size) {
        Specification<Enquiry> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase().replace("\\", "\\\\")
                        .replace("%", "\\%").replace("_", "\\_") + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.<String>get("enquiryNo")), like, '\\'),
                        cb.like(cb.lower(root.<String>get("supplierName")), like, '\\')));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("id").descending());
        return PageResponse.from(enquiryRepository.findAll(spec, pageable).map(EnquirySummary::from));
    }

    @Transactional(readOnly = true)
    public EnquiryResponse get(Long id) {
        return EnquiryResponse.from(find(id));
    }

    @Transactional
    public EnquiryResponse create(EnquiryRequest req) {
        Enquiry e = new Enquiry();
        apply(e, req);
        e.setCreatedBy(currentUser.id());
        e.setEnquiryNo(nextNo(e.getEnquiryDate()));
        return EnquiryResponse.from(enquiryRepository.saveAndFlush(e));
    }

    @Transactional
    public EnquiryResponse update(Long id, EnquiryRequest req) {
        Enquiry e = find(id);
        apply(e, req);
        e.setUpdatedAt(LocalDateTime.now());
        return EnquiryResponse.from(enquiryRepository.saveAndFlush(e));
    }

    @Transactional
    public void delete(Long id) {
        enquiryRepository.delete(find(id));
    }

    private Enquiry find(Long id) {
        return enquiryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Enquiry not found"));
    }

    private void apply(Enquiry e, EnquiryRequest req) {
        e.setEnquiryDate(req.enquiryDate() == null ? LocalDate.now(IST) : req.enquiryDate());
        if (req.supplierId() != null) {
            Supplier s = supplierRepository.findById(req.supplierId())
                    .orElseThrow(() -> new NotFoundException("Supplier not found"));
            e.setSupplierId(s.getId());
            e.setSupplierName(s.getName());
        } else {
            e.setSupplierId(null);
            String n = req.supplierName();
            e.setSupplierName(n == null || n.isBlank() ? null : n.trim());
        }
        e.setNotes(req.notes() == null || req.notes().isBlank() ? null : req.notes().trim());

        e.getItems().clear();
        int line = 1;
        for (EnquiryRequest.Item it : req.items()) {
            EnquiryItem i = new EnquiryItem();
            i.setLineNo(line++);
            i.setProductId(it.productId());
            i.setPartNumber(it.partNumber() == null || it.partNumber().isBlank() ? null : it.partNumber().trim());
            i.setDescription(it.description().trim());
            i.setQuantity(it.quantity());
            i.setUnit(it.unit() == null ? "NOS" : it.unit());
            e.getItems().add(i);
        }
    }

    private String nextNo(LocalDate date) {
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        String fy = startYear + "-" + String.format("%02d", (startYear + 1) % 100);
        String shortFy = String.format("%02d-%02d", startYear % 100, (startYear + 1) % 100);

        counterRepository.ensureExists(fy);
        EnquiryCounter counter = counterRepository.findByIdForUpdate(fy)
                .orElseThrow(() -> new IllegalStateException("Enquiry counter missing"));
        counter.setLastNumber(counter.getLastNumber() + 1);
        return String.format("ENQ/%s/%05d", shortFy, counter.getLastNumber());
    }
}
