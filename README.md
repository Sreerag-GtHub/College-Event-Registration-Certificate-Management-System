# College Event Registration & Certificate Management

A Java Swing desktop app for managing college events, student registrations, attendance, and participation certificates.

Built as an Object-Oriented Programming mini project using Java, JDBC, and MySQL.

## Features

- Student signup and login
- Admin login
- Create and manage events
- Student event registration with admin approval
- Attendance marking (Present / Absent)
- PDF participation certificates

## Tech Stack

- Java 17+
- Swing (FlatLaf look and feel)
- MySQL + JDBC
- Maven
- OpenPDF (certificate PDFs)

## Architecture

```
UI (Swing screens)
   ↓
Service (business rules)
   ↓
DAO (database access)
   ↓
Model (entities)
   ↓
MySQL
```

| Package | Role |
|---------|------|
| `model` | Entities like User, Student, Admin, Event, Registration |
| `dao` | JDBC queries for each entity |
| `service` | Login, registration, attendance, certificates |
| `ui` | Login, dashboards, and panels |
| `util` | DB connection, password hashing, PDF generation |

## OOP Concepts Used

### 1. Encapsulation
Private fields with public getters and setters.

| Where | What |
|-------|------|
| `User`, `Student`, `Admin` | user details, password hash |
| `Event` | title, date, capacity, status |
| `Registration`, `Attendance`, `Certificate` | related IDs and status fields |
| `DatabaseConnection` | DB URL, username, password kept private |

### 2. Abstraction
Hide details behind a simple interface / base class.

| Where | What |
|-------|------|
| `User` (abstract class) | common user data; subclasses fill in role behavior |
| DAO classes | UI/service call methods like `createEvent()` without writing SQL there |
| Service classes | screens call services; business rules stay out of the UI |

### 3. Inheritance
| Child | Parent | Why |
|-------|--------|-----|
| `Admin` | `User` | admin is a type of user |
| `Student` | `User` | student is a type of user (+ roll no, department, course) |
| `LoginFrame`, `AdminDashboard`, `StudentDashboard` | `JFrame` | Swing windows |
| Panels (`EventManagementPanel`, etc.) | `JPanel` | reusable UI sections |

### 4. Polymorphism
Same method call, different behavior based on object type.

| Where | What |
|-------|------|
| `User.getDashboardTitle()` | Admin → admin title; Student → student title |
| `User.getRoleDescription()` | different text per role |
| `User.hasAdminPrivileges()` | Admin → `true`; Student → `false` |
| `UserDAO.mapRowToUser()` | returns `Admin` or `Student` as a `User` |
| Login / dashboards | use a `User` reference; runtime picks Admin/Student methods |

### 5. Other OOP / Java concepts

| Concept | Where |
|---------|--------|
| Enum | `Role` (`ADMIN`, `STUDENT`) |
| Method overriding | `@Override` in `Admin`, `Student`, UI renderers |
| Method overloading | constructors in model classes (default + parameterized) |
| `instanceof` | check Student vs Admin in services and UI |
| Layered design | UI → Service → DAO → Model |
| Utility classes | `PasswordUtil`, `ValidationUtil`, `CertificateGenerator` |

## Exception Handling

### Custom exceptions
Defined in `AuthService`:

| Exception | When |
|-----------|------|
| `AuthenticationException` | wrong login / empty credentials |
| `ValidationException` | invalid signup data (email, password, duplicate ID, etc.) |

### Built-in exceptions used

| Exception | Where | Why |
|-----------|--------|-----|
| `SQLException` | DAO + services | DB errors, duplicate registration, full capacity, etc. |
| `SecurityException` | services | non-admin tries admin action, or student accesses another student's certificate |
| `IllegalArgumentException` | `EventService`, `PasswordUtil`, etc. | invalid input (empty title, bad capacity, null data) |
| `IllegalStateException` | `CertificateService` | certificate requested when not approved / not present |
| `ClassNotFoundException` | `DatabaseConnection` | JDBC driver missing |
| `IOException` / `DocumentException` | `CertificateGenerator` | PDF write / document errors |
| `RuntimeException` | `PasswordUtil` | hashing failure |

### How UI handles errors
Swing screens catch exceptions from services and show a message dialog (or status label on login), so the app does not crash on bad input or DB problems.

## Setup

1. Install Java 17+, Maven, and MySQL.
2. Create the database:
   ```bash
   mysql -u root -p < database/schema.sql
   ```
3. Edit `config/database.properties` with your MySQL username and password.
4. Create the first admin:
   ```bash
   mvn -q exec:java -Dexec.mainClass="com.college.event.util.AdminSetup"
   ```
5. Run the app:
   ```bash
   mvn -q exec:java
   ```

## Project Structure

```
config/                 database settings
database/schema.sql     MySQL tables
src/main/java/com/college/event/
  Main.java
  model/
  dao/
  service/
  ui/
  util/
pom.xml
```

## Roles

- **Admin** – manage events, approve registrations, mark attendance, issue certificates
- **Student** – register for events and download certificates after attendance is marked Present
