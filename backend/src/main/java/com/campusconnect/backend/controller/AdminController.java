package com.campusconnect.backend.controller;

import com.campusconnect.backend.dto.Dtos.AdminScope;
import com.campusconnect.backend.dto.Dtos.RemarkRequest;
import com.campusconnect.backend.dto.Dtos.ServiceRequest;
import com.campusconnect.backend.entity.Booking;
import com.campusconnect.backend.entity.CampusService;
import com.campusconnect.backend.entity.PrintJob;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.mail.Notifier;
import com.campusconnect.backend.service.AnalyticsService;
import com.campusconnect.backend.service.BookingService;
import com.campusconnect.backend.service.CatalogService;
import com.campusconnect.backend.service.FacilityService;
import com.campusconnect.backend.service.PrintService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final BookingService bookings;
    private final PrintService prints;
    private final CatalogService catalog;
    private final FacilityService facilities;
    private final AnalyticsService analytics;
    private final Notifier notifier;

    public AdminController(BookingService bookings, PrintService prints, CatalogService catalog,
                           FacilityService facilities, AnalyticsService analytics, Notifier notifier) {
        this.bookings = bookings; this.prints = prints; this.catalog = catalog;
        this.facilities = facilities; this.analytics = analytics; this.notifier = notifier;
    }

    @GetMapping("/scope")
    public AdminScope scope(@AuthenticationPrincipal User admin) {
        return new AdminScope(admin.role.name(), facilities.managedBy(admin));
    }

    @GetMapping("/services")
    public List<CampusService> services(@AuthenticationPrincipal User admin) { return catalog.forAdmin(admin); }

    @PostMapping("/services")
    public CampusService createService(@AuthenticationPrincipal User admin, @Valid @RequestBody ServiceRequest r) {
        return catalog.create(admin, r);
    }

    @PutMapping("/services/{id}")
    public CampusService updateService(@AuthenticationPrincipal User admin, @PathVariable Long id,
                                       @Valid @RequestBody ServiceRequest r) {
        return catalog.update(admin, id, r);
    }

    @GetMapping("/bookings")
    public List<Booking> allBookings(@AuthenticationPrincipal User admin) { return bookings.forAdmin(admin); }

    @PutMapping("/bookings/{id}/approve")
    public Booking approve(@AuthenticationPrincipal User admin, @PathVariable Long id) {
        Booking b = bookings.approve(admin, id);
        notifier.bookingApproved(b, admin);
        return b;
    }

    @PutMapping("/bookings/{id}/reject")
    public Booking reject(@AuthenticationPrincipal User admin, @PathVariable Long id,
                          @RequestBody(required = false) RemarkRequest r) {
        Booking b = bookings.reject(admin, id, r == null ? null : r.remarks());
        notifier.bookingRejected(b, admin);
        return b;
    }

    @PutMapping("/bookings/{id}/complete")
    public Booking complete(@AuthenticationPrincipal User admin, @PathVariable Long id) {
        return bookings.complete(admin, id);
    }

    @GetMapping("/print-jobs")
    public List<PrintJob> printJobs(@AuthenticationPrincipal User admin) { return prints.forAdmin(admin); }

    @PutMapping("/print-jobs/{id}/start")
    public PrintJob start(@AuthenticationPrincipal User admin, @PathVariable Long id) { return prints.start(admin, id); }

    @PutMapping("/print-jobs/{id}/ready")
    public PrintJob ready(@AuthenticationPrincipal User admin, @PathVariable Long id) {
        PrintJob j = prints.ready(admin, id);
        notifier.printReady(j);
        return j;
    }

    @PutMapping("/print-jobs/{id}/collected")
    public PrintJob collected(@AuthenticationPrincipal User admin, @PathVariable Long id) {
        return prints.collected(admin, id);
    }

    @PutMapping("/print-jobs/{id}/cancel")
    public PrintJob cancelJob(@AuthenticationPrincipal User admin, @PathVariable Long id,
                              @RequestBody(required = false) RemarkRequest r) {
        PrintJob j = prints.cancelByAdmin(admin, id, r == null ? null : r.remarks());
        notifier.printCancelled(j);
        return j;
    }

    @PutMapping("/print-jobs/{id}/refunded")
    public PrintJob refunded(@AuthenticationPrincipal User admin, @PathVariable Long id) {
        return prints.refunded(admin, id);
    }

    @GetMapping("/print-jobs/{jobId}/files/{fileId}")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal User admin, @PathVariable Long jobId,
                                             @PathVariable Long fileId) {
        PrintService.FileDownload d = prints.download(admin, jobId, fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.name(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(d.contentType()))
                .body(d.resource());
    }

    @GetMapping("/analytics")
    public Map<String, Object> analytics(@AuthenticationPrincipal User admin) { return analytics.analytics(admin); }
}
