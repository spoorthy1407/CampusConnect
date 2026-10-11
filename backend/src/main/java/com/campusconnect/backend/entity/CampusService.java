package com.campusconnect.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "services")
public class CampusService {
    public enum Status { AVAILABLE, MAINTENANCE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    public Long id;
    @Column(nullable = false)
    public String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public ServiceType type;
    @ManyToOne(optional = false) @JoinColumn(name = "facility_id")
    public Facility facility;
    public String subType;
    public Integer capacity = 1;
    public String location;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public Status status = Status.AVAILABLE;
}
