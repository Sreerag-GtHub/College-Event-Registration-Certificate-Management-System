package com.college.event.ui;

import com.college.event.model.Student;
import com.college.event.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modern Swing Registration Screen for Enrolled Students.
 */
public class StudentRegistrationFrame extends JFrame {

    private final JFrame parentLoginFrame;
    private final AuthService authService;

    private JTextField txtFullName;
    private JTextField txtStudentId;
    private JTextField txtEmail;
    private JTextField txtDepartment;
    private JTextField txtCourse;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnSubmit;
    private JButton btnBack;
    private JLabel lblStatus;

    public StudentRegistrationFrame(JFrame parentLoginFrame) {
        this.parentLoginFrame = parentLoginFrame;
        this.authService = new AuthService();
        initComponents();
    }

    private void initComponents() {
        setTitle("Student Registration - College Event Portal");
        setSize(520, 680);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.BACKGROUND);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.PRIMARY);
        headerPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel lblTitle = new JLabel("New Student Registration", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubtitle = new JLabel("Create your account to browse and register for college events", SwingConstants.CENTER);
        lblSubtitle.setFont(UITheme.FONT_SMALL);
        lblSubtitle.setForeground(new Color(200, 215, 240));

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSubtitle, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Card
        JPanel cardPanel = UITheme.createCardPanel();
        cardPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 12, 6, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        int row = 0;

        // Full Name
        cardPanel.add(createFieldLabel("Full Name:"), setGbc(gbc, row++));
        txtFullName = new JTextField(20);
        txtFullName.setFont(UITheme.FONT_REGULAR);
        cardPanel.add(txtFullName, setGbc(gbc, row++));

        // Student ID
        cardPanel.add(createFieldLabel("Student ID / Roll No (e.g. CS2026-001):"), setGbc(gbc, row++));
        txtStudentId = new JTextField(20);
        txtStudentId.setFont(UITheme.FONT_REGULAR);
        cardPanel.add(txtStudentId, setGbc(gbc, row++));

        // Email
        cardPanel.add(createFieldLabel("College Email Address:"), setGbc(gbc, row++));
        txtEmail = new JTextField(20);
        txtEmail.setFont(UITheme.FONT_REGULAR);
        cardPanel.add(txtEmail, setGbc(gbc, row++));

        // Department
        cardPanel.add(createFieldLabel("Department (e.g. Computer Science):"), setGbc(gbc, row++));
        txtDepartment = new JTextField(20);
        txtDepartment.setFont(UITheme.FONT_REGULAR);
        cardPanel.add(txtDepartment, setGbc(gbc, row++));

        // Course
        cardPanel.add(createFieldLabel("Course / Degree (e.g. B.Tech CSE, MCA):"), setGbc(gbc, row++));
        txtCourse = new JTextField(20);
        txtCourse.setFont(UITheme.FONT_REGULAR);
        cardPanel.add(txtCourse, setGbc(gbc, row++));

        // Password
        cardPanel.add(createFieldLabel("Password (min 6 characters):"), setGbc(gbc, row++));
        txtPassword = new JPasswordField(20);
        txtPassword.setFont(UITheme.FONT_REGULAR);
        cardPanel.add(txtPassword, setGbc(gbc, row++));

        // Confirm Password
        cardPanel.add(createFieldLabel("Confirm Password:"), setGbc(gbc, row++));
        txtConfirmPassword = new JPasswordField(20);
        txtConfirmPassword.setFont(UITheme.FONT_REGULAR);
        cardPanel.add(txtConfirmPassword, setGbc(gbc, row++));

        // Status Label
        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.DANGER);
        cardPanel.add(lblStatus, setGbc(gbc, row++));

        // Action Buttons
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setOpaque(false);
        btnBack = UITheme.createSecondaryButton("Back to Login");
        btnSubmit = UITheme.createPrimaryButton("Complete Registration");
        btnPanel.add(btnBack);
        btnPanel.add(btnSubmit);
        cardPanel.add(btnPanel, setGbc(gbc, row++));

        JScrollPane scrollPane = new JScrollPane(cardPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(UITheme.BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(mainPanel);

        // Listeners
        btnBack.addActionListener(e -> returnToLogin());
        btnSubmit.addActionListener(e -> performRegistration());
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_REGULAR);
        lbl.setForeground(UITheme.TEXT_DARK);
        return lbl;
    }

    private GridBagConstraints setGbc(GridBagConstraints gbc, int row) {
        gbc.gridy = row;
        return gbc;
    }

    private void performRegistration() {
        String fullName = txtFullName.getText().trim();
        String studentId = txtStudentId.getText().trim();
        String email = txtEmail.getText().trim();
        String department = txtDepartment.getText().trim();
        String course = txtCourse.getText().trim();
        String password = new String(txtPassword.getPassword());
        String confirmPassword = new String(txtConfirmPassword.getPassword());

        btnSubmit.setEnabled(false);
        lblStatus.setForeground(UITheme.INFO);
        lblStatus.setText("Registering student in MySQL database...");

        SwingWorker<Student, Void> worker = new SwingWorker<>() {
            @Override
            protected Student doInBackground() throws Exception {
                return authService.registerStudent(fullName, studentId, email, department, course, password, confirmPassword);
            }

            @Override
            protected void done() {
                btnSubmit.setEnabled(true);
                try {
                    Student created = get();
                    JOptionPane.showMessageDialog(
                        StudentRegistrationFrame.this,
                        "Registration successful!\nYou can now log in using your Student ID: " + created.getStudentId(),
                        "Account Created",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    returnToLogin();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    lblStatus.setForeground(UITheme.DANGER);
                    lblStatus.setText(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void returnToLogin() {
        dispose();
        if (parentLoginFrame != null) {
            parentLoginFrame.setVisible(true);
        }
    }
}
