package com.campusconnect.backend.service;

import com.campusconnect.backend.entity.Listing;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ListingService {
    private final ListingRepository listings;
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ListingService(ListingRepository listings) { this.listings = listings; }

    public List<Listing> browse(String category, String q, BigDecimal maxPrice) {
        return listings.findByStatusOrderByIdDesc(Listing.Status.ACTIVE).stream()
                .filter(l -> category == null || category.isBlank() || category.equalsIgnoreCase(l.category))
                .filter(l -> q == null || q.isBlank() || l.title.toLowerCase().contains(q.toLowerCase()))
                .filter(l -> maxPrice == null || (l.price != null && l.price.compareTo(maxPrice) <= 0))
                .toList();
    }

    public List<Listing> mine(User u) { return listings.findBySellerIdOrderByIdDesc(u.id); }

    public List<Listing> all() { return listings.findAllByOrderByIdDesc(); }

    public Listing create(User seller, String title, String category, BigDecimal price, MultipartFile image) throws IOException {
        if (title == null || title.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        if (price == null || price.signum() < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid price");
        Listing l = new Listing();
        l.seller = seller; l.title = title.trim(); l.category = category; l.price = price;
        l.imageUrl = saveImage(image);
        return listings.save(l);
    }

    public Listing markSold(User u, Long id) {
        Listing l = find(id);
        if (!l.seller.id.equals(u.id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your listing");
        l.status = Listing.Status.SOLD;
        return listings.save(l);
    }

    public void remove(User u, Long id) {
        Listing l = find(id);
        if (!l.seller.id.equals(u.id) && u.role != User.Role.SUPER_ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        listings.delete(l);
    }

    private Listing find(Long id) {
        return listings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
    }

    private String saveImage(MultipartFile f) throws IOException {
        if (f == null || f.isEmpty()) return null;
        String ct = f.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        String ext = ".jpg";
        String orig = f.getOriginalFilename();
        if (orig != null && orig.lastIndexOf('.') >= 0) {
            String e = orig.substring(orig.lastIndexOf('.')).toLowerCase();
            if (e.matches("\\.[a-z0-9]{2,5}")) ext = e;
        }
        Path dir = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(dir);
        String name = UUID.randomUUID() + ext;
        f.transferTo(dir.resolve(name));
        return "/uploads/" + name;
    }
}
