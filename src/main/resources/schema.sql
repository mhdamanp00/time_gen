-- ============================================================
-- Automatic Class Timetable and Schedule Generator
-- MySQL Schema (Fallback Reference)
-- ============================================================
-- Run this script if hbm2ddl.auto doesn't create tables.
-- Usage: mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS timetable_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE timetable_db;

-- One administrator account for protecting the admin screens.
CREATE TABLE IF NOT EXISTS admin_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(60) NOT NULL UNIQUE,
    password_salt VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- Subjects
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS subjects (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    code       VARCHAR(20)  NOT NULL UNIQUE,
    credits    INT          NOT NULL
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- Teachers
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS teachers (
    id    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- Teacher ↔ Subject many-to-many join table
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS teacher_subjects (
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    PRIMARY KEY (teacher_id, subject_id),
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- Classrooms
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS classrooms (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    capacity    INT         NOT NULL
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- Working Days
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS working_days (
    id       BIGINT      AUTO_INCREMENT PRIMARY KEY,
    day_name VARCHAR(20) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- Time Periods
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS time_periods (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    start_time    TIME   NOT NULL,
    end_time      TIME   NOT NULL,
    period_number INT    NOT NULL UNIQUE
) ENGINE=InnoDB;

-- -----------------------------------------------------------
-- Class Schedules (the core timetable entries)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS class_schedules (
    id             BIGINT      AUTO_INCREMENT PRIMARY KEY,
    subject_id     BIGINT      NOT NULL,
    teacher_id     BIGINT      NOT NULL,
    classroom_id   BIGINT      NOT NULL,
    working_day_id BIGINT      NOT NULL,
    time_period_id BIGINT      NOT NULL,
    class_name     VARCHAR(50) NOT NULL,

    FOREIGN KEY (subject_id)     REFERENCES subjects(id),
    FOREIGN KEY (teacher_id)     REFERENCES teachers(id),
    FOREIGN KEY (classroom_id)   REFERENCES classrooms(id),
    FOREIGN KEY (working_day_id) REFERENCES working_days(id),
    FOREIGN KEY (time_period_id) REFERENCES time_periods(id),

    -- Conflict-prevention constraints
    UNIQUE KEY uk_teacher_day_period   (teacher_id, working_day_id, time_period_id),
    UNIQUE KEY uk_classroom_day_period (classroom_id, working_day_id, time_period_id),
    UNIQUE KEY uk_class_day_period (class_name, working_day_id, time_period_id)
) ENGINE=InnoDB;
