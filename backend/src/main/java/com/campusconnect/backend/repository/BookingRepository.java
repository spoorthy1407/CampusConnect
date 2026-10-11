package com.campusconnect.backend.repository;

import com.campusconnect.backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);
    List<Booking> findAllByOrderByStartTimeDesc();
    List<Booking> findByServiceFacilityIdInOrderByStartTimeDesc(Collection<Long> facilityIds);
    List<Booking> findByServiceIdAndStatusAndEndTimeAfterOrderByStartTimeAsc(Long serviceId, Booking.Status status, LocalDateTime now);

    @Query("select count(b) from Booking b where b.service.id = :sid and b.status = :status " +
           "and b.startTime < :end and b.endTime > :start and b.id <> :excludeId")
    long countOverlap(@Param("sid") Long sid, @Param("status") Booking.Status status,
                      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                      @Param("excludeId") Long excludeId);

    @Query("select count(b) from Booking b where b.user.id = :uid and b.status in :statuses " +
           "and b.startTime < :end and b.endTime > :start")
    long countUserOverlap(@Param("uid") Long uid, @Param("statuses") Collection<Booking.Status> statuses,
                          @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select b from Booking b where b.service.facility.id = :fid and b.status in :statuses " +
           "and b.startTime < :end and b.endTime > :start")
    List<Booking> findOverlapping(@Param("fid") Long fid, @Param("statuses") Collection<Booking.Status> statuses,
                                  @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
