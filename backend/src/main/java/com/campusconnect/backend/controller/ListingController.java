package com.campusconnect.backend.controller;

import com.campusconnect.backend.entity.Listing;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.service.ListingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class ListingController {
    private final ListingService listings;

    public ListingController(ListingService listings) { this.listings = listings; }

    @GetMapping
    public List<Listing> browse(@RequestParam(required = false) String category,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) BigDecimal maxPrice) {
        return listings.browse(category, q, maxPrice);
    }

    @GetMapping("/my")
    public List<Listing> mine(@AuthenticationPrincipal User user) { return listings.mine(user); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Listing create(@AuthenticationPrincipal User user,
                          @RequestParam String title,
                          @RequestParam String category,
                          @RequestParam BigDecimal price,
                          @RequestParam(required = false) MultipartFile image) throws IOException {
        return listings.create(user, title, category, price, image);
    }

    @PutMapping("/{id}/sold")
    public Listing sold(@AuthenticationPrincipal User user, @PathVariable Long id) { return listings.markSold(user, id); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        listings.remove(user, id);
        return ResponseEntity.noContent().build();
    }
}
