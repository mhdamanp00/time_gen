# Class Timetable Scheduler

A desktop application for building, managing, and exporting class timetables. It includes an admin workspace for setup and schedule management, conflict checks for teachers and rooms, automatic timetable generation, and PDF export.

## At a glance

- **Language:** Java 17
- **Interface:** JavaFX 21
- **Database:** MySQL 8
- **Persistence:** Hibernate 6
- **Build tool:** Maven 3.8+

## How the project is organized

```text
pom.xml                         Maven build and dependencies
setup.ps1                       Optional Windows Java/Maven setup helper
src/main/java/com/timetable/    Application, interface, and scheduling logic
src/main/resources/fxml/         JavaFX screens
src/main/resources/css/          Application styles
src/main/resources/hibernate.cfg.xml  Database connection settings
src/main/resources/schema.sql    Optional database schema script
```

The repository root is the folder containing `pom.xml`. The name of that folder does not matter. After cloning, run commands from that folder; you do not need to create another `TimetableScheduler` directory inside it.

## Requirements

Install the following before running the application:

1. **Java JDK 17** (not just a JRE). Check with `java -version`.
2. **Apache Maven 3.8 or newer.** Check with `mvn -version`.
3. **MySQL Server 8** running on your computer or a reachable server. Check that you know its host, port, username, and password.

JavaFX and the application libraries are downloaded by Maven during the first build.

## Setup and run

### 1. Get the project

Clone the GitHub repository, then open a terminal in the cloned folder—the folder where `pom.xml` is located.

```bash
git clone <repository-url>
cd <cloned-repository-folder>
```

If you downloaded a ZIP instead, extract it and open a terminal in the extracted project root.

### 2. Create a MySQL database and user

In MySQL, create a database. You can use the MySQL command line:

```sql
CREATE DATABASE timetable_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

For better security, use a dedicated MySQL user for this application instead of your MySQL root account. For example, replace the example password with a strong password you choose:

```sql
CREATE USER 'timetable_app'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
GRANT ALL PRIVILEGES ON timetable_db.* TO 'timetable_app'@'localhost';
```

The app is configured with `hibernate.hbm2ddl.auto=update`, so Hibernate creates or updates the tables when the app starts. `src/main/resources/schema.sql` is also provided as a reference/manual setup option; it is not normally necessary to run it first.

### 3. Configure the database connection

Set database settings in your terminal so credentials stay out of source control. On Windows PowerShell:

```powershell
$env:TIMETABLE_DB_URL = "jdbc:mysql://localhost:3306/timetable_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:TIMETABLE_DB_USERNAME = "timetable_app"
$env:TIMETABLE_DB_PASSWORD = "your-local-database-password"
```

On macOS/Linux, use `export TIMETABLE_DB_URL=...`, `export TIMETABLE_DB_USERNAME=...`, and `export TIMETABLE_DB_PASSWORD=...`. The checked-in XML contains only a placeholder. **Do not commit real database credentials to GitHub.** The previous configuration contained a database password; if that value was pushed to a public repository, change it in MySQL.

### 4. Build the project

From the project root, run:

```bash
mvn clean package
```

The first build may take a few minutes while Maven downloads dependencies. A successful build ends with `BUILD SUCCESS`.

### 5. Start the application

From the same project root, run:

```bash
mvn javafx:run
```

The application connects to MySQL at startup and opens the **Find a timetable** screen. Use **Admin login** to enter the protected management area.

### First-run administrator setup

The first time **Admin login** is selected, create an administrator username and password. Choose a password with at least eight characters and keep it somewhere safe. The password is stored as a salted PBKDF2 hash; it is not stored as readable text. The account persists in the `timetable_db` database. Later launches show the login form, not setup. Use **Sign out** in the top bar to end the admin session and return to the user timetable screen.

If you forget the only admin password, stop the app and remove the administrator account row from MySQL:

```sql
DELETE FROM timetable_db.admin_accounts;
```

The next admin sign-in opens first-run setup again. This resets access only; it does not delete teachers, rooms, or schedules.

### Let users on other computers view the timetable

The admin computer can serve the timetable through a read-only Java API. Keep its JavaFX application running while users connect. Set the database variables above, then start the admin application in server mode:

```powershell
$env:TIMETABLE_API_SERVER = "true"
$env:TIMETABLE_API_PORT = "8080"
mvn javafx:run
```

Find the admin computer's private network address (for example, with `ipconfig`). On each user computer, install Java 17 and Maven, clone the project, then set the API address and launch the JavaFX app:

```powershell
$env:TIMETABLE_API_URL = "http://192.168.1.20:8080"
mvn javafx:run
```

Replace the example IP with the admin computer's address. User installations show the read-only timetable portal and PDF download; they do not connect to MySQL and do not show admin tools. The admin computer must remain on and the app running. Allow TCP port 8080 only on the trusted private network. This API exposes timetable information without login and is intended for a trusted school LAN; it is read-only and does not expose MySQL credentials. Do not expose it directly to the public internet.

## Using the application

### Set up timetable data (Admin)

Before generating a timetable, add the required reference data in the admin screens:

1. **Subjects:** add each subject with its name, code, and credits.
2. **Teachers:** add teachers and associate the subjects they can teach.
3. **Classrooms:** add rooms and their capacities.
4. **Working Days:** add the days on which classes can be scheduled.
5. **Time Periods:** define each period's start and end time.
6. **Class Schedule:** add schedule entries manually, or use **Auto Generate**.

When entering schedules manually, the app checks that the teacher, classroom, and class/section are each free at the selected day and period. Resolve a conflict by changing the teacher, room, day, or period.

### Generate a timetable

Open **Auto Generate** and enter one assignment per line in this format:

```text
class-name,subject-code,teacher-email[,lessons-per-week]
```

For example (the fourth column is optional; if omitted, the subject is scheduled once per week):

```text
CSE-A,CS301,teacher@college.edu,3
CSE-A,CS302,prof@college.edu,2
CSE-B,CS301,teacher@college.edu,3
```

Use subject codes and teacher email addresses that match records already added in the admin screens, and make sure each teacher is assigned to the selected subject. Choose **Generate Timetable** to preview available day, period, and room assignments. Review the proposal and any lessons it could not place, then choose **Apply Proposal** to save it. Existing schedules are preserved by default; select **Replace existing timetable when applying** only when you want to replace them. Replacing is performed as one database transaction, so a failed save preserves the current timetable.

### Search and export

The app opens on **Find a timetable**. Filter by class, teacher, subject code, or day; filters can be combined. Select **Search timetables**, then **Download PDF** to save the matching entries.

## Troubleshooting

| Problem | What to check |
|---|---|
| `mvn` or `java` is not recognized | Install Java JDK 17 and Maven, then open a new terminal. Confirm with `java -version` and `mvn -version`. |
| Maven cannot resolve dependencies | Check your internet connection and rerun `mvn clean package`. |
| `Communications link failure` | Make sure MySQL is running and the host/port in `hibernate.cfg.xml` are correct. |
| Access denied for MySQL user | Check the configured username/password and confirm the user has privileges on `timetable_db`. |
| Unknown database `timetable_db` | Create the database using the SQL above. |
| JavaFX modules or window launch errors | Run from the project root with `mvn javafx:run`; JavaFX dependencies are configured in Maven. |
| Timetable assignment cannot be placed | Confirm the subject, teacher, days, periods, and rooms exist, and that enough unoccupied slots are available. |

## Development

Useful Maven commands (run from the project root):

```bash
mvn clean compile   # compile the application
mvn javafx:run      # launch the application
mvn clean package   # compile and package the Maven application artifact
```

On Windows, `setup.ps1` can help install Java and Maven when `winget` is available. It is optional; you can install the prerequisites yourself and follow the steps above. The script may require administrator approval to install software.
