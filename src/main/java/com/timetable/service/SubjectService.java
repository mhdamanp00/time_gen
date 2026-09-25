package com.timetable.service;

import com.timetable.dao.SubjectDAO;
import com.timetable.model.Subject;

import java.util.List;

public class SubjectService {
    private final SubjectDAO dao = new SubjectDAO();

    public void save(Subject s) {
        validate(s);
        dao.save(s);
    }

    public void update(Subject s) {
        validate(s);
        dao.update(s);
    }

    public void delete(Subject s) { dao.delete(s); }

    public Subject findById(Long id) { return dao.findById(id); }

    public List<Subject> findAll() { return dao.findAll(); }

    private void validate(Subject s) {
        if (s.getName() == null || s.getName().isBlank())
            throw new IllegalArgumentException("Subject name cannot be empty.");
        if (s.getCode() == null || s.getCode().isBlank())
            throw new IllegalArgumentException("Subject code cannot be empty.");
        if (s.getCredits() <= 0)
            throw new IllegalArgumentException("Credits must be a positive number.");
    }
}
