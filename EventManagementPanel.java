package com.college.event.ui;

import com.college.event.model.Event;
import com.college.event.model.User;
import com.college.event.service.EventService;
import com.college.event.util.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Administrator Panel for Event CRUD operations and status management.
 */
public class EventManagementPanel extends JPanel {

    private final User currentUser;
    private final EventService eventService;
    private final Runnable onDataChangedCallback;

    private JTable tblEvents;
    private DefaultTableModel modelEvents;
    private JButton btnAdd;
    private JButton btnEdit;
    private JButton btnDelete;
    private JButton btnToggleStatus;
    private JButton btnRefresh;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public EventManagementPanel(User currentUser, Runnable onDataChangedCallback) {
        this.currentUser = currentUser;
        this.eventService = new EventService();
        this.onDataChangedCallback = onDataChangedCallback;
        initComponents();
        refreshEvents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolbar.setOpaque(false);

        btnAdd = UITheme.createPrimaryButton("+ Add New Event");
        btnEdit = UITheme.createSecondaryButton("Edit Selected Event");
        btnDelete = UITheme.createDangerButton("Delete Event");
        btnToggleStatus = UITheme.createSecondaryButton("Close / Open Event");
        btnRefresh = UITheme.createSecondaryButton("Refresh");

        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnToggleStatus);
        toolbar.add(btnDelete);
        toolbar.add(btnRefresh);

        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {
            "ID", "Title", "Category", "Venue", "Event Date", "Deadline",
            "Capacity", "Approved", "Status", "Approval Req?", "Cert Eligible?"
        };

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
        add(scrollPane, BorderLayout.CENTER);

        // Event Listeners
        btnAdd.addActionListener(e -> showEventDialog(null));
        btnEdit.addActionListener(e -> editSelectedEvent());
        btnDelete.addActionListener(e -> deleteSelectedEvent());
        btnToggleStatus.addActionListener(e -> toggleEventStatus());
        btnRefresh.addActionListener(e -> refreshEvents());
    }

    public void refreshEvents() {
        SwingWorker<List<Event>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Event> doInBackground() throws Exception {
                return eventService.getAllEvents();
            }

            @Override
            protected void done() {
                try {
                    List<Event> list = get();
                    modelEvents.setRowCount(0);
                    for (Event ev : list) {
                        modelEvents.addRow(new Object[]{
                            ev.getEventId(),
                            ev.getTitle(),
                            ev.getCategory(),
                            ev.getVenue(),
                            ev.getEventDate() != null ? dateFormat.format(ev.getEventDate()) : "N/A",
                            ev.getRegistrationDeadline() != null ? dateFormat.format(ev.getRegistrationDeadline()) : "N/A",
                            ev.getMaxCapacity(),
                            ev.getApprovedCount(),
                            ev.getStatus(),
                            ev.isRequiresApproval() ? "YES" : "NO",
                            ev.isCertificateEligible() ? "YES" : "NO"
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(EventManagementPanel.this,
                        "Failed to load events: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void editSelectedEvent() {
        int row = tblEvents.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an event to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int eventId = (int) modelEvents.getValueAt(row, 0);
        try {
            Event event = eventService.getEventById(eventId);
            if (event != null) {
                showEventDialog(event);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error retrieving event details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showEventDialog(Event existingEvent) {
        boolean isEdit = existingEvent != null;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            isEdit ? "Edit Event - ID #" + existingEvent.getEventId() : "Create New College Event", true);
        dialog.setSize(520, 600);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;

        JTextField txtTitle = new JTextField(isEdit ? existingEvent.getTitle() : "", 20);
        JTextField txtVenue = new JTextField(isEdit ? existingEvent.getVenue() : "", 20);
        JComboBox<String> cmbCat = new JComboBox<>(new String[]{"Technical", "Workshop", "Seminar", "Cultural", "Sports"});
        if (isEdit && existingEvent.getCategory() != null) cmbCat.setSelectedItem(existingEvent.getCategory());

        JTextArea txtDesc = new JTextArea(isEdit ? existingEvent.getDescription() : "", 3, 20);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(txtDesc);

        // Date fields with standard formatting example
        String defaultDateStr = dateFormat.format(new Date(System.currentTimeMillis() + 7L * 24 * 3600 * 1000));
        String defaultDeadlineStr = dateFormat.format(new Date(System.currentTimeMillis() + 5L * 24 * 3600 * 1000));

        JTextField txtDate = new JTextField(isEdit && existingEvent.getEventDate() != null ? dateFormat.format(existingEvent.getEventDate()) : defaultDateStr, 20);
        JTextField txtDeadline = new JTextField(isEdit && existingEvent.getRegistrationDeadline() != null ? dateFormat.format(existingEvent.getRegistrationDeadline()) : defaultDeadlineStr, 20);
        JTextField txtCapacity = new JTextField(isEdit ? String.valueOf(existingEvent.getMaxCapacity()) : "50", 20);

        JCheckBox chkApproval = new JCheckBox("Requires Administrator Approval", isEdit ? existingEvent.isRequiresApproval() : true);
        chkApproval.setOpaque(false);
        JCheckBox chkCert = new JCheckBox("Eligible for Participation Certificate", isEdit ? existingEvent.isCertificateEligible() : true);
        chkCert.setOpaque(false);

        panel.add(new JLabel("Event Title:"), setGbc(gbc, row++));
        panel.add(txtTitle, setGbc(gbc, row++));

        panel.add(new JLabel("Category:"), setGbc(gbc, row++));
        panel.add(cmbCat, setGbc(gbc, row++));

        panel.add(new JLabel("Venue / Location:"), setGbc(gbc, row++));
        panel.add(txtVenue, setGbc(gbc, row++));

        panel.add(new JLabel("Event Date & Time (YYYY-MM-DD HH:mm):"), setGbc(gbc, row++));
        panel.add(txtDate, setGbc(gbc, row++));

        panel.add(new JLabel("Registration Deadline (YYYY-MM-DD HH:mm):"), setGbc(gbc, row++));
        panel.add(txtDeadline, setGbc(gbc, row++));

        panel.add(new JLabel("Maximum Seat Capacity:"), setGbc(gbc, row++));
        panel.add(txtCapacity, setGbc(gbc, row++));

        panel.add(new JLabel("Description:"), setGbc(gbc, row++));
        panel.add(descScroll, setGbc(gbc, row++));

        panel.add(chkApproval, setGbc(gbc, row++));
        panel.add(chkCert, setGbc(gbc, row++));

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setOpaque(false);
        JButton btnCancel = UITheme.createSecondaryButton("Cancel");
        JButton btnSave = UITheme.createPrimaryButton(isEdit ? "Update Event" : "Create Event");

        btnCancel.addActionListener(e -> dialog.dispose());
        btnSave.addActionListener(e -> {
            try {
                String title = txtTitle.getText().trim();
                String venue = txtVenue.getText().trim();
                String category = (String) cmbCat.getSelectedItem();
                String desc = txtDesc.getText().trim();

                Date parsedDate = dateFormat.parse(txtDate.getText().trim());
                Date parsedDeadline = dateFormat.parse(txtDeadline.getText().trim());
                Timestamp eventTs = new Timestamp(parsedDate.getTime());
                Timestamp deadlineTs = new Timestamp(parsedDeadline.getTime());

                int capacity = Integer.parseInt(txtCapacity.getText().trim());

                Event ev = isEdit ? existingEvent : new Event();
                ev.setTitle(title);
                ev.setVenue(venue);
                ev.setCategory(category);
                ev.setDescription(desc);
                ev.setEventDate(eventTs);
                ev.setRegistrationDeadline(deadlineTs);
                ev.setMaxCapacity(capacity);
                ev.setRequiresApproval(chkApproval.isSelected());
                ev.setCertificateEligible(chkCert.isSelected());

                if (isEdit) {
                    eventService.updateEvent(currentUser, ev);
                } else {
                    eventService.createEvent(currentUser, ev);
                }

                dialog.dispose();
                refreshEvents();
                if (onDataChangedCallback != null) onDataChangedCallback.run();

                JOptionPane.showMessageDialog(this, isEdit ? "Event updated successfully!" : "Event created successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Validation error: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void deleteSelectedEvent() {
        int row = tblEvents.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an event to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int eventId = (int) modelEvents.getValueAt(row, 0);
        String eventTitle = (String) modelEvents.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete the event:\n\"" + eventTitle + "\" (ID #" + eventId + ")?\n" +
            "Warning: Any associated registrations, attendance, and certificates will also be removed.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            eventService.deleteEvent(currentUser, eventId);
            refreshEvents();
            if (onDataChangedCallback != null) onDataChangedCallback.run();
            JOptionPane.showMessageDialog(this, "Event deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete event: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleEventStatus() {
        int row = tblEvents.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an event to update status.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int eventId = (int) modelEvents.getValueAt(row, 0);
        String currentStatus = (String) modelEvents.getValueAt(row, 8);
        String newStatus = "OPEN".equalsIgnoreCase(currentStatus) ? "CLOSED" : "OPEN";

        try {
            eventService.changeEventStatus(currentUser, eventId, newStatus);
            refreshEvents();
            if (onDataChangedCallback != null) onDataChangedCallback.run();
            JOptionPane.showMessageDialog(this, "Event status changed to: " + newStatus, "Status Updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating status: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private GridBagConstraints setGbc(GridBagConstraints gbc, int row) {
        gbc.gridy = row;
        return gbc;
    }
}
