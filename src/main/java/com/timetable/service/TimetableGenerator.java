package com.timetable.service;

import com.timetable.dao.ClassScheduleDAO;
import com.timetable.model.*;

import java.util.*;

/** Builds a timetable proposal in memory. Nothing is persisted until the admin applies it. */
public class TimetableGenerator {

    /** One input row; lessonsPerWeek expands to that many separate weekly lessons. */
    public record Assignment(String className, Subject subject, Teacher teacher, int lessonsPerWeek) {}

    public static class GenerationResult {
        public final List<ClassSchedule> created = new ArrayList<>();
        public final List<String> failures = new ArrayList<>();
    }

    private final ClassScheduleDAO scheduleDAO = new ClassScheduleDAO();

    /** Plans one slot per requested weekly lesson, reserving existing rows when appending. */
    public GenerationResult preview(List<Assignment> assignments, List<Classroom> classrooms,
                                    List<WorkingDay> days, List<TimePeriod> periods,
                                    boolean replaceExisting) {
        GenerationResult result = new GenerationResult();
        List<WorkingDay> orderedDays = new ArrayList<>(days);
        List<TimePeriod> orderedPeriods = new ArrayList<>(periods);
        orderedDays.sort(Comparator.comparing(WorkingDay::getDayName, String.CASE_INSENSITIVE_ORDER));
        orderedPeriods.sort(Comparator.comparingInt(TimePeriod::getPeriodNumber));
        Set<String> teacherSlots = new HashSet<>();
        Set<String> roomSlots = new HashSet<>();
        Set<String> classSlots = new HashSet<>();
        Map<String, Map<Long, Integer>> dailyLoads = new HashMap<>();

        if (!replaceExisting) {
            for (ClassSchedule s : scheduleDAO.findAll()) {
                teacherSlots.add(teacherKey(s.getTeacher(), s.getWorkingDay(), s.getTimePeriod()));
                roomSlots.add(roomKey(s.getClassroom(), s.getWorkingDay(), s.getTimePeriod()));
                classSlots.add(classKey(s.getClassName(), s.getWorkingDay(), s.getTimePeriod()));
                dailyLoads.computeIfAbsent(s.getClassName().toLowerCase(), k -> new HashMap<>())
                        .merge(s.getWorkingDay().getId(), 1, Integer::sum);
            }
        }

        for (Assignment a : assignments) {
            for (int lesson = 1; lesson <= a.lessonsPerWeek(); lesson++) {
                ClassSchedule proposed = findSlot(a, classrooms, orderedDays, orderedPeriods,
                        teacherSlots, roomSlots, classSlots, dailyLoads);
                if (proposed == null) {
                    result.failures.add(a.className() + " — " + a.subject().getCode() +
                            " with " + a.teacher().getName() + " (lesson " + lesson +
                            "/" + a.lessonsPerWeek() + "): no conflict-free slot available.");
                    continue;
                }
                result.created.add(proposed);
                teacherSlots.add(teacherKey(proposed.getTeacher(), proposed.getWorkingDay(), proposed.getTimePeriod()));
                roomSlots.add(roomKey(proposed.getClassroom(), proposed.getWorkingDay(), proposed.getTimePeriod()));
                classSlots.add(classKey(proposed.getClassName(), proposed.getWorkingDay(), proposed.getTimePeriod()));
                dailyLoads.computeIfAbsent(a.className().trim().toLowerCase(), k -> new HashMap<>())
                        .merge(proposed.getWorkingDay().getId(), 1, Integer::sum);
            }
        }
        return result;
    }

    private ClassSchedule findSlot(Assignment a, List<Classroom> rooms, List<WorkingDay> days,
                                   List<TimePeriod> periods, Set<String> teacherSlots,
                                   Set<String> roomSlots, Set<String> classSlots,
                                   Map<String, Map<Long, Integer>> dailyLoads) {
        // Prefer less-loaded days for this class, then walk available periods.
        List<WorkingDay> balancedDays = new ArrayList<>(days);
        Map<Long, Integer> classLoads = dailyLoads.getOrDefault(a.className().trim().toLowerCase(), Map.of());
        balancedDays.sort(Comparator.comparingInt(d -> classLoads.getOrDefault(d.getId(), 0)));

        for (WorkingDay day : balancedDays) {
            for (TimePeriod period : periods) {
                if (teacherSlots.contains(teacherKey(a.teacher(), day, period)) ||
                    classSlots.contains(classKey(a.className(), day, period))) continue;
                for (Classroom room : rooms) {
                    if (roomSlots.contains(roomKey(room, day, period))) continue;
                    ClassSchedule s = new ClassSchedule();
                    s.setClassName(a.className().trim());
                    s.setSubject(a.subject());
                    s.setTeacher(a.teacher());
                    s.setClassroom(room);
                    s.setWorkingDay(day);
                    s.setTimePeriod(period);
                    return s;
                }
            }
        }
        return null;
    }

    public void apply(GenerationResult proposal, boolean replaceExisting) {
        if (proposal.created.isEmpty()) throw new IllegalArgumentException("There are no lessons to apply.");
        scheduleDAO.applyGenerated(proposal.created, replaceExisting);
    }

    private String teacherKey(Teacher t, WorkingDay d, TimePeriod p) { return t.getId() + ":" + d.getId() + ":" + p.getId(); }
    private String roomKey(Classroom c, WorkingDay d, TimePeriod p) { return c.getId() + ":" + d.getId() + ":" + p.getId(); }
    private String classKey(String name, WorkingDay d, TimePeriod p) { return name.trim().toLowerCase() + ":" + d.getId() + ":" + p.getId(); }
}
