package com.timetable.model;

import jakarta.persistence.*;

/**
 * Core timetable entry: binds a Subject + Teacher + Classroom + Day + Period
 * to a specific class/section (e.g., "CSE-A").
 */
@Entity
@Table(name = "class_schedules",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_teacher_day_period",
                          columnNames = {"teacher_id", "working_day_id", "time_period_id"}),
        @UniqueConstraint(name = "uk_classroom_day_period",
                          columnNames = {"classroom_id", "working_day_id", "time_period_id"})
    }
)
public class ClassSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "working_day_id", nullable = false)
    private WorkingDay workingDay;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "time_period_id", nullable = false)
    private TimePeriod timePeriod;

    @Column(name = "class_name", nullable = false, length = 50)
    private String className;

    public ClassSchedule() {}

    // ── Getters & Setters ──────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    public Classroom getClassroom() { return classroom; }
    public void setClassroom(Classroom classroom) { this.classroom = classroom; }

    public WorkingDay getWorkingDay() { return workingDay; }
    public void setWorkingDay(WorkingDay workingDay) { this.workingDay = workingDay; }

    public TimePeriod getTimePeriod() { return timePeriod; }
    public void setTimePeriod(TimePeriod timePeriod) { this.timePeriod = timePeriod; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    // ── Display helpers (for TableView PropertyValueFactory) ──

    public String getSubjectName() { return subject != null ? subject.toString() : ""; }
    public String getTeacherName() { return teacher != null ? teacher.getName() : ""; }
    public String getRoomNumber() { return classroom != null ? classroom.getRoomNumber() : ""; }
    public String getDayName() { return workingDay != null ? workingDay.getDayName() : ""; }
    public String getPeriodInfo() { return timePeriod != null ? timePeriod.toString() : ""; }

    // ── equals / hashCode ──────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassSchedule that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return className + ": " + subject + " | " + teacher + " | " + classroom;
    }
}
