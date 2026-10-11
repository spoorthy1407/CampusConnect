package com.campusconnect.backend.service;

import com.campusconnect.backend.dto.Dtos.Checkout;
import com.campusconnect.backend.dto.Dtos.PaymentVerifyRequest;
import com.campusconnect.backend.dto.Dtos.PrintCheckout;
import com.campusconnect.backend.dto.Dtos.PrintConfig;
import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.entity.PrintFile;
import com.campusconnect.backend.entity.PrintJob;
import com.campusconnect.backend.entity.ServiceType;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.FacilityRepository;
import com.campusconnect.backend.repository.PrintJobRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class PrintService {
    public record FileDownload(Resource resource, String name, String contentType) {}

    private static final Map<String, String> TYPES = Map.of(
            "pdf", "application/pdf",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "ppt", "application/vnd.ms-powerpoint",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    private static final Set<String> IMAGES = Set.of("jpg", "jpeg", "png");
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PrintJobRepository jobs;
    private final FacilityRepository facilities;
    private final RazorpayService razorpay;
    private final AccessControl access;

    @Value("${app.print-dir:print-uploads}")
    private String printDir;
    @Value("${print.price.bw:2}")
    private BigDecimal priceBw;
    @Value("${print.price.color:10}")
    private BigDecimal priceColor;
    @Value("${print.price.a3-multiplier:2}")
    private BigDecimal a3Multiplier;
    @Value("${print.max-files:10}")
    private int maxFiles;

    public PrintService(PrintJobRepository jobs, FacilityRepository facilities, RazorpayService razorpay, AccessControl access) {
        this.jobs = jobs; this.facilities = facilities; this.razorpay = razorpay; this.access = access;
    }

    private ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }

    public PrintConfig config() {
        return new PrintConfig(razorpay.enabled(), razorpay.keyId(), priceBw, priceColor, a3Multiplier, maxFiles,
                facilities.findByTypeAndActiveTrueOrderByNameAsc(ServiceType.PRINTING));
    }

    public BigDecimal price(PrintJob.ColorMode color, PrintJob.PaperSize paper, int totalPages, int copies) {
        BigDecimal perPage = color == PrintJob.ColorMode.COLOR ? priceColor : priceBw;
        if (paper == PrintJob.PaperSize.A3) perPage = perPage.multiply(a3Multiplier);
        return perPage.multiply(BigDecimal.valueOf((long) totalPages * copies)).setScale(2, RoundingMode.HALF_UP);
    }

    public PrintCheckout submit(User user, Long facilityId, PrintJob.ColorMode color, PrintJob.Orientation orientation,
                                PrintJob.PaperSize paper, PrintJob.Sides sides, int copies, PrintJob.PaymentMode mode,
                                String notes, List<MultipartFile> files, List<Integer> pageHints) throws IOException {
        Facility f = facilities.findById(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Printing centre not found"));
        if (f.type != ServiceType.PRINTING || !f.active) throw bad("Choose a valid printing centre");
        if (copies < 1 || copies > 50) throw bad("Copies must be between 1 and 50");
        if (files == null || files.isEmpty()) throw bad("Attach at least one file");
        if (files.size() > maxFiles) throw bad("You can send at most " + maxFiles + " files in one job");
        if (mode == PrintJob.PaymentMode.ONLINE && !razorpay.enabled())
            throw bad("Online payment is not available right now. Please choose Pay at counter.");

        PrintJob job = new PrintJob();
        job.user = user; job.facility = f;
        job.colorMode = color; job.orientation = orientation; job.paperSize = paper; job.sides = sides;
        job.copies = copies; job.paymentMode = mode;
        job.notes = notes == null || notes.isBlank() ? null : notes.trim();
        job.pickupCode = pickupCode();

        List<Path> saved = new ArrayList<>();
        try {
            Path dir = Paths.get(printDir).toAbsolutePath();
            Files.createDirectories(dir);
            for (int i = 0; i < files.size(); i++) {
                MultipartFile mf = files.get(i);
                if (mf.isEmpty()) throw bad("One of the selected files is empty");
                String original = cleanName(mf.getOriginalFilename());
                String ext = extension(original);
                if (!TYPES.containsKey(ext))
                    throw bad("Unsupported file: " + original + ". Allowed types: PDF, JPG, PNG, DOC, DOCX, PPT, PPTX");
                String stored = UUID.randomUUID() + "." + ext;
                Path target = dir.resolve(stored);
                mf.transferTo(target);
                saved.add(target);
                PrintFile pf = new PrintFile();
                pf.job = job; pf.originalName = original; pf.storedName = stored;
                pf.contentType = TYPES.get(ext); pf.sizeBytes = mf.getSize();
                pf.pages = countPages(target, ext, pageHints, i, original);
                job.files.add(pf);
            }
        } catch (IOException | RuntimeException e) {
            saved.forEach(this::deleteQuietly);
            throw e;
        }

        job.totalPages = job.files.stream().mapToInt(x -> x.pages).sum();
        job.amount = price(color, paper, job.totalPages, copies);
        job.status = mode == PrintJob.PaymentMode.ONLINE ? PrintJob.Status.AWAITING_PAYMENT : PrintJob.Status.QUEUED;
        PrintJob savedJob = jobs.save(job);

        if (mode == PrintJob.PaymentMode.ONLINE) {
            try {
                return new PrintCheckout(savedJob, startPayment(savedJob));
            } catch (ResponseStatusException e) {
                return new PrintCheckout(jobs.findById(savedJob.id).orElse(savedJob), null);
            }
        }
        return new PrintCheckout(savedJob, null);
    }

    private int countPages(Path file, String ext, List<Integer> hints, int index, String name) {
        if (ext.equals("pdf")) {
            try (PDDocument doc = Loader.loadPDF(file.toFile())) {
                int n = doc.getNumberOfPages();
                if (n < 1) throw bad(name + " has no pages");
                return n;
            } catch (IOException e) {
                throw bad("Could not read " + name + ". If it is password protected, remove the password and upload it again.");
            }
        }
        if (IMAGES.contains(ext)) return 1;
        int hint = hints != null && index < hints.size() && hints.get(index) != null ? hints.get(index) : 0;
        if (hint < 1 || hint > 500) throw bad("Enter the number of pages for " + name);
        return hint;
    }

    private Checkout startPayment(PrintJob job) {
        PrintJob current = job;
        if (current.razorpayOrderId == null) {
            long paise = current.amount.movePointRight(2).longValue();
            current.razorpayOrderId = razorpay.createOrder(paise, "job-" + current.id);
            current = jobs.save(current);
        }
        return new Checkout(razorpay.keyId(), current.razorpayOrderId, current.amount.movePointRight(2).longValue(), "INR");
    }

    private PrintJob find(Long id) {
        return jobs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Print job not found"));
    }

    private PrintJob owned(User user, Long id) {
        PrintJob j = find(id);
        if (!j.user.id.equals(user.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your print job");
        return j;
    }

    public List<PrintJob> mine(User user) { return jobs.findByUserIdOrderByIdDesc(user.id); }

    public Checkout pay(User user, Long id) {
        PrintJob j = owned(user, id);
        if (j.status != PrintJob.Status.AWAITING_PAYMENT) throw bad("This job does not need payment");
        return startPayment(j);
    }

    public PrintJob verify(User user, Long id, PaymentVerifyRequest r) {
        PrintJob j = owned(user, id);
        if (j.paymentStatus == PrintJob.PaymentStatus.PAID) return j;
        if (j.status != PrintJob.Status.AWAITING_PAYMENT || j.razorpayOrderId == null
                || !j.razorpayOrderId.equals(r.razorpayOrderId()))
            throw bad("This payment does not match the print job");
        if (!razorpay.verify(j.razorpayOrderId, r.razorpayPaymentId(), r.razorpaySignature()))
            throw bad("Payment verification failed");
        j.paymentStatus = PrintJob.PaymentStatus.PAID;
        j.razorpayPaymentId = r.razorpayPaymentId();
        j.status = PrintJob.Status.QUEUED;
        return jobs.save(j);
    }

    public PrintJob cancel(User user, Long id) {
        PrintJob j = owned(user, id);
        if (j.status != PrintJob.Status.AWAITING_PAYMENT && j.status != PrintJob.Status.QUEUED)
            throw bad("Printing has already started, so this job can no longer be cancelled");
        return closeAsCancelled(j, null);
    }

    public List<PrintJob> forAdmin(User admin) {
        if (access.isSuper(admin)) return jobs.findAllByOrderByIdDesc();
        Set<Long> ids = access.facilityIds(admin);
        if (ids.isEmpty()) return List.of();
        return jobs.findByFacilityIdInOrderByIdDesc(ids);
    }

    private PrintJob managed(User admin, Long id) {
        PrintJob j = find(id);
        access.require(admin, j.facility);
        return j;
    }

    public PrintJob start(User admin, Long id) {
        PrintJob j = managed(admin, id);
        if (j.status != PrintJob.Status.QUEUED) throw bad("Only queued jobs can be started");
        j.status = PrintJob.Status.PRINTING;
        return jobs.save(j);
    }

    public PrintJob ready(User admin, Long id) {
        PrintJob j = managed(admin, id);
        if (j.status != PrintJob.Status.QUEUED && j.status != PrintJob.Status.PRINTING)
            throw bad("Only queued or printing jobs can be marked ready");
        j.status = PrintJob.Status.READY;
        return jobs.save(j);
    }

    public PrintJob collected(User admin, Long id) {
        PrintJob j = managed(admin, id);
        if (j.status != PrintJob.Status.READY) throw bad("Only ready jobs can be marked collected");
        j.status = PrintJob.Status.COLLECTED;
        if (j.paymentStatus == PrintJob.PaymentStatus.PENDING) j.paymentStatus = PrintJob.PaymentStatus.PAID;
        purgeFiles(j);
        return jobs.save(j);
    }

    public PrintJob cancelByAdmin(User admin, Long id, String remarks) {
        PrintJob j = managed(admin, id);
        if (j.status == PrintJob.Status.COLLECTED || j.status == PrintJob.Status.CANCELLED)
            throw bad("This job is already closed");
        return closeAsCancelled(j, remarks);
    }

    public PrintJob refunded(User admin, Long id) {
        PrintJob j = managed(admin, id);
        if (j.paymentStatus != PrintJob.PaymentStatus.REFUND_DUE) throw bad("No refund is pending for this job");
        j.paymentStatus = PrintJob.PaymentStatus.REFUNDED;
        return jobs.save(j);
    }

    private PrintJob closeAsCancelled(PrintJob j, String remarks) {
        j.status = PrintJob.Status.CANCELLED;
        j.remarks = remarks == null || remarks.isBlank() ? null : remarks.trim();
        if (j.paymentStatus == PrintJob.PaymentStatus.PAID) j.paymentStatus = PrintJob.PaymentStatus.REFUND_DUE;
        purgeFiles(j);
        return jobs.save(j);
    }

    public FileDownload download(User admin, Long jobId, Long fileId) {
        PrintJob j = managed(admin, jobId);
        PrintFile pf = j.files.stream().filter(x -> x.id.equals(fileId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (pf.removed) throw new ResponseStatusException(HttpStatus.GONE, "This file was deleted after the job was closed");
        Path path = Paths.get(printDir).toAbsolutePath().resolve(pf.storedName);
        if (!Files.exists(path)) throw new ResponseStatusException(HttpStatus.GONE, "File is no longer on the server");
        return new FileDownload(new FileSystemResource(path), pf.originalName, pf.contentType);
    }

    private void purgeFiles(PrintJob j) {
        Path dir = Paths.get(printDir).toAbsolutePath();
        for (PrintFile pf : j.files) {
            if (!pf.removed) {
                deleteQuietly(dir.resolve(pf.storedName));
                pf.removed = true;
            }
        }
    }

    private void deleteQuietly(Path p) {
        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
    }

    private String cleanName(String name) {
        String n = name == null ? "file" : name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).trim();
        if (n.isEmpty()) n = "file";
        return n.length() > 150 ? n.substring(n.length() - 150) : n;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private String pickupCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        return sb.toString();
    }
}
