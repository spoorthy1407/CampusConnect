package com.campusconnect.backend.config;

import com.campusconnect.backend.entity.CampusService;
import com.campusconnect.backend.entity.Facility;
import com.campusconnect.backend.entity.ServiceType;
import com.campusconnect.backend.entity.User;
import com.campusconnect.backend.repository.CampusServiceRepository;
import com.campusconnect.backend.repository.FacilityRepository;
import com.campusconnect.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalTime;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final FacilityRepository facilities;
    private final CampusServiceRepository services;
    private final PasswordEncoder encoder;

    @Value("${app.super-admin.name:Campus Admin}")
    private String adminName;
    @Value("${app.super-admin.email:admin@campus.edu}")
    private String adminEmail;
    @Value("${app.super-admin.password:admin123}")
    private String adminPassword;

    public DataSeeder(UserRepository users, FacilityRepository facilities, CampusServiceRepository services,
                      PasswordEncoder encoder) {
        this.users = users; this.facilities = facilities; this.services = services; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (!users.existsByEmail(adminEmail.toLowerCase())) {
            User a = new User();
            a.fullName = adminName;
            a.email = adminEmail.toLowerCase();
            a.passwordHash = encoder.encode(adminPassword);
            a.role = User.Role.SUPER_ADMIN;
            users.save(a);
        }
        if (facilities.count() == 0) seedCampus();
    }

    private void seedCampus() {
        for (String block : new String[]{"G-Block", "E-Block", "D-Block"}) {
            Facility lib = facility(block + " Library", ServiceType.LIBRARY, block,
                    "Quiet reading hall with discussion rooms and group tables. Silence is maintained in the reading area.",
                    8, 30, 20, 0, 180);
            resource(lib, "Study Room 1", "Study Room", 6, block + " Library, first floor");
            resource(lib, "Study Room 2", "Study Room", 6, block + " Library, first floor");
            resource(lib, "Group Table 1", "Group Table", 4, block + " Library, ground floor");
            resource(lib, "Group Table 2", "Group Table", 4, block + " Library, ground floor");
        }

        Facility space = facility("G-Block Interactive Space", ServiceType.INTERACTIVE_SPACE, "G-Block",
                "Collaborative space with discussion chambers, study tables and computers for group study.",
                8, 0, 20, 0, 180);
        resource(space, "Chamber 1", "Chamber", 6, "G-Block Interactive Space");
        resource(space, "Chamber 2", "Chamber", 6, "G-Block Interactive Space");
        resource(space, "Chamber 3", "Chamber", 8, "G-Block Interactive Space");
        for (int i = 1; i <= 4; i++)
            resource(space, "Table " + i, "Table", 4, "G-Block Interactive Space");
        for (int i = 1; i <= 8; i++)
            resource(space, String.format("Computer %02d", i), "Computer", 1, "G-Block Interactive Space");

        facility("I-Block Printing Centre", ServiceType.PRINTING, "I-Block",
                "Send your files online and collect the prints at the I-Block counter. Colour and black and white, A4 and A3.",
                9, 0, 17, 0, 120);

        Facility sports = facility("Sports Complex", ServiceType.SPORTS_COURT, "Sports Complex",
                "Indoor courts for badminton, basketball and table tennis. Bring your own racket.",
                6, 0, 20, 0, 90);
        resource(sports, "Badminton Court 1", "Badminton", 4, "Sports Complex");
        resource(sports, "Badminton Court 2", "Badminton", 4, "Sports Complex");
        resource(sports, "Basketball Court", "Basketball", 10, "Sports Complex");
        resource(sports, "Table Tennis Table", "Table Tennis", 4, "Sports Complex");

        Facility tutoring = facility("Peer Tutoring Centre", ServiceType.TUTORING, "CSE Block",
                "Book a one-to-one session with a senior student tutor.",
                9, 0, 17, 0, 60);
        resource(tutoring, "Maths Tutoring", "Tutoring", 2, "CSE Block, Room 2");
        resource(tutoring, "Programming Tutoring", "Tutoring", 2, "CSE Block, Lab 1");
    }

    private Facility facility(String name, ServiceType type, String block, String description,
                              int openH, int openM, int closeH, int closeM, int maxMinutes) {
        Facility f = new Facility();
        f.name = name; f.type = type; f.blockName = block; f.description = description;
        f.openTime = LocalTime.of(openH, openM);
        f.closeTime = LocalTime.of(closeH, closeM);
        f.maxBookingMinutes = maxMinutes;
        return facilities.save(f);
    }

    private void resource(Facility f, String name, String subType, int capacity, String location) {
        CampusService s = new CampusService();
        s.name = name; s.facility = f; s.type = f.type; s.subType = subType;
        s.capacity = capacity; s.location = location;
        services.save(s);
    }
}
