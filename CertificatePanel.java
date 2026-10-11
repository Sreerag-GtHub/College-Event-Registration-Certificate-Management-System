package com.college.event.ui;

import com.college.event.model.Event;
import com.college.event.model.Registration;
import com.college.event.model.User;
import com.college.event.service.CertificateService;
import com.college.event.service.EventService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Administrator Panel for managing eligible certificate recipients, issuing records,
 * and exporting certificates to PDF.
 */
public class CertificatePanel extends JPanel {

    private final User currentUser;
    private final CertificateService certificateService;
    private final EventService eventService;

    private JComboBox<EventItem> cmbEvents;
    private JTable tblEligible;
    private DefaultTableModel modelEligible;
    private JButton btnIssueCert;
    private JButton btnDownloadPdf;
    private JButton btnRefresh;

    public CertificatePanel(User currentUser) {
        this.currentUser = currentUser;
        this.certificateService = new CertificateService();
        this.eventService = new EventService();
        initComponents();
        loadEvents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setOpaque(false);

        toolbar.add(new JLabel("Select Event:"));
        cmbEvents = new JComboBox<>();
        cmbEvents.setPreferredSize(new Dimension(320, 30));
        cmbEvents.addActionListener(e -> loadEligibleParticipants());
        toolbar.add(cmbEvents);

        btnIssueCert = UITheme.createPrimaryButton("Issue Certificate");
        btnDownloadPdf = UITheme.createAccentButton("Export Certificate PDF");
        btnRefresh = UITheme.createSecondaryButton("Refresh");

        toolbar.add(btnIssueCert);
        toolbar.add(btnDownloadPdf);
        toolbar.add(btnRefresh);

        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"Reg ID", "Student Name", "Roll / ID", "Department", "Attendance", "Certificate Status", "Certificate Code"};
        modelEligible = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tblEligible = new JTable(modelEligible);
        tblEligible.setRowHeight(28);
        tblEligible.setFont(UITheme.FONT_REGULAR);
        tblEligible.getTableHeader().setFont(UITheme.FONT_BOLD);
        tblEligible.getColumnModel().getColumn(4).setCellRenderer(new UITheme.StatusCellRenderer());
        tblEligible.getColumnModel().getColumn(5).setCellRenderer(new UITheme.StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblEligible);
        add(scrollPane, BorderLayout.CENTER);

        // Action Listeners
        btnIssueCert.addActionListener(e -> issueCertificateForSelected());
        btnDownloadPdf.addActionListener(e -> exportCertificatePdfForSelected());
        btnRefresh.addActionListener(e -> {
            loadEvents();
            loadEligibleParticipants();
        });
    }

    public void loadEvents() {
        EventItem currentSelected = (EventItem) cmbEvents.getSelectedItem();
        cmbEvents.removeAllItems();

        try {
            List<Event> events = eventService.getAllEvents();
            for (Event ev : events) {
                if (ev.isCertificateEligible()) {
                    EventItem item = new EventItem(ev.getEventId(), ev.getTitle());
                    cmbEvents.addItem(item);
                    if (currentSelected != null && currentSelected.id == ev.getEventId()) {
                        cmbEvents.setSelectedItem(item);
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Could not load events for certificates: " + ex.getMessage());
        }
    }

    public void loadEligibleParticipants() {
        EventItem selected = (EventItem) cmbEvents.getSelectedItem();
        if (selected == null) {
            modelEligible.setRowCount(0);
            return;
        }

        SwingWorker<List<Registration>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Registration> doInBackground() throws Exception {
                return certificateService.getEligibleParticipants(currentUser, selected.id);
            }

            @Override
            protected void done() {
                try {
                    List<Registration> list = get();
                    modelEligible.setRowCount(0);
                    for (Registration r : list) {
                        modelEligible.addRow(new Object[]{
                            r.getRegistrationId(),
                            r.getStudentName(),
                            r.getStudentRollNo(),
                            r.getDepartment(),
                            r.getAttendanceStatus(),
                            r.isCertificateIssued() ? "ISSUED" : "ELIGIBLE",
                            r.getCertificateCode() != null ? r.getCertificateCode() : "Not Generated Yet"
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CertificatePanel.this,
                        "Failed to load eligible participants: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void issueCertificateForSelected() {
        int row = tblEligible.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an eligible student from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int regId = (int) modelEligible.getValueAt(row, 0);
        String studentName = (String) modelEligible.getValueAt(row, 1);
        String status = (String) modelEligible.getValueAt(row, 5);

        if ("ISSUED".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "A certificate has already been issued for " + studentName + ".\nUse 'Export Certificate PDF' to view or re-download.", "Already Issued", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            certificateService.issueCertificate(currentUser, regId);
            loadEligibleParticipants();
            JOptionPane.showMessageDialog(this, "Certificate successfully generated and recorded for: " + studentName,
                "Certificate Issued", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error issuing certificate: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCertificatePdfForSelected() {
        int row = tblEligible.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int regId = (int) modelEligible.getValueAt(row, 0);
        String studentName = (String) modelEligible.getValueAt(row, 1);
        EventItem selectedEvent = (EventItem) cmbEvents.getSelectedItem();
        String eventTitle = selectedEvent != null ? selectedEvent.title : "Event";

        JFileChooser chooser = new JFileChooser();
        String safeName = (studentName + "_" + eventTitle).replaceAll("[^a-zA-Z0-9_-]", "_");
        chooser.setSelectedFile(new File("Certificate_" + safeName + ".pdf"));
        chooser.setDialogTitle("Export Certificate PDF");

        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) return;

        File targetFile = chooser.getSelectedFile();
        btnDownloadPdf.setEnabled(false);

        SwingWorker<File, Void> worker = new SwingWorker<>() {
            @Override
            protected File doInBackground() throws Exception {
                return certificateService.generateAndSaveCertificate(currentUser, regId, targetFile);
            }

            @Override
            protected void done() {
                btnDownloadPdf.setEnabled(true);
                try {
                    File saved = get();
                    loadEligibleParticipants();

                    int openChoice = JOptionPane.showConfirmDialog(
                        CertificatePanel.this,
                        "Certificate successfully exported to:\n" + saved.getAbsolutePath() +
                        "\n\nWould you like to open the PDF file now?",
                        "Export Complete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                    );

                    if (openChoice == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(saved);
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CertificatePanel.this,
                        "Error saving certificate PDF: " + ex.getMessage(), "Export Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private static class EventItem {
        final int id;
        final String title;

        EventItem(int id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
