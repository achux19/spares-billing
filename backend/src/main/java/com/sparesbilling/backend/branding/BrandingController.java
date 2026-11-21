package com.sparesbilling.backend.branding;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/branding")
@RequiredArgsConstructor
public class BrandingController {

    private final BrandingService service;

    @GetMapping
    public BrandingResponse get() {
        return BrandingResponse.from(service.find());
    }

    @GetMapping("/favicon")
    public ResponseEntity<byte[]> favicon() {
        Branding b = service.find();
        if (b.getFaviconData() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(b.getFaviconType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(b.getFaviconData());
    }

    /** Needs a login (it is the shop's own logo). The app fetches it once and keeps it for printing. */
    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        Branding b = service.find();
        if (b.getLogoData() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(b.getLogoType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(b.getLogoData());
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public BrandingResponse uploadLogo(@RequestParam("file") MultipartFile file) {
        return service.setLogo(file);
    }

    @PutMapping("/logo-size")
    @PreAuthorize("hasRole('OWNER')")
    public BrandingResponse logoSize(@Valid @RequestBody LogoSizeRequest req) {
        return service.setLogoSize(req.heightMm(), req.widthMm());
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('OWNER')")
    public BrandingResponse clearLogo() {
        return service.clearLogo();
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public BrandingResponse rename(@Valid @RequestBody BrandingRequest req) {
        return service.rename(req.appName());
    }

    @PostMapping(value = "/favicon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public BrandingResponse upload(@RequestParam("file") MultipartFile file) {
        return service.setFavicon(file);
    }

    @DeleteMapping("/favicon")
    @PreAuthorize("hasRole('OWNER')")
    public BrandingResponse clear() {
        return service.clearFavicon();
    }
}