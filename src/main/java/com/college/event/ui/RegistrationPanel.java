package com.college.event.ui;

import com.college.event.model.Event;
import com.college.event.model.Registration;
import com.college.event.model.User;
import com.college.event.service.EventService;
import com.college.event.service.RegistrationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class RegistrationPanel extends JPanel {

    private final User currentUser;
    private final RegistrationService registrationService;
    private final EventService eventService;
    private final Runnable onDataChangedCallback;

    private JTable tblRegistrations;
    private DefaultTableModel modelRegistrations;
    private JComboBox<EventFilterItem> cmbEventFilter;
    private JButton btnApprove;
    private JButton btnReject;
    private JButton btnRefresh;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public RegistrationPanel(User currentUser, Runnable onDataChangedCallback) {
        this.currentUser = currentUser;
        this.registrationService = new RegistrationService();
        this.eventService = new EventService();
        this.onDataChangedCallback = onDataChangedCallback;
        initComponents();
        loadEventFilterOptions();
        refreshRegistrations();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setOpaque(false);

        toolbar.add(new JLabel("Filter by Event:"));
        cmbEventFilter = new JComboBox<>();
        cmbEventFilter.setPreferredSize(new Dimension(280, 30));
        cmbEventFilter.addActionListener(e -> refreshRegistrations());
        toolbar.add(cmbEventFilter);

        btnApprove = UITheme.createPrimaryButton("✓ Approve Registration");
        btnReject = UITheme.createDangerButton("✗ Reject Registration");
        btnRefresh = UITheme.createSecondaryButton("Refresh");

        toolbar.add(btnApprove);
        toolbar.add(btnReject);
        toolbar.add(btnRefresh);

        add(toolbar, BorderLayout.NORTH);

        String[] columns = {
            "Reg ID", "Student Name", "Roll / ID", "Department", "Event Title",
            "Event Date", "Reg Date", "Status", "Attendance"
        };

        modelRegistrations = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tblRegistrations = new JTable(modelRegistrations);
        tblRegistrations.setRowHeight(28);
        tblRegistrations.setFont(UITheme.FONT_REGULAR);
        tblRegistrations.getTableHeader().setFont(UITheme.FONT_BOLD);
        tblRegistrations.getColumnModel().getColumn(7).setCellRenderer(new UITheme.StatusCellRenderer());
        tblRegistrations.getColumnModel().getColumn(8).setCellRenderer(new UITheme.StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblRegistrations);
        add(scrollPane, BorderLayout.CENTER);

        btnApprove.addActionListener(e -> updateSelectedRegistration(true));
        btnReject.addActionListener(e -> updateSelectedRegistration(false));
        btnRefresh.addActionListener(e -> {
            loadEventFilterOptions();
            refreshRegistrations();
        });
    }

    public void loadEventFilterOptions() {
        EventFilterItem currentSelected = (EventFilterItem) cmbEventFilter.getSelectedItem();
        cmbEventFilter.removeAllItems();
        cmbEventFilter.addItem(new EventFilterItem(0, "All Events"));

        try {
            List<Event> events = eventService.getAllEvents();
            for (Event ev : events) {
                EventFilterItem item = new EventFilterItem(ev.getEventId(), ev.getTitle());
                cmbEventFilter.addItem(item);
                if (currentSelected != null && currentSelected.id == ev.getEventId()) {
                    cmbEventFilter.setSelectedItem(item);
                }
            }
        } catch (Exception ex) {
            System.err.println("Could not load event filter options: " + ex.getMessage());
        }
    }

    public void refreshRegistrations() {
        EventFilterItem selected = (EventFilterItem) cmbEventFilter.getSelectedItem();
        Integer eventIdFilter = (selected != null && selected.id > 0) ? selected.id : null;

        SwingWorker<List<Registration>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Registration> doInBackground() throws Exception {
                return registrationService.getAllRegistrations(currentUser, eventIdFilter);
            }

            @Override
            protected void done() {
                try {
                    List<Registration> list = get();
                    modelRegistrations.setRowCount(0);
                    for (Registration r : list) {
                        modelRegistrations.addRow(new Object[]{
                            r.getRegistrationId(),
                            r.getStudentName(),
                            r.getStudentRollNo(),
                            r.getDepartment(),
                            r.getEventTitle(),
                            r.getEventDate() != null ? dateFormat.format(r.getEventDate()) : "N/A",
                            r.getRegistrationDate() != null ? dateFormat.format(r.getRegistrationDate()) : "N/A",
                            r.getStatus(),
                            r.getAttendanceStatus()
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RegistrationPanel.this,
                        "Failed to load registrations: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateSelectedRegistration(boolean approve) {
        int row = tblRegistrations.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a registration from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int regId = (int) modelRegistrations.getValueAt(row, 0);
        String studentName = (String) modelRegistrations.getValueAt(row, 1);
        String eventTitle = (String) modelRegistrations.getValueAt(row, 4);
        String currentStatus = (String) modelRegistrations.getValueAt(row, 7);

        String action = approve ? "APPROVE" : "REJECT";
        String remarks = JOptionPane.showInputDialog(
            this,
            "Enter optional remarks to " + action + " registration for:\n" +
            studentName + " - " + eventTitle + "\n(Current Status: " + currentStatus + ")",
            action.equals("APPROVE") ? "Registration Approved" : "Registration Rejected"
        );

        if (remarks == null) return;

        try {
            if (approve) {
                registrationService.approveRegistration(currentUser, regId, remarks);
            } else {
                registrationService.rejectRegistration(currentUser, regId, remarks);
            }

            refreshRegistrations();
            if (onDataChangedCallback != null) onDataChangedCallback.run();

            JOptionPane.showMessageDialog(this, "Registration " + action.toLowerCase() + "d successfully.",
                "Status Updated", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to update registration: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static class EventFilterItem {
        final int id;
        final String title;

        EventFilterItem(int id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
