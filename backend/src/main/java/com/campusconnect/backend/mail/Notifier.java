package com.campusconnect.backend.mail;

import com.campusconnect.backend.entity.Booking;
import com.campusconnect.backend.entity.PrintJob;
import com.campusconnect.backend.entity.User;
import org.springframework.stereotype.Component;
import java.time.format.DateTimeFormatter;

@Component
public class Notifier {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy, hh:mm a");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");

    private final MailService mail;

    public Notifier(MailService mail) { this.mail = mail; }

    private String slot(Booking b) {
        return b.startTime.format(WHEN) + " to " + b.endTime.format(TIME);
    }

    private String place(Booking b) {
        String block = b.service.facility.blockName == null ? "" : " (" + b.service.facility.blockName + ")";
        return b.service.facility.name + block;
    }

    public void bookingApproved(Booking b, User admin) {
        String body = """
                Hi %s,

                Good news! Your booking request has been approved.

                Resource : %s
                Place    : %s
                When     : %s
                Purpose  : %s
                Approved by : %s

                Please arrive on time. If your plans change, cancel the booking from your Profile page so others can use the slot.

                - CampusConnect
                """.formatted(b.user.fullName, b.service.name, place(b), slot(b),
                b.purpose == null || b.purpose.isBlank() ? "-" : b.purpose, admin.fullName);
        mail.send(b.user.email, "Booking approved: " + b.service.name, body);
    }

    public void bookingRejected(Booking b, User admin) {
        String reason = b.remarks == null || b.remarks.isBlank() ? "No reason was given." : b.remarks;
        String body = """
                Hi %s,

                Sorry, your booking request could not be approved.

                Resource : %s
                Place    : %s
                When     : %s
                Reason   : %s
                Reviewed by : %s

                You can pick another slot or resource from the Book a Service page.

                - CampusConnect
                """.formatted(b.user.fullName, b.service.name, place(b), slot(b), reason, admin.fullName);
        mail.send(b.user.email, "Booking not approved: " + b.service.name, body);
    }

    public void printReady(PrintJob j) {
        String block = j.facility.blockName == null ? "" : " (" + j.facility.blockName + ")";
        String body = """
                Hi %s,

                Your print job #%d is ready for pickup.

                Collect from : %s%s
                Pages        : %d x %d copies
                Amount       : Rs. %s (%s)
                Pickup code  : %s

                Show this code at the counter to collect your prints.

                - CampusConnect
                """.formatted(j.user.fullName, j.id, j.facility.name, block, j.totalPages, j.copies,
                j.amount.toPlainString(), j.paymentStatus == PrintJob.PaymentStatus.PAID ? "paid" : "pay at counter",
                j.pickupCode);
        mail.send(j.user.email, "Your prints are ready (job #" + j.id + ")", body);
    }

    public void printCancelled(PrintJob j) {
        String reason = j.remarks == null || j.remarks.isBlank() ? "No reason was given." : j.remarks;
        String refund = j.paymentStatus == PrintJob.PaymentStatus.REFUND_DUE
                ? "Your online payment will be refunded by the print centre.\n" : "";
        String body = """
                Hi %s,

                Your print job #%d at %s was cancelled.

                Reason : %s
                %s
                - CampusConnect
                """.formatted(j.user.fullName, j.id, j.facility.name, reason, refund);
        mail.send(j.user.email, "Print job cancelled (job #" + j.id + ")", body);
    }
}
