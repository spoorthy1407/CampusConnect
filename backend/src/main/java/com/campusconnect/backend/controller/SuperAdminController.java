package com.campusconnect.backend.controller;

import com.campusconnect.backend.dto.Dtos.AdminCreateRequest;
import com.campusconnect.backend.dto.Dtos.AdminInfo;
import com.campusconnect.backend.dto.Dtos.FacilityAccessRequest;
import com.campusconnect.backend.dto.Dtos.FacilityRequest;
import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.entity.Listing;
import com.campusconnect.backend.service.AdminUserService;
import com.campusconnect.backend.service.FacilityService;
import com.campusconnect.backend.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/super")
public class SuperAdminController {
    private final FacilityService facilities;
    private final AdminUserService adminUsers;
    private final ListingService listings;

    public SuperAdminController(FacilityService facilities, AdminUserService adminUsers, ListingService listings) {
        this.facilities = facilities; this.adminUsers = adminUsers; this.listings = listings;
    }

    @PostMapping("/facilities")
    public Facility createFacility(@Valid @RequestBody FacilityRequest r) { return facilities.create(r); }

    @PutMapping("/facilities/{id}")
    public Facility updateFacility(@PathVariable Long id, @Valid @RequestBody FacilityRequest r) {
        return facilities.update(id, r);
    }

    @GetMapping("/admins")
    public List<AdminInfo> admins() { return adminUsers.list(); }

    @PostMapping("/admins")
    public AdminInfo createAdmin(@Valid @RequestBody AdminCreateRequest r) { return adminUsers.create(r); }

    @PutMapping("/admins/{id}/facilities")
    public AdminInfo setFacilities(@PathVariable Long id, @Valid @RequestBody FacilityAccessRequest r) {
        return adminUsers.setFacilities(id, r.facilityIds());
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<Void> removeAdmin(@PathVariable Long id) {
        adminUsers.remove(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/listings")
    public List<Listing> allListings() { return listings.all(); }
}
