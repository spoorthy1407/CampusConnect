package com.campusconnect.backend.service;

import com.campusconnect.backend.entity.Booking;
import com.campusconnect.backend.entity.Listing;
import com.campusconnect.backend.entity.PrintJob;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.ListingRepository;
import com.campusconnect.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final BookingService bookings;
    private final PrintService prints;
    private final AccessControl access;
    private final ListingRepository listingRepo;
    private final UserRepository userRepo;

    public AnalyticsService(BookingService bookings, PrintService prints, AccessControl access,
                            ListingRepository listingRepo, UserRepository userRepo) {
        this.bookings = bookings; this.prints = prints; this.access = access;
        this.listingRepo = listingRepo; this.userRepo = userRepo;
    }

    private List<Map<String, Object>> rows(Map<String, Long> counts, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
                .toList();
    }

    public Map<String, Object> analytics(User admin) {
        List<Booking> bs = bookings.forAdmin(admin);
        List<PrintJob> js = prints.forAdmin(admin);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bookingsPerResource", rows(bs.stream().collect(Collectors.groupingBy(
                b -> b.service.facility.name + " - " + b.service.name, Collectors.counting())), 10));
        m.put("bookingsPerFacility", rows(bs.stream().collect(Collectors.groupingBy(
                b -> b.service.facility.name, Collectors.counting())), 10));
        m.put("topStudents", rows(bs.stream().collect(Collectors.groupingBy(
                b -> b.user.fullName, Collectors.counting())), 5));
        m.put("bookingsByStatus", rows(bs.stream().collect(Collectors.groupingBy(
                b -> b.status.name(), Collectors.counting())), 10));

        List<PrintJob> live = js.stream().filter(j -> j.status != PrintJob.Status.CANCELLED).toList();
        BigDecimal revenue = js.stream().filter(j -> j.paymentStatus == PrintJob.PaymentStatus.PAID)
                .map(j -> j.amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        m.put("printJobs", live.size());
        m.put("pagesPrinted", live.stream().mapToLong(j -> (long) j.totalPages * j.copies).sum());
        m.put("printRevenue", revenue);
        m.put("printJobsByStatus", rows(js.stream().collect(Collectors.groupingBy(
                j -> j.status.name(), Collectors.counting())), 10));

        if (access.isSuper(admin)) {
            m.put("activeListings", listingRepo.countByStatus(Listing.Status.ACTIVE));
            m.put("soldListings", listingRepo.countByStatus(Listing.Status.SOLD));
            m.put("totalUsers", userRepo.count());
        }
        return m;
    }
}
