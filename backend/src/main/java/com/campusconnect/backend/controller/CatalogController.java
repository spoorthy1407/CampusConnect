package com.campusconnect.backend.controller;

import com.campusconnect.backend.dto.Dtos.ResourceAvailability;
import com.campusconnect.backend.entity.CampusService;
import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.service.BookingService;
import com.campusconnect.backend.service.CatalogService;
import com.campusconnect.backend.service.FacilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;
    private final FacilityService facilities;
    private final BookingService bookings;

    public CatalogController(CatalogService catalog, FacilityService facilities, BookingService bookings) {
        this.catalog = catalog; this.facilities = facilities; this.bookings = bookings;
    }

    @GetMapping("/facilities")
    public List<Facility> facilities() { return facilities.active(); }

    @GetMapping("/services")
    public List<CampusService> services(@RequestParam(required = false) Long facilityId) { return catalog.all(facilityId); }

    @GetMapping("/services/{id}/slots")
    public List<Map<String, Object>> slots(@PathVariable Long id) { return bookings.slots(id); }

    @GetMapping("/facilities/{id}/availability")
    public List<ResourceAvailability> availability(@PathVariable Long id,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return bookings.availability(id, start, end);
    }
}
