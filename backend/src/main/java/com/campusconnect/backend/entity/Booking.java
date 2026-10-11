package com.campusconnect.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    public enum Status { PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    public User user;
    @ManyToOne(optional = false) @JoinColumn(name = "service_id")
    public CampusService service;
    @Column(nullable = false)
    public LocalDateTime startTime;
    @Column(nullable = false)
    public LocalDateTime endTime;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public Status status = Status.PENDING;
    public String purpose;
    @Column(length = 500)
    public String remarks;
    public LocalDateTime createdAt = LocalDateTime.now();
}
