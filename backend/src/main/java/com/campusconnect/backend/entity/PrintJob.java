package com.campusconnect.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "print_jobs")
public class PrintJob {
    public enum Status { AWAITING_PAYMENT, QUEUED, PRINTING, READY, COLLECTED, CANCELLED }
    public enum ColorMode { BLACK_WHITE, COLOR }
    public enum Orientation { PORTRAIT, LANDSCAPE }
    public enum PaperSize { A4, A3 }
    public enum Sides { SINGLE, DOUBLE }
    public enum PaymentMode { ONLINE, COUNTER }
    public enum PaymentStatus { PENDING, PAID, REFUND_DUE, REFUNDED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "print_job_id")
    public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    public User user;
    @ManyToOne(optional = false) @JoinColumn(name = "facility_id")
    public Facility facility;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public ColorMode colorMode;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public Orientation orientation;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public PaperSize paperSize;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public Sides sides;
    public int copies = 1;
    public int totalPages;
    @Column(precision = 10, scale = 2)
    public BigDecimal amount;
    @Column(length = 500)
    public String notes;
    @Column(length = 500)
    public String remarks;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public Status status = Status.QUEUED;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public PaymentMode paymentMode;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public PaymentStatus paymentStatus = PaymentStatus.PENDING;
    public String razorpayOrderId;
    public String razorpayPaymentId;
    public String pickupCode;
    public LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    public List<PrintFile> files = new ArrayList<>();
}
