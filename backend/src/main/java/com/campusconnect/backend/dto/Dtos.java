package com.campusconnect.backend.dto;

import com.campusconnect.backend.entity.CampusService;
import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.entity.PrintJob;
import com.campusconnect.backend.entity.ServiceType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class Dtos {
    public record RegisterRequest(@NotBlank String fullName,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record BookingRequest(@NotNull Long serviceId,
                                 @NotNull LocalDateTime startTime,
                                 @NotNull LocalDateTime endTime,
                                 String purpose) {}

    public record RemarkRequest(String remarks) {}

    public record FacilityRequest(@NotBlank String name,
                                  @NotNull ServiceType type,
                                  String blockName,
                                  String description,
                                  @NotNull LocalTime openTime,
                                  @NotNull LocalTime closeTime,
                                  @Min(15) Integer maxBookingMinutes,
                                  Boolean active) {}

    public record ServiceRequest(@NotBlank String name,
                                 @NotNull Long facilityId,
                                 String subType,
                                 @Min(1) Integer capacity,
                                 String location,
                                 CampusService.Status status) {}

    public record ResourceAvailability(Long id, String name, String subType, Integer capacity, String location,
                                       CampusService.Status status, boolean free, long pending) {}

    public record AdminCreateRequest(String fullName,
                                     @NotBlank @Email String email,
                                     String password,
                                     @NotEmpty List<Long> facilityIds) {}

    public record FacilityAccessRequest(@NotEmpty List<Long> facilityIds) {}

    public record AdminInfo(Long id, String fullName, String email, List<Long> facilityIds) {}

    public record AdminScope(String role, List<Facility> facilities) {}

    public record PrintConfig(boolean razorpayEnabled, String razorpayKeyId, BigDecimal priceBw, BigDecimal priceColor,
                              BigDecimal a3Multiplier, int maxFiles, List<Facility> facilities) {}

    public record Checkout(String keyId, String orderId, long amountPaise, String currency) {}

    public record PrintCheckout(PrintJob job, Checkout checkout) {}

    public record PaymentVerifyRequest(@NotBlank String razorpayOrderId,
                                       @NotBlank String razorpayPaymentId,
                                       @NotBlank String razorpaySignature) {}
}
