package com.timetable.service;

import com.timetable.dao.ClassScheduleDAO;
import com.timetable.model.*;

import java.util.List;

/**
 * Service layer for ClassSchedule management.
 * Performs conflict-detection before persisting any schedule entry.
 *
 * Conflict rules:
 *  1. A teacher cannot be booked in two classes at the same Day + Period.
 *  2. A classroom cannot be booked for two classes at the same Day + Period.
 */
public class ScheduleService {

    private final ClassScheduleDAO dao = new ClassScheduleDAO();

    // ── Create / Update ────────────────────────────────

    /**
     * Saves a new schedule entry after conflict checks.
     * @throws ConflictException if a conflict is detected
     * @throws IllegalArgumentException if required fields are missing
     */
    public void save(ClassSchedule schedule) {
        validate(schedule);
        checkConflicts(schedule, null);
        dao.save(schedule);
    }

    /**
     * Updates an existing schedule entry after conflict checks (excluding self).
     * @throws ConflictException if a conflict is detected
     */
    public void update(ClassSchedule schedule) {
        validate(schedule);
        checkConflicts(schedule, schedule.getId());
        dao.update(schedule);
    }

    public void delete(ClassSchedule schedule) {
        dao.delete(schedule);
    }

    public ClassSchedule findById(Long id) {
        return dao.findById(id);
    }

    public List<ClassSchedule> findAll() {
        return dao.findAll();
    }

    // ── Search / Filter ────────────────────────────────

    public List<ClassSchedule> findByClassName(String className) {
        return dao.findByClassName(className);
    }

    public List<ClassSchedule> findByTeacher(Teacher teacher) {
        return dao.findByTeacher(teacher);
    }

    public List<ClassSchedule> findByDay(WorkingDay day) {
        return dao.findByDay(day);
    }

    public List<ClassSchedule> findBySubjectCode(String code) {
        return dao.findBySubjectCode(code);
    }

    // ── Validation ─────────────────────────────────────

    private void validate(ClassSchedule s) {
        if (s.getClassName() == null || s.getClassName().isBlank())
            throw new IllegalArgumentException("Class/Section name cannot be empty.");
        if (s.getSubject() == null)
            throw new IllegalArgumentException("Subject must be selected.");
        if (s.getTeacher() == null)
            throw new IllegalArgumentException("Teacher must be selected.");
        if (s.getClassroom() == null)
            throw new IllegalArgumentException("Classroom must be selected.");
        if (s.getWorkingDay() == null)
            throw new IllegalArgumentException("Working day must be selected.");
        if (s.getTimePeriod() == null)
            throw new IllegalArgumentException("Time period must be selected.");
    }

    // ── Conflict Detection ─────────────────────────────

    /**
     * Checks for teacher and classroom booking conflicts.
     *
     * @param schedule  the schedule being created or updated
     * @param excludeId the ID to exclude when updating (null for new entries)
     * @throws ConflictException if a conflict is found
     */
    private void checkConflicts(ClassSchedule schedule, Long excludeId) {
        WorkingDay day = schedule.getWorkingDay();
        TimePeriod period = schedule.getTimePeriod();

        // Rule 1: Teacher conflict
        if (dao.isTeacherBooked(schedule.getTeacher(), day, period, excludeId)) {
            throw new ConflictException(
                "⚠ TEACHER CONFLICT\n\n" +
                "Teacher \"" + schedule.getTeacher().getName() + "\" is already scheduled on " +
                day.getDayName() + " — " + period + ".\n\n" +
                "Please choose a different day, period, or teacher."
            );
        }

        // Rule 2: Classroom conflict
        if (dao.isClassroomBooked(schedule.getClassroom(), day, period, excludeId)) {
            throw new ConflictException(
                "⚠ CLASSROOM CONFLICT\n\n" +
                "Room \"" + schedule.getClassroom().getRoomNumber() + "\" is already booked on " +
                day.getDayName() + " — " + period + ".\n\n" +
                "Please choose a different day, period, or classroom."
            );
        }
    }
}
