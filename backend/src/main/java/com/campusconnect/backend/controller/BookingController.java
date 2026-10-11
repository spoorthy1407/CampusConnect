package com.campusconnect.backend.controller;

import com.campusconnect.backend.dto.Dtos.BookingRequest;
import com.campusconnect.backend.entity.Booking;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookings;

    public BookingController(BookingService bookings) { this.bookings = bookings; }

    @PostMapping
    public Booking create(@AuthenticationPrincipal User user, @Valid @RequestBody BookingRequest r) {
        return bookings.create(user, r);
    }

    @GetMapping("/my")
    public List<Booking> mine(@AuthenticationPrincipal User user) { return bookings.mine(user); }

    @PutMapping("/{id}/cancel")
    public Booking cancel(@AuthenticationPrincipal User user, @PathVariable Long id) { return bookings.cancel(user, id); }
}
