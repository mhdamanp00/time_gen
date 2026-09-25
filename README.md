# Automatic Class Timetable and Schedule Generator

A Java 17 + JavaFX desktop application for automatically generating and managing class timetables for educational institutions.

---

## 📋 Features

| Feature | Description |
|---------|-------------|
| **Admin Portal** | Full CRUD for Subjects, Teachers, Classrooms, Working Days, Time Periods |
| **Schedule Management** | Create schedule entries with real-time conflict detection |
| **Conflict Detection** | Blocks saving if teacher or classroom is double-booked at same Day+Period |
| **Auto-Generation** | Constraint-aware greedy algorithm auto-fills the timetable |
| **User Portal** | Read-only search by Class, Teacher, Subject, Day |
| **PDF Export** | Tabular A4 landscape PDF with Day×Period grid per class/section |

---

## 🛠️ Tech Stack

- **Java 17** — core language
- **JavaFX 21** — desktop GUI (FXML-based views)
- **Hibernate ORM 6.4** — persistence layer (Session/SessionFactory)
- **MySQL 8.x** — relational database
- **Apache PDFBox 3.0** — PDF export
- **Maven** — build tool

---

## 📁 Project Structure

```
TimetableScheduler/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── com/timetable/
        │       ├── App.java                  ← JavaFX entry point
        │       ├── Launcher.java             ← IDE-friendly launcher
        │       ├── model/                    ← JPA @Entity classes
        │       │   ├── Subject.java
        │       │   ├── Teacher.java
        │       │   ├── Classroom.java
        │       │   ├── WorkingDay.java
        │       │   ├── TimePeriod.java
        │       │   └── ClassSchedule.java
        │       ├── dao/                      ← Hibernate DAO layer
        │       │   ├── HibernateUtil.java
        │       │   ├── GenericDAO.java
        │       │   ├── SubjectDAO.java
        │       │   ├── TeacherDAO.java
        │       │   ├── ClassroomDAO.java
        │       │   ├── WorkingDayDAO.java
        │       │   ├── TimePeriodDAO.java
        │       │   └── ClassScheduleDAO.java
        │       ├── service/                  ← Business logic + conflict detection
        │       │   ├── SubjectService.java
        │       │   ├── TeacherService.java
        │       │   ├── ClassroomService.java
        │       │   ├── WorkingDayService.java
        │       │   ├── TimePeriodService.java
        │       │   ├── ScheduleService.java
        │       │   ├── ConflictException.java
        │       │   └── TimetableGenerator.java
        │       ├── controller/               ← JavaFX controllers
        │       │   ├── MainController.java
        │       │   ├── AdminDashboardController.java
        │       │   ├── SubjectController.java
        │       │   ├── TeacherController.java
        │       │   ├── ClassroomController.java
        │       │   ├── WorkingDayController.java
        │       │   ├── TimePeriodController.java
        │       │   ├── ScheduleController.java
        │       │   ├── AutoGenerateController.java
        │       │   └── UserDashboardController.java
        │       └── util/
        │           └── PDFExportUtil.java    ← PDFBox timetable export
        └── resources/
            ├── hibernate.cfg.xml
            ├── schema.sql
            ├── css/
            │   └── styles.css
            └── fxml/
                ├── MainView.fxml
                ├── AdminDashboard.fxml
                ├── SubjectView.fxml
                ├── TeacherView.fxml
                ├── ClassroomView.fxml
                ├── WorkingDayView.fxml
                ├── TimePeriodView.fxml
                ├── ScheduleView.fxml
                ├── AutoGenerateView.fxml
                └── UserDashboard.fxml
```

---

## ⚙️ Setup Instructions

### 1. Prerequisites

- Java 17 JDK installed ([download](https://adoptium.net/))
- Maven 3.8+ installed ([download](https://maven.apache.org/))
- MySQL 8.x running locally

### 2. Create the Database

```sql
CREATE DATABASE timetable_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

Or run the provided schema file:

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 3. Configure Database Credentials

Edit `src/main/resources/hibernate.cfg.xml`:

```xml
<property name="hibernate.connection.url">
    jdbc:mysql://localhost:3306/timetable_db?useSSL=false&amp;serverTimezone=UTC
</property>
<property name="hibernate.connection.username">root</property>
<property name="hibernate.connection.password">YOUR_PASSWORD</property>
```

Replace `YOUR_PASSWORD` with your MySQL root password.

> **Note:** `hbm2ddl.auto=update` will auto-create all tables on first launch.

### 5. Build the Project

**On this machine (run in each new PowerShell terminal):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "C:\tools\maven\bin;$env:JAVA_HOME\bin;$env:PATH"
```

Then build:
```bash
cd d:\jp\TimetableScheduler
mvn clean compile
```

**Or run `setup.ps1` once to automate installation:**
```powershell
cd d:\jp\TimetableScheduler
.\setup.ps1
```

### 5. Run the Application

**Via Maven (recommended):**
```bash
mvn javafx:run
```

**Via IDE (IntelliJ IDEA):**
1. Open the project (File → Open → select `TimetableScheduler/`)
2. Mark `src/main/java` as Sources Root
3. Set `com.timetable.Launcher` as the main class
4. Run directly — the Launcher class bypasses module-path issues

**If you need explicit VM args:**
```
--module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml
```

---

## 🚀 End-to-End Usage Flow

### Admin Portal

1. **Subjects** → Add subjects (name, code, credits)
2. **Teachers** → Add teachers + assign their subjects
3. **Classrooms** → Add rooms with capacities
4. **Working Days** → Add Monday–Friday (or custom)
5. **Time Periods** → Add periods with start/end times
6. **Class Schedule** → Manually create schedule entries
   - Conflict detection auto-runs on Save
   - If teacher or room is double-booked: ⚠ conflict dialog appears
7. **Auto Generate** → Enter assignments as CSV lines:
   ```
   CSE-A,CS301,teacher@college.edu
   CSE-A,CS302,prof@college.edu
   CSE-B,CS301,teacher@college.edu
   ```
   Click **Generate Timetable** — algorithm places all entries conflict-free

### User Portal

1. Enter any filter (Class, Teacher, Subject Code, Day)
2. Click **Search** — results appear in the table
3. Click **Export PDF** → save a printable timetable grid

---

## 🧠 Scheduling Algorithm

The auto-generator uses a **constraint-aware greedy algorithm**:

```
For each (className, subject, teacher) assignment:
  For each WorkingDay:
    For each TimePeriod:
      For each Classroom:
        If teacher NOT already booked at (day, period):
          AND classroom NOT already booked at (day, period):
            → Create ClassSchedule, persist, mark slot as used
            → Break to next assignment
  If no slot found → report failure
```

**Constraints enforced:**
- C1: `teacher_id + working_day_id + time_period_id` must be unique (db + service layer)
- C2: `classroom_id + working_day_id + time_period_id` must be unique (db + service layer)

---

## 📄 PDF Export Format

- **Paper:** A4 Landscape
- **Layout:** One page per class/section
- **Rows:** Time periods (Period 1, Period 2, …)
- **Columns:** Working days (Monday, Tuesday, …)
- **Cell contents:** Subject code / Subject name / Teacher name / Room number

---

## 🔧 Troubleshooting

| Issue | Fix |
|-------|-----|
| `Communications link failure` | MySQL not running or wrong port/credentials in `hibernate.cfg.xml` |
| `Table 'timetable_db.xxx' doesn't exist` | Run `schema.sql` manually or set `hbm2ddl.auto=create` temporarily |
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | Run `mvn clean install` to download MySQL connector |
| JavaFX `module not found` | Use `mvn javafx:run` or set VM args in IDE |
| Conflict dialog appears | Correct behaviour — change teacher, room, day, or period to resolve |
