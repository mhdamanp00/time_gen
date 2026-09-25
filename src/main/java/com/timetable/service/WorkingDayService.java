package com.timetable.service;

import com.timetable.dao.WorkingDayDAO;
import com.timetable.model.WorkingDay;

import java.util.List;

public class WorkingDayService {
    private final WorkingDayDAO dao = new WorkingDayDAO();

    public void save(WorkingDay d) {
        validate(d);
        dao.save(d);
    }

    public void update(WorkingDay d) {
        validate(d);
        dao.update(d);
    }

    public void delete(WorkingDay d) { dao.delete(d); }

    public WorkingDay findById(Long id) { return dao.findById(id); }

    public List<WorkingDay> findAll() { return dao.findAll(); }

    private void validate(WorkingDay d) {
        if (d.getDayName() == null || d.getDayName().isBlank())
            throw new IllegalArgumentException("Day name cannot be empty.");
    }
}
