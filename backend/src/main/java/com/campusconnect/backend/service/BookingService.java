package com.campusconnect.backend.service;

import com.campusconnect.backend.dto.Dtos.BookingRequest;
import com.campusconnect.backend.dto.Dtos.ResourceAvailability;
import com.campusconnect.backend.entity.Booking;
import com.campusconnect.backend.entity.CampusService;
import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.BookingRepository;
import com.campusconnect.backend.repository.CampusServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BookingService {
    private static final List<Booking.Status> ACTIVE = List.of(Booking.Status.PENDING, Booking.Status.APPROVED);

    private final BookingRepository bookings;
    private final CampusServiceRepository services;
    private final AccessControl access;

    public BookingService(BookingRepository bookings, CampusServiceRepository services, AccessControl access) {
        this.bookings = bookings; this.services = services; this.access = access;
    }

    private ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private ResponseStatusException conflict(String m) { return new ResponseStatusException(HttpStatus.CONFLICT, m); }

    private Booking get(Long id) {
        return bookings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    public Booking create(User user, BookingRequest r) {
        CampusService svc = services.findById(r.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        Facility f = svc.facility;
        if (!f.active) throw bad(f.name + " is currently closed for bookings");
        if (svc.status != CampusService.Status.AVAILABLE) throw bad(svc.name + " is under maintenance");
        validateWindow(f, r.startTime(), r.endTime());
        if (bookings.countUserOverlap(user.id, ACTIVE, r.startTime(), r.endTime()) > 0)
            throw conflict("You already have a booking or pending request during this time");
        if (bookings.countOverlap(svc.id, Booking.Status.APPROVED, r.startTime(), r.endTime(), -1L) > 0)
            throw conflict("This slot overlaps an already approved booking");
        Booking b = new Booking();
        b.user = user; b.service = svc;
        b.startTime = r.startTime(); b.endTime = r.endTime();
        b.purpose = r.purpose();
        return bookings.save(b);
    }

    private void validateWindow(Facility f, LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) throw bad("End time must be after start time");
        if (start.isBefore(LocalDateTime.now())) throw bad("Start time must be in the future");
        if (!start.toLocalDate().equals(end.toLocalDate())) throw bad("A booking must start and end on the same day");
        LocalTime s = start.toLocalTime();
        LocalTime e = end.toLocalTime();
        if (s.isBefore(f.openTime) || e.isAfter(f.closeTime))
            throw bad(f.name + " is open from " + f.openTime + " to " + f.closeTime);
        long minutes = Duration.between(start, end).toMinutes();
        if (f.maxBookingMinutes != null && minutes > f.maxBookingMinutes)
            throw bad("The maximum booking length at " + f.name + " is " + f.maxBookingMinutes + " minutes");
    }

    public List<ResourceAvailability> availability(Long facilityId, LocalDateTime start, LocalDateTime end) {
        List<CampusService> list = services.findByFacilityIdOrderBySubTypeAscNameAsc(facilityId);
        List<Booking> overlapping = bookings.findOverlapping(facilityId, ACTIVE, start, end);
        Set<Long> taken = new HashSet<>();
        Map<Long, Long> pending = new LinkedHashMap<>();
        for (Booking b : overlapping) {
            if (b.status == Booking.Status.APPROVED) taken.add(b.service.id);
            else pending.merge(b.service.id, 1L, Long::sum);
        }
        return list.stream().map(s -> new ResourceAvailability(
                s.id, s.name, s.subType, s.capacity, s.location, s.status,
                s.status == CampusService.Status.AVAILABLE && !taken.contains(s.id),
                pending.getOrDefault(s.id, 0L))).toList();
    }

    public List<Booking> mine(User user) { return bookings.findByUserIdOrderByStartTimeDesc(user.id); }

    public List<Booking> forAdmin(User admin) {
        if (access.isSuper(admin)) return bookings.findAllByOrderByStartTimeDesc();
        Set<Long> ids = access.facilityIds(admin);
        if (ids.isEmpty()) return List.of();
        return bookings.findByServiceFacilityIdInOrderByStartTimeDesc(ids);
    }

    public List<Map<String, Object>> slots(Long serviceId) {
        return bookings.findByServiceIdAndStatusAndEndTimeAfterOrderByStartTimeAsc(
                        serviceId, Booking.Status.APPROVED, LocalDateTime.now())
                .stream().map(b -> Map.<String, Object>of("startTime", b.startTime, "endTime", b.endTime)).toList();
    }

    public Booking cancel(User user, Long id) {
        Booking b = get(id);
        if (!b.user.id.equals(user.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        if (b.status != Booking.Status.PENDING && b.status != Booking.Status.APPROVED)
            throw bad("This booking can no longer be cancelled");
        b.status = Booking.Status.CANCELLED;
        return bookings.save(b);
    }

    @Transactional
    public Booking approve(User admin, Long id) {
        Booking b = get(id);
        access.require(admin, b.service.facility);
        if (b.status != Booking.Status.PENDING) throw bad("Only pending bookings can be approved");
        if (b.startTime.isBefore(LocalDateTime.now())) throw bad("This slot is already in the past");
        if (bookings.countOverlap(b.service.id, Booking.Status.APPROVED, b.startTime, b.endTime, b.id) > 0)
            throw conflict("Conflicts with another approved booking - reject this one");
        b.status = Booking.Status.APPROVED;
        return bookings.save(b);
    }

    public Booking reject(User admin, Long id, String remarks) {
        Booking b = get(id);
        access.require(admin, b.service.facility);
        if (b.status != Booking.Status.PENDING) throw bad("Only pending bookings can be rejected");
        b.status = Booking.Status.REJECTED;
        b.remarks = remarks == null || remarks.isBlank() ? null : remarks.trim();
        return bookings.save(b);
    }

    public Booking complete(User admin, Long id) {
        Booking b = get(id);
        access.require(admin, b.service.facility);
        if (b.status != Booking.Status.APPROVED) throw bad("Only approved bookings can be completed");
        b.status = Booking.Status.COMPLETED;
        return bookings.save(b);
    }
}
