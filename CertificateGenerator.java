package com.college.event.util;

import com.college.event.model.Certificate;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility for generating professional, landscape participation certificates in PDF format.
 * Powered by OpenPDF.
 */
public class CertificateGenerator {

    private static final String DEFAULT_COLLEGE_NAME = "METROPOLITAN INSTITUTE OF TECHNOLOGY";
    private static final String DEFAULT_COLLEGE_TAGLINE = "Department of Student Affairs & Academic Activities";

    /**
     * Generates a landscape PDF participation certificate and writes it to the designated file.
     *
     * @param cert       Certificate model containing student, event, and issue details
     * @param targetFile Destination PDF file
     * @throws DocumentException if PDF structure error occurs
     * @throws IOException       if file I/O error occurs
     */
    public static void generateCertificatePdf(Certificate cert, File targetFile) throws DocumentException, IOException {
        if (cert == null || targetFile == null) {
            throw new IllegalArgumentException("Certificate data and destination file must not be null.");
        }

        // 1. Create Landscape A4 Document with margins
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(targetFile));

        // Add page border event
        writer.setPageEvent(new CertificateBorderEvent());

        document.open();

        // 2. Define Fonts & Palette
        Color primaryNavy = new Color(20, 35, 75);
        Color accentGold = new Color(185, 135, 30);
        Color darkGrey = new Color(50, 50, 50);

        Font fontHeaderCollege = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, primaryNavy);
        Font fontSubheader = FontFactory.getFont(FontFactory.HELVETICA, 11, accentGold);
        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, accentGold);
        Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 13, darkGrey);
        Font fontStudentName = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, primaryNavy);
        Font fontEventName = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, primaryNavy);
        Font fontMetadata = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Font fontSignatory = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, darkGrey);
        Font fontSignatoryTitle = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
        String formattedEventDate = cert.getEventDate() != null ? dateFormat.format(cert.getEventDate()) : "Date N/A";
        String formattedIssueDate = cert.getIssueDate() != null ? dateFormat.format(cert.getIssueDate()) : dateFormat.format(new Date());

        // 3. Document Content
        // Spacing
        Paragraph space1 = new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 10));
        document.add(space1);

        // College Name Header
        Paragraph collegeHeader = new Paragraph(DEFAULT_COLLEGE_NAME, fontHeaderCollege);
        collegeHeader.setAlignment(Element.ALIGN_CENTER);
        document.add(collegeHeader);

        // Subheader
        Paragraph collegeTagline = new Paragraph(DEFAULT_COLLEGE_TAGLINE, fontSubheader);
        collegeTagline.setAlignment(Element.ALIGN_CENTER);
        collegeTagline.setSpacingAfter(15);
        document.add(collegeTagline);

        // Certificate Title
        Paragraph title = new Paragraph("CERTIFICATE OF PARTICIPATION", fontTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(15);
        document.add(title);

        // Introductory statement
        Paragraph intro = new Paragraph("This is proudly presented to", fontBody);
        intro.setAlignment(Element.ALIGN_CENTER);
        intro.setSpacingAfter(6);
        document.add(intro);

        // Student Name
        String displayName = (cert.getStudentName() != null && !cert.getStudentName().isEmpty()) 
                             ? cert.getStudentName().toUpperCase() : "STUDENT PARTICIPANT";
        Paragraph studentPara = new Paragraph(displayName, fontStudentName);
        studentPara.setAlignment(Element.ALIGN_CENTER);
        studentPara.setSpacingAfter(4);
        document.add(studentPara);

        // Student Roll No & Department
        String studentInfo = "";
        if (cert.getStudentRollNo() != null && !cert.getStudentRollNo().isEmpty()) {
            studentInfo += "Student ID: " + cert.getStudentRollNo();
        }
        if (cert.getDepartment() != null && !cert.getDepartment().isEmpty()) {
            studentInfo += (studentInfo.isEmpty() ? "" : "  |  Department of ") + cert.getDepartment();
        }
        if (!studentInfo.isEmpty()) {
            Paragraph infoPara = new Paragraph(studentInfo, fontBody);
            infoPara.setAlignment(Element.ALIGN_CENTER);
            infoPara.setSpacingAfter(10);
            document.add(infoPara);
        }

        // Participation statement
        Paragraph statement = new Paragraph(
            "for active participation and successful completion of the college event",
            fontBody
        );
        statement.setAlignment(Element.ALIGN_CENTER);
        statement.setSpacingAfter(6);
        document.add(statement);

        // Event Name
        String eventName = cert.getEventTitle() != null ? cert.getEventTitle() : "College Event";
        Paragraph eventPara = new Paragraph("\"" + eventName + "\"", fontEventName);
        eventPara.setAlignment(Element.ALIGN_CENTER);
        eventPara.setSpacingAfter(6);
        document.add(eventPara);

        // Event Date & Category
        String eventDetails = "Organized on " + formattedEventDate;
        if (cert.getEventCategory() != null && !cert.getEventCategory().isEmpty()) {
            eventDetails += " under " + cert.getEventCategory() + " Activities";
        }
        Paragraph datePara = new Paragraph(eventDetails + ".", fontBody);
        datePara.setAlignment(Element.ALIGN_CENTER);
        datePara.setSpacingAfter(25);
        document.add(datePara);

        // 4. Signatory and Metadata Table
        PdfPTable footerTable = new PdfPTable(3);
        footerTable.setWidthPercentage(90);
        footerTable.setWidths(new float[]{35f, 30f, 35f});

        // Signatory 1: Event Convener
        PdfPCell cell1 = new PdfPCell();
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph sign1Line = new Paragraph("____________________________\n", fontSignatory);
        sign1Line.setAlignment(Element.ALIGN_CENTER);
        Paragraph sign1Title = new Paragraph("Event Coordinator\nFaculty Convener", fontSignatoryTitle);
        sign1Title.setAlignment(Element.ALIGN_CENTER);
        cell1.addElement(sign1Line);
        cell1.addElement(sign1Title);
        footerTable.addCell(cell1);

        // Center: Certificate Code & Issue Date
        PdfPCell cellCenter = new PdfPCell();
        cellCenter.setBorder(Rectangle.NO_BORDER);
        cellCenter.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph certIdPara = new Paragraph("Certificate ID: " + cert.getCertificateCode(), fontMetadata);
        certIdPara.setAlignment(Element.ALIGN_CENTER);
        Paragraph issueDatePara = new Paragraph("Issued: " + formattedIssueDate, fontMetadata);
        issueDatePara.setAlignment(Element.ALIGN_CENTER);
        cellCenter.addElement(certIdPara);
        cellCenter.addElement(issueDatePara);
        footerTable.addCell(cellCenter);

        // Signatory 2: Principal / Dean
        PdfPCell cell3 = new PdfPCell();
        cell3.setBorder(Rectangle.NO_BORDER);
        cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph sign2Line = new Paragraph("____________________________\n", fontSignatory);
        sign2Line.setAlignment(Element.ALIGN_CENTER);
        Paragraph sign2Title = new Paragraph("Principal / Dean\nHead of Institution", fontSignatoryTitle);
        sign2Title.setAlignment(Element.ALIGN_CENTER);
        cell3.addElement(sign2Line);
        cell3.addElement(sign2Title);
        footerTable.addCell(cell3);

        document.add(footerTable);

        // Close Document
        document.close();
    }

    /**
     * Inner class to draw a decorative double border around the certificate.
     */
    private static class CertificateBorderEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle rect = document.getPageSize();

            // Outer border
            canvas.setColorStroke(new Color(20, 35, 75));
            canvas.setLineWidth(4f);
            canvas.rectangle(rect.getLeft() + 15, rect.getBottom() + 15,
                             rect.getWidth() - 30, rect.getHeight() - 30);
            canvas.stroke();

            // Inner gold decorative border
            canvas.setColorStroke(new Color(185, 135, 30));
            canvas.setLineWidth(1.5f);
            canvas.rectangle(rect.getLeft() + 20, rect.getBottom() + 20,
                             rect.getWidth() - 40, rect.getHeight() - 40);
            canvas.stroke();
        }
    }
}
