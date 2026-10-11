package com.campusconnect.backend.service;

import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AccessControl {
    public boolean isSuper(User u) { return u.role == User.Role.SUPER_ADMIN; }

    public Set<Long> facilityIds(User u) {
        return u.managedFacilities.stream().map(f -> f.id).collect(Collectors.toSet());
    }

    public boolean canManage(User u, Facility f) {
        if (isSuper(u)) return true;
        return u.role == User.Role.ADMIN && facilityIds(u).contains(f.id);
    }

    public void require(User u, Facility f) {
        if (!canManage(u, f))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the admin of " + f.name);
    }
}
