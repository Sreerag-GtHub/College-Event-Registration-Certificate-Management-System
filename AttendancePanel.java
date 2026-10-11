package com.college.event.ui;

import com.college.event.model.Attendance;
import com.college.event.model.Event;
import com.college.event.model.User;
import com.college.event.service.AttendanceService;
import com.college.event.service.EventService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Administrator Panel for recording and managing student event attendance.
 */
public class AttendancePanel extends JPanel {

    private final User currentUser;
    private final AttendanceService attendanceService;
    private final EventService eventService;
    private final Runnable onDataChangedCallback;

    private JComboBox<EventItem> cmbEvents;
    private JTable tblParticipants;
    private DefaultTableModel modelParticipants;
    private JButton btnMarkPresent;
    private JButton btnMarkAbsent;
    private JButton btnRefresh;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public AttendancePanel(User currentUser, Runnable onDataChangedCallback) {
        this.currentUser = currentUser;
        this.attendanceService = new AttendanceService();
        this.eventService = new EventService();
        this.onDataChangedCallback = onDataChangedCallback;
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
        cmbEvents.addActionListener(e -> loadParticipants());
        toolbar.add(cmbEvents);

        btnMarkPresent = UITheme.createPrimaryButton("✓ Mark PRESENT");
        btnMarkAbsent = UITheme.createDangerButton("✗ Mark ABSENT");
        btnRefresh = UITheme.createSecondaryButton("Refresh");

        toolbar.add(btnMarkPresent);
        toolbar.add(btnMarkAbsent);
        toolbar.add(btnRefresh);

        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"Reg ID", "Student Name", "Roll / ID", "Department", "Attendance Status", "Recorded At", "Remarks"};
        modelParticipants = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tblParticipants = new JTable(modelParticipants);
        tblParticipants.setRowHeight(28);
        tblParticipants.setFont(UITheme.FONT_REGULAR);
        tblParticipants.getTableHeader().setFont(UITheme.FONT_BOLD);
        tblParticipants.getColumnModel().getColumn(4).setCellRenderer(new UITheme.StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(tblParticipants);
        add(scrollPane, BorderLayout.CENTER);

        // Action Listeners
        btnMarkPresent.addActionListener(e -> markSelectedAttendance("PRESENT"));
        btnMarkAbsent.addActionListener(e -> markSelectedAttendance("ABSENT"));
        btnRefresh.addActionListener(e -> {
            loadEvents();
            loadParticipants();
        });
    }

    public void loadEvents() {
        EventItem currentSelected = (EventItem) cmbEvents.getSelectedItem();
        cmbEvents.removeAllItems();

        try {
            List<Event> events = eventService.getAllEvents();
            for (Event ev : events) {
                EventItem item = new EventItem(ev.getEventId(), ev.getTitle());
                cmbEvents.addItem(item);
                if (currentSelected != null && currentSelected.id == ev.getEventId()) {
                    cmbEvents.setSelectedItem(item);
                }
            }
        } catch (Exception ex) {
            System.err.println("Could not load events for attendance: " + ex.getMessage());
        }
    }

    public void loadParticipants() {
        EventItem selected = (EventItem) cmbEvents.getSelectedItem();
        if (selected == null) {
            modelParticipants.setRowCount(0);
            return;
        }

        SwingWorker<List<Attendance>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Attendance> doInBackground() throws Exception {
                return attendanceService.getEventParticipants(currentUser, selected.id);
            }

            @Override
            protected void done() {
                try {
                    List<Attendance> list = get();
                    modelParticipants.setRowCount(0);
                    for (Attendance a : list) {
                        modelParticipants.addRow(new Object[]{
                            a.getRegistrationId(),
                            a.getStudentName(),
                            a.getStudentRollNo(),
                            a.getDepartment(),
                            a.getStatus(),
                            a.getMarkedAt() != null ? dateFormat.format(a.getMarkedAt()) : "Not Recorded",
                            a.getRemarks() != null ? a.getRemarks() : ""
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AttendancePanel.this,
                        "Failed to load attendance list: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void markSelectedAttendance(String status) {
        int row = tblParticipants.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an approved student participant from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int regId = (int) modelParticipants.getValueAt(row, 0);
        String studentName = (String) modelParticipants.getValueAt(row, 1);

        String remarks = JOptionPane.showInputDialog(
            this,
            "Enter optional remarks for marking " + studentName + " as " + status + ":",
            status.equals("PRESENT") ? "Verified On-Site" : "Unexcused Absence"
        );

        if (remarks == null) return;

        try {
            attendanceService.markAttendance(currentUser, regId, status, remarks);
            loadParticipants();
            if (onDataChangedCallback != null) onDataChangedCallback.run();

            JOptionPane.showMessageDialog(this, "Attendance recorded: " + studentName + " marked " + status,
                "Attendance Updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to mark attendance: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
