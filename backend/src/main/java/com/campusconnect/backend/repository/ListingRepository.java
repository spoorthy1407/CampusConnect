package com.campusconnect.backend.repository;

import com.campusconnect.backend.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByStatusOrderByIdDesc(Listing.Status status);
    List<Listing> findBySellerIdOrderByIdDesc(Long sellerId);
    List<Listing> findAllByOrderByIdDesc();
    long countByStatus(Listing.Status status);
}
