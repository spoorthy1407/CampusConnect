package com.campusconnect.backend.service;

import com.campusconnect.backend.dto.Dtos.AdminCreateRequest;
import com.campusconnect.backend.dto.Dtos.AdminInfo;
import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.FacilityRepository;
import com.campusconnect.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {
    private final UserRepository users;
    private final FacilityRepository facilities;
    private final PasswordEncoder encoder;

    public AdminUserService(UserRepository users, FacilityRepository facilities, PasswordEncoder encoder) {
        this.users = users; this.facilities = facilities; this.encoder = encoder;
    }

    private AdminInfo info(User u) {
        return new AdminInfo(u.id, u.fullName, u.email, u.managedFacilities.stream().map(f -> f.id).sorted().toList());
    }

    private Set<Facility> resolve(List<Long> ids) {
        List<Facility> found = facilities.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more selected facilities do not exist");
        return new HashSet<>(found);
    }

    public List<AdminInfo> list() {
        return users.findByRoleOrderByFullNameAsc(User.Role.ADMIN).stream().map(this::info).toList();
    }

    public AdminInfo create(AdminCreateRequest r) {
        String email = r.email().trim().toLowerCase();
        Set<Facility> assigned = resolve(r.facilityIds());
        User u = users.findByEmail(email).orElse(null);
        if (u != null) {
            if (u.role != User.Role.STUDENT)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This email already belongs to an admin");
        } else {
            if (r.fullName() == null || r.fullName().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required for a new admin");
            if (r.password() == null || r.password().length() < 6)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
            u = new User();
            u.fullName = r.fullName().trim();
            u.email = email;
            u.passwordHash = encoder.encode(r.password());
        }
        u.role = User.Role.ADMIN;
        u.managedFacilities = assigned;
        return info(users.save(u));
    }

    public AdminInfo setFacilities(Long id, List<Long> facilityIds) {
        User u = admin(id);
        u.managedFacilities = resolve(facilityIds);
        return info(users.save(u));
    }

    public void remove(Long id) {
        User u = admin(id);
        u.role = User.Role.STUDENT;
        u.managedFacilities = new HashSet<>();
        users.save(u);
    }

    private User admin(Long id) {
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (u.role != User.Role.ADMIN) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This user is not a facility admin");
        return u;
    }
}
