package com.college.event.service;

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
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class CertificateService {

    private final CertificateDAO certificateDAO;
    private final RegistrationDAO registrationDAO;
    private final Random random = new Random();

    public CertificateService() {
        this.certificateDAO = new CertificateDAO();
        this.registrationDAO = new RegistrationDAO();
    }

    public CertificateService(CertificateDAO certificateDAO, RegistrationDAO registrationDAO) {
        this.certificateDAO = certificateDAO;
        this.registrationDAO = registrationDAO;
    }

    public Certificate issueCertificate(User currentUser, int registrationId) throws SecurityException, SQLException {
        assertAdmin(currentUser);

        Certificate existing = certificateDAO.findByRegistrationId(registrationId);
        if (existing != null) {
            return existing;
        }

        Registration reg = registrationDAO.findById(registrationId);
        if (reg == null) {
            throw new SQLException("Registration record not found.");
        }

        Certificate cert = new Certificate();
        cert.setCertificateCode(generateUniqueCertificateCode());
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

    public File generateAndSaveCertificate(User currentUser, int registrationId, File destinationFile)
            throws SecurityException, Exception {

        if (currentUser == null) {
            throw new SecurityException("Authentication required.");
        }
        if (destinationFile == null) {
            throw new IllegalArgumentException("Destination file must be specified.");
        }

        Registration targetReg = registrationDAO.findById(registrationId);
        if (targetReg == null) {
            throw new SQLException("Registration record not found for ID: " + registrationId);
        }

        if (!currentUser.hasAdminPrivileges() && targetReg.getStudentId() != currentUser.getUserId()) {
            throw new SecurityException("Unauthorized: You cannot access certificates for other students.");
        }

        if (!"PRESENT".equalsIgnoreCase(targetReg.getAttendanceStatus())) {
            throw new IllegalStateException("Certificate unavailable: Attendance is not marked as PRESENT.");
        }

        if (!"APPROVED".equalsIgnoreCase(targetReg.getStatus())) {
            throw new IllegalStateException("Certificate unavailable: Registration is not approved.");
        }

        Certificate cert = certificateDAO.findByRegistrationId(registrationId);
        if (cert == null) {
            cert = new Certificate();
            cert.setCertificateCode(generateUniqueCertificateCode());
            cert.setRegistrationId(registrationId);
            cert.setStudentId(targetReg.getStudentId());
            cert.setEventId(targetReg.getEventId());
            cert.setIssueDate(new Timestamp(System.currentTimeMillis()));
            cert.setIssuedBy(currentUser.hasAdminPrivileges() ? currentUser.getUserId() : null);
            certificateDAO.issueCertificate(cert);
            cert = certificateDAO.findByRegistrationId(registrationId);
        }

        cert.setStudentName(targetReg.getStudentName());
        cert.setStudentRollNo(targetReg.getStudentRollNo());
        cert.setDepartment(targetReg.getDepartment());
        cert.setEventTitle(targetReg.getEventTitle());
        cert.setEventDate(targetReg.getEventDate());

        String path = destinationFile.getAbsolutePath();
        if (!path.toLowerCase().endsWith(".pdf")) {
            destinationFile = new File(path + ".pdf");
        }

        CertificateGenerator.generateCertificatePdf(cert, destinationFile);
        return destinationFile;
    }

    public List<Registration> getEligibleParticipants(User currentUser, int eventId) throws SecurityException, SQLException {
        assertAdmin(currentUser);
        return certificateDAO.findEligibleParticipants(eventId);
    }

    public List<Certificate> getStudentCertificates(User currentUser) throws SecurityException, SQLException {
        if (currentUser == null || !(currentUser instanceof Student)) {
            throw new SecurityException("Only enrolled students can view their certificate records.");
        }
        return certificateDAO.findByStudentId(currentUser.getUserId());
    }

    private String generateUniqueCertificateCode() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int number = 10000000 + random.nextInt(90000000);
        return "CERT-" + year + "-" + number;
    }

    private void assertAdmin(User currentUser) {
        if (currentUser == null || !currentUser.hasAdminPrivileges()) {
            throw new SecurityException("Unauthorized: Administrator permissions are required.");
        }
    }
}
