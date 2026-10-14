package com.college.event.ui;

import com.college.event.model.Role;
import com.college.event.model.User;
import com.college.event.service.AuthService;
import com.college.event.util.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LoginFrame extends JFrame {

    private final AuthService authService;

    private JTextField txtIdentifier;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;
    private JButton btnExit;
    private JLabel lblStatus;

    public LoginFrame() {
        this.authService = new AuthService();
        initComponents();
    }

    private void initComponents() {
        setTitle("College Event Registration & Certificate Management");
        setSize(460, 520);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.BACKGROUND);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.PRIMARY);
        headerPanel.setBorder(new EmptyBorder(25, 25, 20, 25));

        JLabel lblTitle = new JLabel("College Event Portal", SwingConstants.CENTER);
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubtitle = new JLabel("Event Registration & Certificate Management", SwingConstants.CENTER);
        lblSubtitle.setFont(UITheme.FONT_SMALL);
        lblSubtitle.setForeground(new Color(200, 215, 240));

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSubtitle, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel cardPanel = UITheme.createCardPanel();
        cardPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        int row = 0;

        JLabel lblInstruction = new JLabel("Please sign in to continue");
        lblInstruction.setFont(UITheme.FONT_BOLD);
        lblInstruction.setForeground(UITheme.TEXT_DARK);
        gbc.gridy = row++;
        cardPanel.add(lblInstruction, gbc);

        JLabel lblUser = new JLabel("Username or Email:");
        lblUser.setFont(UITheme.FONT_REGULAR);
        gbc.gridy = row++;
        cardPanel.add(lblUser, gbc);

        txtIdentifier = new JTextField(20);
        txtIdentifier.setFont(UITheme.FONT_REGULAR);
        txtIdentifier.setPreferredSize(new Dimension(300, 34));
        gbc.gridy = row++;
        cardPanel.add(txtIdentifier, gbc);

        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(UITheme.FONT_REGULAR);
        gbc.gridy = row++;
        cardPanel.add(lblPass, gbc);

        txtPassword = new JPasswordField(20);
        txtPassword.setFont(UITheme.FONT_REGULAR);
        txtPassword.setPreferredSize(new Dimension(300, 34));
        gbc.gridy = row++;
        cardPanel.add(txtPassword, gbc);

        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.DANGER);
        gbc.gridy = row++;
        cardPanel.add(lblStatus, gbc);

        btnLogin = UITheme.createPrimaryButton("Sign In");
        gbc.gridy = row++;
        cardPanel.add(btnLogin, gbc);

        btnRegister = UITheme.createSecondaryButton("Register as Student");
        gbc.gridy = row++;
        cardPanel.add(btnRegister, gbc);

        btnExit = new JButton("Exit Application");
        btnExit.setFont(UITheme.FONT_SMALL);
        btnExit.setForeground(UITheme.TEXT_MUTED);
        btnExit.setContentAreaFilled(false);
        btnExit.setBorderPainted(false);
        btnExit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridy = row++;
        cardPanel.add(btnExit, gbc);

        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        centerWrapper.setBackground(UITheme.BACKGROUND);
        centerWrapper.add(cardPanel);
        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        setContentPane(mainPanel);

        btnLogin.addActionListener(e -> performLogin());
        btnRegister.addActionListener(e -> openStudentRegistration());
        btnExit.addActionListener(e -> System.exit(0));

        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        txtIdentifier.addKeyListener(enterListener);
        txtPassword.addKeyListener(enterListener);
    }

    private void performLogin() {
        String identifier = txtIdentifier.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (identifier.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Please enter both username and password.");
            return;
        }

        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);
        lblStatus.setForeground(UITheme.INFO);
        lblStatus.setText("Authenticating with MySQL...");

        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() throws Exception {
                return authService.login(identifier, password);
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                btnRegister.setEnabled(true);
                try {
                    User user = get();
                    lblStatus.setText(" ");
                    dispose();

                    if (user.hasAdminPrivileges()) {
                        SwingUtilities.invokeLater(() -> new AdminDashboard(user).setVisible(true));
                    } else {
                        SwingUtilities.invokeLater(() -> new StudentDashboard(user).setVisible(true));
                    }

                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    lblStatus.setForeground(UITheme.DANGER);
                    lblStatus.setText(cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void openStudentRegistration() {
        new StudentRegistrationFrame(this).setVisible(true);
        setVisible(false);
    }
}
