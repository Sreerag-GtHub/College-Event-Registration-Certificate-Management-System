package com.college.event.ui;

import com.college.event.model.User;
import com.college.event.service.EventService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class AdminDashboard extends JFrame {

    private final User currentUser;
    private final EventService eventService;

    private JLabel lblTotalStudents;
    private JLabel lblTotalEvents;
    private JLabel lblTotalRegs;
    private JLabel lblApprovedRegs;
    private JLabel lblStudentsPresent;

    private JTabbedPane tabbedPane;
    private EventManagementPanel eventPanel;
    private RegistrationPanel registrationPanel;
    private AttendancePanel attendancePanel;
    private CertificatePanel certificatePanel;

    public AdminDashboard(User currentUser) {
        this.currentUser = currentUser;
        this.eventService = new EventService();
        initComponents();
        refreshMetrics();
    }

    private void initComponents() {
        setTitle(currentUser.getDashboardTitle());
        setSize(1150, 780);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.BACKGROUND);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.PRIMARY);
        headerPanel.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel lblTitle = new JLabel(currentUser.getDashboardTitle());
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubtitle = new JLabel(currentUser.getRoleDescription());
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

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(headerPanel, BorderLayout.NORTH);
        northContainer.add(createMetricsPanel(), BorderLayout.SOUTH);
        mainPanel.add(northContainer, BorderLayout.NORTH);

        Runnable onDataChanged = this::refreshMetrics;

        eventPanel = new EventManagementPanel(currentUser, onDataChanged);
        registrationPanel = new RegistrationPanel(currentUser, onDataChanged);
        attendancePanel = new AttendancePanel(currentUser, onDataChanged);
        certificatePanel = new CertificatePanel(currentUser);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.addTab("Manage Events", eventPanel);
        tabbedPane.addTab("Student Registrations", registrationPanel);
        tabbedPane.addTab("Attendance Tracking", attendancePanel);
        tabbedPane.addTab("Certificate Issuance", certificatePanel);

        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == 1) registrationPanel.loadEventFilterOptions();
            if (idx == 2) attendancePanel.loadEvents();
            if (idx == 3) certificatePanel.loadEvents();
        });

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    private JPanel createMetricsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 12, 0));
        panel.setBackground(UITheme.BACKGROUND);
        panel.setBorder(new EmptyBorder(12, 20, 12, 20));

        lblTotalStudents = new JLabel("0", SwingConstants.CENTER);
        lblTotalEvents = new JLabel("0", SwingConstants.CENTER);
        lblTotalRegs = new JLabel("0", SwingConstants.CENTER);
        lblApprovedRegs = new JLabel("0", SwingConstants.CENTER);
        lblStudentsPresent = new JLabel("0", SwingConstants.CENTER);

        panel.add(createMetricCard("Enrolled Students", lblTotalStudents, UITheme.INFO));
        panel.add(createMetricCard("Total Events", lblTotalEvents, UITheme.PRIMARY_LIGHT));
        panel.add(createMetricCard("Registrations", lblTotalRegs, UITheme.WARNING.darker()));
        panel.add(createMetricCard("Approved", lblApprovedRegs, UITheme.SUCCESS));
        panel.add(createMetricCard("Students Present", lblStudentsPresent, UITheme.ACCENT));

        return panel;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = UITheme.createCardPanel();
        card.setLayout(new BorderLayout(4, 4));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_SMALL);
        titleLabel.setForeground(UITheme.TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void refreshMetrics() {
        SwingWorker<Map<String, Integer>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Integer> doInBackground() throws Exception {
                return eventService.getDashboardStatistics(currentUser);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Integer> stats = get();
                    lblTotalStudents.setText(String.valueOf(stats.getOrDefault("totalStudents", 0)));
                    lblTotalEvents.setText(String.valueOf(stats.getOrDefault("totalEvents", 0)));
                    lblTotalRegs.setText(String.valueOf(stats.getOrDefault("totalRegistrations", 0)));
                    lblApprovedRegs.setText(String.valueOf(stats.getOrDefault("approvedRegistrations", 0)));
                    lblStudentsPresent.setText(String.valueOf(stats.getOrDefault("studentsPresent", 0)));
                } catch (Exception ex) {
                    System.err.println("Could not update dashboard metrics: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out of the Administrator portal?",
            "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }
}
