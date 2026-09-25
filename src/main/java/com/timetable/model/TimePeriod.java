package com.timetable.model;

import jakarta.persistence.*;
import java.time.LocalTime;

/**
 * Represents a time slot within a working day (e.g., Period 1: 09:00 – 09:50).
 */
@Entity
@Table(name = "time_periods")
public class TimePeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "period_number", nullable = false, unique = true)
    private int periodNumber;

    public TimePeriod() {}

    public TimePeriod(LocalTime startTime, LocalTime endTime, int periodNumber) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.periodNumber = periodNumber;
    }

    // ── Getters & Setters ──────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getPeriodNumber() { return periodNumber; }
    public void setPeriodNumber(int periodNumber) { this.periodNumber = periodNumber; }

    // ── Derived display helpers ────────────────────────

    public String getTimeRange() {
        return startTime + " – " + endTime;
    }

    // ── equals / hashCode ──────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimePeriod that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /** Used by ComboBox display. */
    @Override
    public String toString() {
        return "Period " + periodNumber + " (" + startTime + "–" + endTime + ")";
    }
}
