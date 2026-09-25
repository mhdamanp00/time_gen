package com.timetable.service;

import com.timetable.dao.TeacherDAO;
import com.timetable.model.Teacher;

import java.util.List;

public class TeacherService {
    private final TeacherDAO dao = new TeacherDAO();

    public void save(Teacher t) {
        validate(t);
        dao.save(t);
    }

    public void update(Teacher t) {
        validate(t);
        dao.update(t);
    }

    public void delete(Teacher t) { dao.delete(t); }

    public Teacher findById(Long id) { return dao.findById(id); }

    public List<Teacher> findAll() { return dao.findAll(); }

    private void validate(Teacher t) {
        if (t.getName() == null || t.getName().isBlank())
            throw new IllegalArgumentException("Teacher name cannot be empty.");
        if (t.getEmail() == null || t.getEmail().isBlank())
            throw new IllegalArgumentException("Email cannot be empty.");
        if (!t.getEmail().contains("@"))
            throw new IllegalArgumentException("Email must be a valid address.");
    }
}
