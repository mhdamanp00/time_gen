package com.timetable.service;

import com.timetable.dao.TimePeriodDAO;
import com.timetable.model.TimePeriod;

import java.util.List;

public class TimePeriodService {
    private final TimePeriodDAO dao = new TimePeriodDAO();

    public void save(TimePeriod p) {
        validate(p);
        ensurePeriodNumberAvailable(p);
        dao.save(p);
    }

    public void update(TimePeriod p) {
        validate(p);
        ensurePeriodNumberAvailable(p);
        dao.update(p);
    }

    public void delete(TimePeriod p) { dao.delete(p); }

    public TimePeriod findById(Long id) { return dao.findById(id); }

    public List<TimePeriod> findAll() { return dao.findAll(); }

    private void validate(TimePeriod p) {
        if (p.getStartTime() == null)
            throw new IllegalArgumentException("Start time cannot be empty.");
        if (p.getEndTime() == null)
            throw new IllegalArgumentException("End time cannot be empty.");
        if (!p.getEndTime().isAfter(p.getStartTime()))
            throw new IllegalArgumentException("End time must be after start time.");
        if (p.getPeriodNumber() <= 0)
            throw new IllegalArgumentException("Period number must be a positive integer.");
    }

    private void ensurePeriodNumberAvailable(TimePeriod period) {
        boolean alreadyUsed = dao.findAll().stream()
                .anyMatch(existing -> existing.getPeriodNumber() == period.getPeriodNumber()
                        && !existing.getId().equals(period.getId()));
        if (alreadyUsed) {
            throw new IllegalArgumentException("Period number " + period.getPeriodNumber()
                    + " already exists. Choose another number or select that row to update it.");
        }
    }
}
