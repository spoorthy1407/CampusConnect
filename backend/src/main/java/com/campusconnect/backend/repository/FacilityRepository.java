package com.campusconnect.backend.repository;

import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    Optional<Facility> findByName(String name);
    List<Facility> findAllByOrderByNameAsc();
    List<Facility> findByActiveTrueOrderByNameAsc();
    List<Facility> findByTypeAndActiveTrueOrderByNameAsc(ServiceType type);
}
