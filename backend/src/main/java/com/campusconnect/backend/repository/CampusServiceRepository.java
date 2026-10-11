package com.campusconnect.backend.repository;

import com.campusconnect.backend.entity.CampusService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface CampusServiceRepository extends JpaRepository<CampusService, Long> {
    List<CampusService> findAllByOrderByFacilityNameAscSubTypeAscNameAsc();
    List<CampusService> findByFacilityIdOrderBySubTypeAscNameAsc(Long facilityId);
    List<CampusService> findByFacilityIdInOrderByFacilityNameAscSubTypeAscNameAsc(Collection<Long> facilityIds);
    boolean existsByNameAndFacilityId(String name, Long facilityId);
}
