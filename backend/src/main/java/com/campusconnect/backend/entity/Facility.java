package com.campusconnect.backend.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "facilities")
public class Facility {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "facility_id")
    public Long id;
    @Column(nullable = false, unique = true)
    public String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public ServiceType type;
    public String blockName;
    @Column(length = 500)
    public String description;
    @Column(nullable = false)
    public LocalTime openTime = LocalTime.of(9, 0);
    @Column(nullable = false)
    public LocalTime closeTime = LocalTime.of(17, 0);
    public Integer maxBookingMinutes = 120;
    @Column(nullable = false)
    public boolean active = true;
}
