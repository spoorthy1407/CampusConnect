package com.campusconnect.backend.controller;

import com.campusconnect.backend.dto.Dtos.Checkout;
import com.campusconnect.backend.dto.Dtos.PaymentVerifyRequest;
import com.campusconnect.backend.dto.Dtos.PrintCheckout;
import com.campusconnect.backend.dto.Dtos.PrintConfig;
import com.campusconnect.backend.entity.PrintJob;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.service.PrintService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/print")
public class PrintController {
    private final PrintService prints;

    public PrintController(PrintService prints) { this.prints = prints; }

    @GetMapping("/config")
    public PrintConfig config() { return prints.config(); }

    @GetMapping("/jobs/my")
    public List<PrintJob> mine(@AuthenticationPrincipal User user) { return prints.mine(user); }

    @PostMapping(value = "/jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PrintCheckout submit(@AuthenticationPrincipal User user,
                                @RequestParam Long facilityId,
                                @RequestParam PrintJob.ColorMode colorMode,
                                @RequestParam PrintJob.Orientation orientation,
                                @RequestParam PrintJob.PaperSize paperSize,
                                @RequestParam PrintJob.Sides sides,
                                @RequestParam int copies,
                                @RequestParam PrintJob.PaymentMode paymentMode,
                                @RequestParam(required = false) String notes,
                                @RequestParam("files") List<MultipartFile> files,
                                @RequestParam(value = "pages", required = false) List<Integer> pages) throws IOException {
        return prints.submit(user, facilityId, colorMode, orientation, paperSize, sides, copies, paymentMode, notes, files, pages);
    }

    @PostMapping("/jobs/{id}/pay")
    public Checkout pay(@AuthenticationPrincipal User user, @PathVariable Long id) { return prints.pay(user, id); }

    @PostMapping("/jobs/{id}/verify")
    public PrintJob verify(@AuthenticationPrincipal User user, @PathVariable Long id,
                           @Valid @RequestBody PaymentVerifyRequest r) {
        return prints.verify(user, id, r);
    }

    @PutMapping("/jobs/{id}/cancel")
    public PrintJob cancel(@AuthenticationPrincipal User user, @PathVariable Long id) { return prints.cancel(user, id); }
}
