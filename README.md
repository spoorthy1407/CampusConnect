# CampusConnect

```
CampusConnect/
  backend/   Spring Boot 3.3 + Spring Data JPA + Spring Security (JWT) + MySQL   (Java 17+)
  frontend/  React 18 + Vite + React Router
```

## What is in this version

- Facilities: G-Block / E-Block / D-Block Libraries, G-Block Interactive Space, I-Block Printing Centre, Sports Complex, Peer Tutoring Centre. Each has a block, opening hours, a maximum booking length and a description shown to students.
- Interactive Space (G-Block): students book a chamber, a table or a computer. They pick a date and time first and see which resources are free for exactly that window.
- Booking rules: future time only, same day, inside opening hours, within the maximum length, no overlap with an approved booking of the same resource, and a student cannot hold two overlapping bookings.
- Facility admins: every admin sees and decides only the bookings and print jobs of the facilities assigned to them. The super admin creates admins and assigns facilities.
- Email: the student gets an email when a booking is approved or rejected, and when print jobs are ready or cancelled.
- Printing: students upload PDFs, photos or documents, choose black and white or colour, portrait or landscape, A4 or A3, single or double sided and number of copies, then pay with Razorpay or at the counter. The print centre admin downloads the files, prints, marks the job ready and the student collects it with a pickup code. Files are deleted from the server when a job is collected or cancelled.

## One-time setup (first run, or whenever you upgrade from the old version)

1. Install Java 17 or newer, Maven 3.9+, Node 18+ and MySQL 8. Start MySQL.
2. The database structure changed, so delete the old database once. In a MySQL shell:

   ```sql
   DROP DATABASE IF EXISTS campusconnect;
   ```

   (The backend creates a fresh `campusconnect` database and all tables by itself.)
3. Open `backend/src/main/resources/application.properties` and set `spring.datasource.password` to your MySQL root password.

## Run the backend

```
cd backend
mvn spring-boot:run
```

Or open the `backend` folder in IntelliJ / VS Code and run `BackendApplication`.

Check it: http://localhost:8080/api/test should print `CampusConnect backend is running`.

On first start the app creates:
- the super admin `admin@campus.edu` / `admin123` (change it in `application.properties` under `app.super-admin.*` before the first run)
- all facilities and resources listed above (3 libraries with study rooms and group tables, the Interactive Space with 3 chambers, 4 tables and 8 computers, the I-Block printing centre, sports courts and tutoring)

## Run the frontend

```
cd frontend
npm install
npm run dev
```

Open http://localhost:5173

If the backend is not on port 8080, copy `.env.example` to `.env` and change `VITE_API_URL`.

## First walk-through (about 5 minutes)

1. Log in as `admin@campus.edu` / `admin123`, open Super Admin, then the Admins tab.
2. Add an admin for each library. Example: name Priya, email priya@campus.edu, a password, tick only "G-Block Library". Do the same for D-Block, E-Block, the Interactive Space and the I-Block Printing Centre.
3. Log out. Register a normal student account (use a real email if you want to receive the approval mail).
4. As the student: Book a Service, pick G-Block Interactive Space, choose a date and time, pick a computer or chamber, submit.
5. Log in as the G-Block Interactive Space admin: Bookings, Approve. The student receives the approval email.
6. Log in as an admin of a different library: the first student's booking is not visible to them.
7. As the student: Print, choose the I-Block Printing Centre, attach a PDF, choose options, send. Log in as the I-Block admin, Print Jobs, download the file, Start printing, Mark ready (the student is emailed), Collected.

## Turn on approval emails (Gmail example)

1. Google account, Security, turn on 2-Step Verification, then create an App Password (16 characters).
2. In `application.properties` set:

   ```
   app.mail.enabled=true
   spring.mail.username=yourname@gmail.com
   spring.mail.password=your16characterapppassword
   ```

3. Restart the backend. While `app.mail.enabled=false` the emails are only printed in the backend console, so the rest of the app still works.

## Turn on Razorpay payments

1. Create a Razorpay account and open Dashboard, Settings, API Keys, then generate Test Mode keys.
2. In `application.properties` set `razorpay.key-id=rzp_test_xxxxxxxx` and `razorpay.key-secret=xxxxxxxx`.
3. Restart the backend. The "Pay online (Razorpay)" option becomes active on the Print page.
4. Test with Razorpay's test card or UPI details from their documentation. The payment signature is verified on the server before a job is queued.
5. Without keys, students can still print and choose Pay at counter.

Change printing prices in `application.properties` (`print.price.bw`, `print.price.color`, `print.price.a3-multiplier`).

## Notes

- Page counting: PDFs are counted automatically and each photo counts as 1 page. For Word and PowerPoint files the student types the number of pages. If you want everything counted automatically, ask students to upload PDFs.
- Adding a library, a printing point in another block (for example the canteen) or a new admin does not need code: use Super Admin, Facilities and Admins. Add resources (computers, chambers, rooms) in the Resources tab.
- The super admin also moderates the marketplace.
- Uploaded print files are kept in `backend/print-uploads/`, marketplace photos in `backend/uploads/`.

## Common first-run errors

- `Access denied for user 'root'` : wrong MySQL password in `application.properties`.
- `Data truncated for column 'role'` or `Unknown column` : the old database is still there. Run `DROP DATABASE campusconnect;` and start again.
- `Unsupported class file major version` : Java older than 17.
- CORS error in the browser : backend not running, or the frontend is not on http://localhost:5173 (add your URL to `app.cors-origins`).
- Port 8080 in use : change `server.port` and `VITE_API_URL`.
- Razorpay window does not open : the keys are empty or wrong, or the browser has no internet.
