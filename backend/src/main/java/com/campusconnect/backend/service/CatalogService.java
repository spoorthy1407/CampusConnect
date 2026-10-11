package com.campusconnect.backend.service;

import com.campusconnect.backend.dto.Dtos.ServiceRequest;
import com.campusconnect.backend.entity.CampusService;
import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.entity.ServiceType;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.CampusServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Set;

@Service
public class CatalogService {
    private final CampusServiceRepository repo;
    private final FacilityService facilities;
    private final AccessControl access;

    public CatalogService(CampusServiceRepository repo, FacilityService facilities, AccessControl access) {
        this.repo = repo; this.facilities = facilities; this.access = access;
    }

    public List<CampusService> all(Long facilityId) {
        if (facilityId != null) return repo.findByFacilityIdOrderBySubTypeAscNameAsc(facilityId);
        return repo.findAllByOrderByFacilityNameAscSubTypeAscNameAsc();
    }

    public List<CampusService> forAdmin(User admin) {
        if (access.isSuper(admin)) return repo.findAllByOrderByFacilityNameAscSubTypeAscNameAsc();
        Set<Long> ids = access.facilityIds(admin);
        if (ids.isEmpty()) return List.of();
        return repo.findByFacilityIdInOrderByFacilityNameAscSubTypeAscNameAsc(ids);
    }

    public CampusService create(User admin, ServiceRequest r) {
        Facility f = facilities.get(r.facilityId());
        access.require(admin, f);
        if (repo.existsByNameAndFacilityId(r.name().trim(), f.id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This facility already has a resource with that name");
        CampusService s = new CampusService();
        apply(s, f, r);
        return repo.save(s);
    }

    public CampusService update(User admin, Long id, ServiceRequest r) {
        CampusService s = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        access.require(admin, s.facility);
        Facility f = facilities.get(r.facilityId());
        access.require(admin, f);
        apply(s, f, r);
        return repo.save(s);
    }

    private void apply(CampusService s, Facility f, ServiceRequest r) {
        if (f.type == ServiceType.PRINTING)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Printing centres take print jobs, not slot bookings");
        s.name = r.name().trim();
        s.facility = f;
        s.type = f.type;
        s.subType = r.subType() == null || r.subType().isBlank() ? null : r.subType().trim();
        s.capacity = r.capacity() == null ? 1 : r.capacity();
        s.location = r.location();
        if (r.status() != null) s.status = r.status();
    }
}
