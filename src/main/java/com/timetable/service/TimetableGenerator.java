package com.timetable.service;

import com.timetable.model.*;
import com.timetable.dao.ClassScheduleDAO;

import java.util.*;

/**
 * Constraint-Aware Greedy Scheduling Algorithm
 * ─────────────────────────────────────────────
 * Given a list of (className → subjects) assignments, plus all available
 * teachers, classrooms, days and periods, this generator greedily fills
 * timetable slots while honouring two hard constraints:
 *
 *   C1 – A teacher may not appear in two slots at the same Day+Period.
 *   C2 – A classroom may not appear in two slots at the same Day+Period.
 *
 * Algorithm overview (per class-section):
 *   For each subject needed by the class:
 *     Find a teacher that can teach this subject (has it in their subject list).
 *     For each (day, period) slot in some order:
 *       For each available classroom:
 *         If teacher is free at (day, period) AND classroom is free at (day, period):
 *           → Create and persist the ClassSchedule entry.
 *           → Mark teacher + classroom as used at that slot.
 *           → Move on to next subject.
 *     If no slot found: report failure for this subject.
 *
 * Result: list of successfully created entries + list of unscheduled items.
 */
public class TimetableGenerator {

    /** Simple carrier for one scheduling request. */
    public record Assignment(String className, Subject subject, Teacher teacher) {}

    /** Result of a generation run. */
    public static class GenerationResult {
        public final List<ClassSchedule> created = new ArrayList<>();
        public final List<String> failures = new ArrayList<>();
    }

    private final ClassScheduleDAO scheduleDAO = new ClassScheduleDAO();

    /**
     * Runs the greedy scheduling algorithm.
     *
     * @param assignments  list of (class, subject, teacher) triples to schedule
     * @param classrooms   available classrooms
     * @param days         available working days
     * @param periods      available time periods
     * @param clearFirst   if true, deletes all existing schedules before running
     * @return             GenerationResult with created entries and failure messages
     */
    public GenerationResult generate(
            List<Assignment> assignments,
            List<Classroom> classrooms,
            List<WorkingDay> days,
            List<TimePeriod> periods,
            boolean clearFirst) {

        if (clearFirst) {
            scheduleDAO.deleteAll();
        }

        GenerationResult result = new GenerationResult();

        // Track booked slots in memory for fast O(1) conflict checks
        // Key: "teacherId_dayId_periodId" or "classroomId_dayId_periodId"
        Set<String> bookedTeacherSlots    = new HashSet<>();
        Set<String> bookedClassroomSlots  = new HashSet<>();

        // Pre-populate with any existing schedules (when clearFirst == false)
        if (!clearFirst) {
            for (ClassSchedule cs : scheduleDAO.findAll()) {
                bookedTeacherSlots.add(teacherKey(cs.getTeacher(), cs.getWorkingDay(), cs.getTimePeriod()));
                bookedClassroomSlots.add(classroomKey(cs.getClassroom(), cs.getWorkingDay(), cs.getTimePeriod()));
            }
        }

        for (Assignment assignment : assignments) {
            boolean scheduled = false;

            outer:
            for (WorkingDay day : days) {
                for (TimePeriod period : periods) {
                    // Check teacher availability
                    String tKey = teacherKey(assignment.teacher(), day, period);
                    if (bookedTeacherSlots.contains(tKey)) continue;

                    // Try each classroom
                    for (Classroom classroom : classrooms) {
                        String cKey = classroomKey(classroom, day, period);
                        if (bookedClassroomSlots.contains(cKey)) continue;

                        // Slot is free — create the schedule entry
                        ClassSchedule cs = new ClassSchedule();
                        cs.setClassName(assignment.className());
                        cs.setSubject(assignment.subject());
                        cs.setTeacher(assignment.teacher());
                        cs.setClassroom(classroom);
                        cs.setWorkingDay(day);
                        cs.setTimePeriod(period);

                        scheduleDAO.save(cs);
                        result.created.add(cs);

                        // Mark slot as taken
                        bookedTeacherSlots.add(tKey);
                        bookedClassroomSlots.add(cKey);

                        scheduled = true;
                        break outer;
                    }
                }
            }

            if (!scheduled) {
                result.failures.add(
                    "Could not schedule: [" + assignment.className() + "] " +
                    assignment.subject().getCode() + " with " + assignment.teacher().getName() +
                    " — no free slot found."
                );
            }
        }

        return result;
    }

    // ── Key helpers ────────────────────────────────────

    private String teacherKey(Teacher t, WorkingDay d, TimePeriod p) {
        return "T" + t.getId() + "_D" + d.getId() + "_P" + p.getId();
    }

    private String classroomKey(Classroom c, WorkingDay d, TimePeriod p) {
        return "C" + c.getId() + "_D" + d.getId() + "_P" + p.getId();
    }
}
