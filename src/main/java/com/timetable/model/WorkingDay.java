package com.timetable.model;

import jakarta.persistence.*;

/**
 * Represents a working day of the week (e.g., Monday, Tuesday …).
 */
@Entity
@Table(name = "working_days")
public class WorkingDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_name", nullable = false, unique = true, length = 20)
    private String dayName;

    public WorkingDay() {}

    public WorkingDay(String dayName) {
        this.dayName = dayName;
    }

    // ── Getters & Setters ──────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    // ── equals / hashCode ──────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkingDay that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /** Used by ComboBox display. */
    @Override
    public String toString() {
        return dayName;
    }
}
