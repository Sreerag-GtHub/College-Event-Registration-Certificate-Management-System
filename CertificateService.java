package com.college.event.service;

import com.college.event.dao.AttendanceDAO;
import com.college.event.dao.CertificateDAO;
import com.college.event.dao.RegistrationDAO;
import com.college.event.model.Certificate;
import com.college.event.model.Registration;
import com.college.event.model.Student;
import com.college.event.model.User;
import com.college.event.util.CertificateGenerator;

import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Service managing Certificate eligibility verification, issuance, and PDF generation.
 */
public class CertificateService {

    private final CertificateDAO certificateDAO;
    private final RegistrationDAO registrationDAO;
    private final AttendanceDAO attendanceDAO;

    public CertificateService() {
        this.certificateDAO = new CertificateDAO();
        this.registrationDAO = new RegistrationDAO();
        this.attendanceDAO = new AttendanceDAO();
    }

    public CertificateService(CertificateDAO certificateDAO, RegistrationDAO registrationDAO, AttendanceDAO attendanceDAO) {
        this.certificateDAO = certificateDAO;
        this.registrationDAO = registrationDAO;
        this.attendanceDAO = attendanceDAO;
    }

    /**
     * Issues a certificate record in MySQL for an eligible student.
     * Enforces Administrator permissions.
     */
    public Certificate issueCertificate(User currentUser, int registrationId) throws SecurityException, SQLException {
        assertAdmin(currentUser);

        // Check if certificate already issued
        Certificate existing = certificateDAO.findByRegistrationId(registrationId);
        if (existing != null) {
            return existing; // Already issued
        }

        // Generate unique certificate ID
        String code = generateUniqueCertificateCode();

        // Retrieve registration details
        Registration reg = null;
        List<Registration> allRegs = registrationDAO.findAllWithDetails(null);
        for (Registration r : allRegs) {
            if (r.getRegistrationId() == registrationId) {
                reg = r;
                break;
            }
        }

        if (reg == null) {
            throw new SQLException("Registration record not found.");
        }

        Certificate cert = new Certificate();
        cert.setCertificateCode(code);
        cert.setRegistrationId(registrationId);
        cert.setStudentId(reg.getStudentId());
        cert.setEventId(reg.getEventId());
        cert.setIssueDate(new Timestamp(System.currentTimeMillis()));
        cert.setIssuedBy(currentUser.getUserId());

        boolean ok = certificateDAO.issueCertificate(cert);
        if (!ok) {
            throw new SQLException("Failed to issue certificate.");
        }

        return certificateDAO.findByRegistrationId(registrationId);
    }

    /**
     * Verifies eligibility, retrieves or creates certificate metadata, and exports PDF to user-selected file.
     * Can be invoked by Administrator or the enrolled Student themselves.
     */
    public File generateAndSaveCertificate(User currentUser, int registrationId, File destinationFile)
            throws SecurityException, Exception {

        if (currentUser == null) {
            throw new SecurityException("Authentication required.");
        }
        if (destinationFile == null) {
            throw new IllegalArgumentException("Destination file must be specified.");
        }

        // Verify registration ownership or admin privilege
        Registration targetReg = null;
        List<Registration> allRegs = registrationDAO.findAllWithDetails(null);
        for (Registration r : allRegs) {
            if (r.getRegistrationId() == registrationId) {
                targetReg = r;
                break;
            }
        }

        if (targetReg == null) {
            throw new SQLException("Registration record not found for ID: " + registrationId);
        }

        if (!currentUser.hasAdminPrivileges() && targetReg.getStudentId() != currentUser.getUserId()) {
            throw new SecurityException("Unauthorized: You cannot access certificates for other students.");
        }

        // Verify that attendance is explicitly marked PRESENT
        if (!"PRESENT".equalsIgnoreCase(targetReg.getAttendanceStatus())) {
            throw new IllegalStateException("Certificate unavailable: Attendance is not marked as PRESENT (Current status: " 
                + targetReg.getAttendanceStatus() + ").");
        }

        if (!"APPROVED".equalsIgnoreCase(targetReg.getStatus())) {
            throw new IllegalStateException("Certificate unavailable: Registration is not approved.");
        }

        // Retrieve or issue the Certificate record
        Certificate cert = certificateDAO.findByRegistrationId(registrationId);
        if (cert == null) {
            // Automatically issue the record if student meets all conditions
            String code = generateUniqueCertificateCode();
            cert = new Certificate();
            cert.setCertificateCode(code);
            cert.setRegistrationId(registrationId);
            cert.setStudentId(targetReg.getStudentId());
            cert.setEventId(targetReg.getEventId());
            cert.setIssueDate(new Timestamp(System.currentTimeMillis()));
            cert.setIssuedBy(currentUser.hasAdminPrivileges() ? currentUser.getUserId() : null);

            certificateDAO.issueCertificate(cert);
            cert = certificateDAO.findByRegistrationId(registrationId);
        }

        // Populate certificate metadata for rendering
        cert.setStudentName(targetReg.getStudentName());
        cert.setStudentRollNo(targetReg.getStudentRollNo());
        cert.setDepartment(targetReg.getDepartment());
        cert.setEventTitle(targetReg.getEventTitle());
        cert.setEventDate(targetReg.getEventDate());

        // Ensure file ends with .pdf
        String path = destinationFile.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) {
            destinationFile = new File(path + ".pdf");
        }

        // Generate PDF certificate
        CertificateGenerator.generateCertificatePdf(cert, destinationFile);

        return destinationFile;
    }

    /**
     * Retrieves all eligible participants for an event (APPROVED + PRESENT) for admin certificate management.
     */
    public List<Registration> getEligibleParticipants(User currentUser, int eventId) throws SecurityException, SQLException {
        assertAdmin(currentUser);
        return certificateDAO.findEligibleParticipants(eventId);
    }

    /**
     * Retrieves all certificates earned by the logged-in student.
     */
    public List<Certificate> getStudentCertificates(User currentUser) throws SecurityException, SQLException {
        if (currentUser == null || !(currentUser instanceof Student)) {
            throw new SecurityException("Only enrolled students can view their certificate records.");
        }
        return certificateDAO.findByStudentId(currentUser.getUserId());
    }

    private String generateUniqueCertificateCode() {
        String uuidPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int year = java.time.Year.now().getValue();
        return "CERT-" + year + "-" + uuidPart;
    }

    private void assertAdmin(User currentUser) {
        if (currentUser == null || !currentUser.hasAdminPrivileges()) {
            throw new SecurityException("Unauthorized: Administrator permissions are required.");
        }
    }
}
