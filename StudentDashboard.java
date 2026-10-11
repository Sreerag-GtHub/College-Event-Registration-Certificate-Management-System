package com.college.event.ui;

import com.college.event.model.Event;
import com.college.event.model.Registration;
import com.college.event.model.Student;
import com.college.event.model.User;
import com.college.event.service.CertificateService;
import com.college.event.service.EventService;
import com.college.event.service.RegistrationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Modern Swing Student Dashboard.
 * 
 * Demonstrates:
 * - Runtime Polymorphism: invoked via a polymorphic User reference (user.getDashboardTitle())
 * - Role separation: strictly student actions
 * - Real-time database event browsing, registration, and PDF certificate export
 */
public class StudentDashboard extends JFrame {

    private final User currentUser;
    private final EventService eventService;
    private final RegistrationService registrationService;
    private final CertificateService certificateService;

    private JTabbedPane tabbedPane;

    // Browse Events Tab
    private JTable tblEvents;
    private DefaultTableModel modelEvents;
    private JTextField txtSearch;
    private JComboBox<String> cmbCategory;
    private JCheckBox chkOnlyOpen;
    private JButton btnSearch;
    private JButton btnRegister;

    // My Registrations Tab
    private JTable tblRegistrations;
    private DefaultTableModel modelRegistrations;
    private JButton btnRefreshReg;

    // Certificates Tab
    private JTable tblCertificates;
    private DefaultTableModel modelCertificates;
    private JButton btnDownloadCert;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public StudentDashboard(User currentUser) {
        this.currentUser = currentUser;
        this.eventService = new EventService();
        this.registrationService = new RegistrationService();
        this.certificateService = new CertificateService();
        initComponents();
        loadAllData();
    }

    private void initComponents() {
        // OOP Polymorphic method call
        setTitle(currentUser.getDashboardTitle());
        setSize(1050, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.BACKGROUND);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.PRIMARY);
        headerPanel.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel(currentUser.getDashboardTitle());
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);

        String subtitleText = currentUser.getRoleDescription();
        if (currentUser instanceof Student s) {
            subtitleText += " | Dept: " + (s.getDepartment() != null ? s.getDepartment() : "General");
        }
        JLabel lblSubtitle = new JLabel(subtitleText);
        lblSubtitle.setFont(UITheme.FONT_SMALL);
        lblSubtitle.setForeground(new Color(210, 225, 245));

        JPanel headerTextPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerTextPanel.setOpaque(false);
        headerTextPanel.add(lblTitle);
        headerTextPanel.add(lblSubtitle);

        JButton btnLogout = UITheme.createSecondaryButton("Logout");
        btnLogout.addActionListener(e -> logout());

        headerPanel.add(headerTextPanel, BorderLayout.WEST);
        headerPanel.add(btnLogout, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);

        tabbedPane.addTab("Browse College Events", createBrowseEventsPanel());
        tabbedPane.addTab("My Registrations & Attendance", createMyRegistrationsPanel());
        tabbedPane.addTab("My Certificates", createCertificatesPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    // =========================================================================
    // Tab 1: Browse Events Panel
    // =========================================================================
    private JPanel createBrowseEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Filter Bar
        JPanel filterBar = UITheme.createCardPanel();
        filterBar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 6));

        filterBar.add(new JLabel("Search:"));
        txtSearch = new JTextField(16);
        filterBar.add(txtSearch);

        filterBar.add(new JLabel("Category:"));
        cmbCategory = new JComboBox<>(new String[]{
            "All Categories", "Technical", "Workshop", "Seminar", "Cultural", "Sports"
        });
        filterBar.add(cmbCategory);

        chkOnlyOpen = new JCheckBox("Open Events Only", true);
        chkOnlyOpen.setOpaque(false);
        filterBar.add(chkOnlyOpen);

        btnSearch = UITheme.createPrimaryButton("Filter Events");
        btnSearch.addActionListener(e -> refreshEvents());
        filterBar.add(btnSearch);

        panel.add(filterBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Event Title", "Category", "Venue", "Event Date", "Deadline", "Capacity", "Available Seats", "Status"};
        modelEvents = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tblEvents = new JTable(modelEvents);
        tblEvents.setRowHeight(28);
        tblEvents.setFont(UITheme.FONT_REGULAR);
        tblEvents.getTableHeader().setFont(UITheme.FONT_BOLD);
        tblEvents.getColumnModel().getColumn(8).setCellRenderer(new UITheme.StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblEvents);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);

        btnRegister = UITheme.createAccentButton("Register for Selected Event");
        btnRegister.addActionListener(e -> registerSelectedEvent());
        actionPanel.add(btnRegister);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // Tab 2: My Registrations Panel
    // =========================================================================
    private JPanel createMyRegistrationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] columns = {"Reg ID", "Event Title", "Event Date", "Registration Date", "Reg Status", "Attendance Status", "Certificate Status"};
        modelRegistrations = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tblRegistrations = new JTable(modelRegistrations);
        tblRegistrations.setRowHeight(28);
        tblRegistrations.setFont(UITheme.FONT_REGULAR);
        tblRegistrations.getTableHeader().setFont(UITheme.FONT_BOLD);
        tblRegistrations.getColumnModel().getColumn(4).setCellRenderer(new UITheme.StatusCellRenderer());
        tblRegistrations.getColumnModel().getColumn(5).setCellRenderer(new UITheme.StatusCellRenderer());
        tblRegistrations.getColumnModel().getColumn(6).setCellRenderer(new UITheme.StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblRegistrations);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action Toolbar
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);

        btnRefreshReg = UITheme.createSecondaryButton("Refresh My Registrations");
        btnRefreshReg.addActionListener(e -> refreshRegistrations());
        actionPanel.add(btnRefreshReg);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // Tab 3: My Certificates Panel
    // =========================================================================
    private JPanel createCertificatesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblNotice = new JLabel("Participation certificates are unlocked once your attendance is recorded as PRESENT.");
        lblNotice.setFont(UITheme.FONT_BOLD);
        lblNotice.setForeground(UITheme.PRIMARY);
        panel.add(lblNotice, BorderLayout.NORTH);

        String[] columns = {"Reg ID", "Event Title", "Event Date", "Attendance", "Certificate Code", "Downloadable"};
        modelCertificates = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tblCertificates = new JTable(modelCertificates);
        tblCertificates.setRowHeight(28);
        tblCertificates.setFont(UITheme.FONT_REGULAR);
        tblCertificates.getTableHeader().setFont(UITheme.FONT_BOLD);
        tblCertificates.getColumnModel().getColumn(3).setCellRenderer(new UITheme.StatusCellRenderer());
        tblCertificates.getColumnModel().getColumn(5).setCellRenderer(new UITheme.StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblCertificates);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);

        btnDownloadCert = UITheme.createPrimaryButton("Download PDF Certificate");
        btnDownloadCert.addActionListener(e -> downloadSelectedCertificate());
        actionPanel.add(btnDownloadCert);

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // Data Loading & Actions
    // =========================================================================

    private void loadAllData() {
        refreshEvents();
        refreshRegistrations();
    }

    private void refreshEvents() {
        String keyword = txtSearch != null ? txtSearch.getText().trim() : "";
        String category = cmbCategory != null ? (String) cmbCategory.getSelectedItem() : "All Categories";
        boolean onlyOpen = chkOnlyOpen != null && chkOnlyOpen.isSelected();

        SwingWorker<List<Event>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Event> doInBackground() throws Exception {
                return eventService.searchEvents(keyword, category, onlyOpen);
            }

            @Override
            protected void done() {
                try {
                    List<Event> events = get();
                    modelEvents.setRowCount(0);
                    for (Event ev : events) {
                        modelEvents.addRow(new Object[]{
                            ev.getEventId(),
                            ev.getTitle(),
                            ev.getCategory(),
                            ev.getVenue(),
                            ev.getEventDate() != null ? dateFormat.format(ev.getEventDate()) : "N/A",
                            ev.getRegistrationDeadline() != null ? dateFormat.format(ev.getRegistrationDeadline()) : "N/A",
                            ev.getMaxCapacity(),
                            ev.getAvailableSeats(),
                            ev.getStatus()
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentDashboard.this,
                        "Failed to load events: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void refreshRegistrations() {
        SwingWorker<List<Registration>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Registration> doInBackground() throws Exception {
                return registrationService.getStudentRegistrations(currentUser);
            }

            @Override
            protected void done() {
                try {
                    List<Registration> regs = get();
                    modelRegistrations.setRowCount(0);
                    modelCertificates.setRowCount(0);

                    for (Registration r : regs) {
                        String certStatus = r.isCertificateIssued() ? "ISSUED" : 
                            ("PRESENT".equalsIgnoreCase(r.getAttendanceStatus()) ? "ELIGIBLE" : "NOT ELIGIBLE");

                        modelRegistrations.addRow(new Object[]{
                            r.getRegistrationId(),
                            r.getEventTitle(),
                            r.getEventDate() != null ? dateFormat.format(r.getEventDate()) : "N/A",
                            r.getRegistrationDate() != null ? dateFormat.format(r.getRegistrationDate()) : "N/A",
                            r.getStatus(),
                            r.getAttendanceStatus(),
                            certStatus
                        });

                        // Populate Certificates tab
                        boolean canDownload = "PRESENT".equalsIgnoreCase(r.getAttendanceStatus()) 
                                           && "APPROVED".equalsIgnoreCase(r.getStatus());
                        modelCertificates.addRow(new Object[]{
                            r.getRegistrationId(),
                            r.getEventTitle(),
                            r.getEventDate() != null ? dateFormat.format(r.getEventDate()) : "N/A",
                            r.getAttendanceStatus(),
                            r.getCertificateCode() != null ? r.getCertificateCode() : "Pending Issuance",
                            canDownload ? "YES" : "NO"
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StudentDashboard.this,
                        "Failed to load registrations: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void registerSelectedEvent() {
        int selectedRow = tblEvents.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an event from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int eventId = (int) modelEvents.getValueAt(selectedRow, 0);
        String eventTitle = (String) modelEvents.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to register for:\n\"" + eventTitle + "\"?",
            "Confirm Event Registration",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        btnRegister.setEnabled(false);

        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return registrationService.registerForEvent(currentUser, eventId);
            }

            @Override
            protected void done() {
                btnRegister.setEnabled(true);
                try {
                    int regId = get();
                    JOptionPane.showMessageDialog(
                        StudentDashboard.this,
                        "Registration submitted successfully!\nRegistration ID: #" + regId +
                        "\nYour registration is recorded. Check 'My Registrations' for status updates.",
                        "Registration Successful",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    refreshEvents();
                    refreshRegistrations();
                    tabbedPane.setSelectedIndex(1); // Switch to My Registrations tab
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(
                        StudentDashboard.this,
                        cause.getMessage(),
                        "Registration Failed",
                        JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private void downloadSelectedCertificate() {
        int selectedRow = tblCertificates.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an event from the certificates list.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int regId = (int) modelCertificates.getValueAt(selectedRow, 0);
        String eventTitle = (String) modelCertificates.getValueAt(selectedRow, 1);
        String attendanceStatus = (String) modelCertificates.getValueAt(selectedRow, 3);
        String downloadable = (String) modelCertificates.getValueAt(selectedRow, 5);

        if (!"YES".equalsIgnoreCase(downloadable)) {
            JOptionPane.showMessageDialog(
                this,
                "Certificate is not available yet.\n" +
                "Attendance must be marked PRESENT by an administrator (Current status: " + attendanceStatus + ").",
                "Certificate Locked",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // File Chooser for destination
        JFileChooser chooser = new JFileChooser();
        String safeTitle = eventTitle.replaceAll("[^a-zA-Z0-9_-]", "_");
        chooser.setSelectedFile(new File("Certificate_" + safeTitle + ".pdf"));
        chooser.setDialogTitle("Save Participation Certificate PDF");

        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File targetFile = chooser.getSelectedFile();
        btnDownloadCert.setEnabled(false);

        SwingWorker<File, Void> worker = new SwingWorker<>() {
            @Override
            protected File doInBackground() throws Exception {
                return certificateService.generateAndSaveCertificate(currentUser, regId, targetFile);
            }

            @Override
            protected void done() {
                btnDownloadCert.setEnabled(true);
                try {
                    File savedPdf = get();
                    refreshRegistrations(); // refresh cert codes

                    int openChoice = JOptionPane.showConfirmDialog(
                        StudentDashboard.this,
                        "Certificate saved successfully to:\n" + savedPdf.getAbsolutePath() +
                        "\n\nWould you like to open the PDF certificate now?",
                        "Certificate Downloaded",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE
                    );

                    if (openChoice == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(savedPdf);
                    }

                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(
                        StudentDashboard.this,
                        "Error generating certificate: " + cause.getMessage(),
                        "Generation Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }
}
