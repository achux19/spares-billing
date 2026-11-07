package com.sparesbilling.backend.enquiry;

import com.sparesbilling.backend.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enquiries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class EnquiryController {

    private final EnquiryService enquiryService;

    @GetMapping
    public PageResponse<EnquirySummary> search(@RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return enquiryService.search(q, page, size);
    }

    @GetMapping("/{id}")
    public EnquiryResponse get(@PathVariable Long id) {
        return enquiryService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnquiryResponse create(@Valid @RequestBody EnquiryRequest req) {
        return enquiryService.create(req);
    }

    @PutMapping("/{id}")
    public EnquiryResponse update(@PathVariable Long id, @Valid @RequestBody EnquiryRequest req) {
        return enquiryService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        enquiryService.delete(id);
    }
}
