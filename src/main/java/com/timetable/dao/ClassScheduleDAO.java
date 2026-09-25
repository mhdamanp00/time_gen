package com.timetable.dao;

import com.timetable.model.ClassSchedule;
import com.timetable.model.Classroom;
import com.timetable.model.Teacher;
import com.timetable.model.WorkingDay;
import com.timetable.model.TimePeriod;
import org.hibernate.Session;

import java.util.List;

/**
 * DAO for ClassSchedule with conflict-detection and filter queries.
 */
public class ClassScheduleDAO extends GenericDAO<ClassSchedule> {

    public ClassScheduleDAO() {
        super(ClassSchedule.class);
    }

    /**
     * Checks if the given teacher is already booked at the given day+period.
     * Optionally excludes an existing schedule ID (used during updates).
     */
    public boolean isTeacherBooked(Teacher teacher, WorkingDay day, TimePeriod period, Long excludeId) {
        try (Session session = openSession()) {
            String hql = "SELECT COUNT(s) FROM ClassSchedule s " +
                         "WHERE s.teacher = :teacher AND s.workingDay = :day AND s.timePeriod = :period" +
                         (excludeId != null ? " AND s.id != :excludeId" : "");
            var q = session.createQuery(hql, Long.class)
                    .setParameter("teacher", teacher)
                    .setParameter("day", day)
                    .setParameter("period", period);
            if (excludeId != null) q.setParameter("excludeId", excludeId);
            return q.uniqueResult() > 0;
        }
    }

    /**
     * Checks if the given classroom is already booked at the given day+period.
     * Optionally excludes an existing schedule ID (used during updates).
     */
    public boolean isClassroomBooked(Classroom classroom, WorkingDay day, TimePeriod period, Long excludeId) {
        try (Session session = openSession()) {
            String hql = "SELECT COUNT(s) FROM ClassSchedule s " +
                         "WHERE s.classroom = :classroom AND s.workingDay = :day AND s.timePeriod = :period" +
                         (excludeId != null ? " AND s.id != :excludeId" : "");
            var q = session.createQuery(hql, Long.class)
                    .setParameter("classroom", classroom)
                    .setParameter("day", day)
                    .setParameter("period", period);
            if (excludeId != null) q.setParameter("excludeId", excludeId);
            return q.uniqueResult() > 0;
        }
    }

    /** Returns all schedules for a given class/section name (case-insensitive). */
    public List<ClassSchedule> findByClassName(String className) {
        try (Session session = openSession()) {
            return session.createQuery(
                    "FROM ClassSchedule s WHERE LOWER(s.className) LIKE :cn ORDER BY s.workingDay.dayName, s.timePeriod.periodNumber",
                    ClassSchedule.class)
                    .setParameter("cn", "%" + className.toLowerCase() + "%")
                    .list();
        }
    }

    /** Returns all schedules for a given teacher. */
    public List<ClassSchedule> findByTeacher(Teacher teacher) {
        try (Session session = openSession()) {
            return session.createQuery(
                    "FROM ClassSchedule s WHERE s.teacher = :teacher ORDER BY s.workingDay.dayName, s.timePeriod.periodNumber",
                    ClassSchedule.class)
                    .setParameter("teacher", teacher)
                    .list();
        }
    }

    /** Returns all schedules for a given working day. */
    public List<ClassSchedule> findByDay(WorkingDay day) {
        try (Session session = openSession()) {
            return session.createQuery(
                    "FROM ClassSchedule s WHERE s.workingDay = :day ORDER BY s.timePeriod.periodNumber, s.className",
                    ClassSchedule.class)
                    .setParameter("day", day)
                    .list();
        }
    }

    /** Returns all schedules for a given subject code (partial match). */
    public List<ClassSchedule> findBySubjectCode(String code) {
        try (Session session = openSession()) {
            return session.createQuery(
                    "FROM ClassSchedule s WHERE LOWER(s.subject.code) LIKE :code ORDER BY s.workingDay.dayName, s.timePeriod.periodNumber",
                    ClassSchedule.class)
                    .setParameter("code", "%" + code.toLowerCase() + "%")
                    .list();
        }
    }

    /** Deletes all schedule entries (used before auto-regeneration). */
    public void deleteAll() {
        org.hibernate.Transaction tx = null;
        try (Session session = openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery("DELETE FROM ClassSchedule").executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to clear schedules", e);
        }
    }
}
