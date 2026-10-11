package com.campusconnect.backend.service;

import com.campusconnect.backend.dto.Dtos.FacilityRequest;
import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.FacilityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Comparator;
import java.util.List;

@Service
public class FacilityService {
    private final FacilityRepository repo;
    private final AccessControl access;

    public FacilityService(FacilityRepository repo, AccessControl access) {
        this.repo = repo; this.access = access;
    }

    public List<Facility> active() { return repo.findByActiveTrueOrderByNameAsc(); }

    public List<Facility> managedBy(User u) {
        if (access.isSuper(u)) return repo.findAllByOrderByNameAsc();
        return u.managedFacilities.stream().sorted(Comparator.comparing(f -> f.name)).toList();
    }

    public Facility get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
    }

    public Facility create(FacilityRequest r) {
        if (repo.findByName(r.name().trim()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A facility with this name already exists");
        Facility f = new Facility();
        apply(f, r);
        return repo.save(f);
    }

    public Facility update(Long id, FacilityRequest r) {
        Facility f = get(id);
        if (f.type != r.type())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The type of a facility cannot be changed after it is created");
        repo.findByName(r.name().trim()).ifPresent(other -> {
            if (!other.id.equals(f.id))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A facility with this name already exists");
        });
        apply(f, r);
        return repo.save(f);
    }

    private void apply(Facility f, FacilityRequest r) {
        if (!r.closeTime().isAfter(r.openTime()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Closing time must be after opening time");
        f.name = r.name().trim();
        f.type = r.type();
        f.blockName = r.blockName();
        f.description = r.description();
        f.openTime = r.openTime();
        f.closeTime = r.closeTime();
        f.maxBookingMinutes = r.maxBookingMinutes() == null ? 120 : r.maxBookingMinutes();
        if (r.active() != null) f.active = r.active();
    }
}
