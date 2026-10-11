package com.campusconnect.backend.repository;

import com.campusconnect.backend.entity.PrintJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface PrintJobRepository extends JpaRepository<PrintJob, Long> {
    List<PrintJob> findByUserIdOrderByIdDesc(Long userId);
    List<PrintJob> findAllByOrderByIdDesc();
    List<PrintJob> findByFacilityIdInOrderByIdDesc(Collection<Long> facilityIds);
}
