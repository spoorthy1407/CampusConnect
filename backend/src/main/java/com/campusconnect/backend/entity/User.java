package com.campusconnect.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {
    public enum Role { STUDENT, ADMIN, SUPER_ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    public Long id;
    @Column(nullable = false)
    public String fullName;
    @Column(nullable = false, unique = true)
    public String email;
    @JsonIgnore
    @Column(nullable = false)
    public String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public Role role = Role.STUDENT;
    public LocalDateTime createdAt = LocalDateTime.now();

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "admin_facilities",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "facility_id"))
    public Set<Facility> managedFacilities = new HashSet<>();
}
