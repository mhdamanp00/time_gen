package com.timetable.service;

import com.timetable.dao.ClassroomDAO;
import com.timetable.model.Classroom;

import java.util.List;

public class ClassroomService {
    private final ClassroomDAO dao = new ClassroomDAO();

    public void save(Classroom c) {
        validate(c);
        dao.save(c);
    }

    public void update(Classroom c) {
        validate(c);
        dao.update(c);
    }

    public void delete(Classroom c) { dao.delete(c); }

    public Classroom findById(Long id) { return dao.findById(id); }

    public List<Classroom> findAll() { return dao.findAll(); }

    private void validate(Classroom c) {
        if (c.getRoomNumber() == null || c.getRoomNumber().isBlank())
            throw new IllegalArgumentException("Room number cannot be empty.");
        if (c.getCapacity() <= 0)
            throw new IllegalArgumentException("Capacity must be a positive number.");
    }
}
