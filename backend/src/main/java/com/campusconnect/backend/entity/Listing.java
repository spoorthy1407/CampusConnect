package com.campusconnect.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "listings")
public class Listing {
    public enum Status { ACTIVE, SOLD }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "seller_id")
    public User seller;
    @Column(nullable = false)
    public String title;
    public String category;
    @Column(precision = 10, scale = 2)
    public BigDecimal price;
    public String imageUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public Status status = Status.ACTIVE;
    public LocalDateTime createdAt = LocalDateTime.now();
}
