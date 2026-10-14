package com.college.event.service;

import com.college.event.dao.UserDAO;
import com.college.event.model.Role;
import com.college.event.model.Student;
import com.college.event.model.User;
import com.college.event.util.PasswordUtil;
import com.college.event.util.ValidationUtil;

import java.sql.SQLException;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User login(String identifier, String password) throws AuthenticationException, SQLException {
        if (!ValidationUtil.isNonEmpty(identifier) || !ValidationUtil.isNonEmpty(password)) {
            throw new AuthenticationException("Username/email and password cannot be empty.");
        }

        User user = userDAO.findByUsernameOrEmail(identifier.trim());
        if (user == null) {
            throw new AuthenticationException("Invalid username or password.");
        }

        boolean passwordValid = PasswordUtil.verifyPassword(password, user.getPasswordHash());
        if (!passwordValid) {
            throw new AuthenticationException("Invalid username or password.");
        }

        return user;
    }

    public Student registerStudent(String fullName, String studentId, String email,
                                   String department, String course, String password,
                                   String confirmPassword) throws ValidationException, SQLException {

        if (!ValidationUtil.isNonEmpty(fullName)) {
            throw new ValidationException("Full name is required.");
        }
        if (!ValidationUtil.isNonEmpty(studentId)) {
            throw new ValidationException("Student ID (Roll Number) is required.");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ValidationException("A valid email address is required.");
        }
        if (!ValidationUtil.isNonEmpty(department)) {
            throw new ValidationException("Department is required.");
        }
        if (!ValidationUtil.isNonEmpty(course)) {
            throw new ValidationException("Course is required.");
        }

        if (!ValidationUtil.isValidPassword(password)) {
            throw new ValidationException("Password must be at least 6 characters in length.");
        }
        if (!ValidationUtil.passwordsMatch(password, confirmPassword)) {
            throw new ValidationException("Password and Confirm Password do not match.");
        }

        String cleanStudentId = studentId.trim();
        String cleanEmail = email.trim().toLowerCase();
        String username = cleanStudentId;

        if (userDAO.existsByStudentId(cleanStudentId)) {
            throw new ValidationException("Student ID '" + cleanStudentId + "' is already registered.");
        }
        if (userDAO.existsByEmail(cleanEmail)) {
            throw new ValidationException("Email '" + cleanEmail + "' is already registered.");
        }
        if (userDAO.existsByUsername(username)) {
            throw new ValidationException("Username '" + username + "' is already taken.");
        }

        String passwordHash = PasswordUtil.hashPassword(password);

        Student student = new Student();
        student.setFullName(fullName.trim());
        student.setUsername(username);
        student.setEmail(cleanEmail);
        student.setPasswordHash(passwordHash);
        student.setRole(Role.STUDENT);
        student.setStudentId(cleanStudentId);
        student.setDepartment(department.trim());
        student.setCourse(course.trim());

        boolean created = userDAO.createUser(student);
        if (!created) {
            throw new SQLException("Failed to create student account in database.");
        }

        return student;
    }

    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
