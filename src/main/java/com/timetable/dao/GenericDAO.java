package com.timetable.dao;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

/**
 * Generic DAO providing basic CRUD operations for any entity type.
 * Concrete DAOs extend this class with their entity class.
 *
 * @param <T> The entity type
 */
public class GenericDAO<T> {

    private final Class<T> entityClass;

    public GenericDAO(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected Session openSession() {
        return HibernateUtil.getSessionFactory().openSession();
    }

    // ── CREATE ─────────────────────────────────────────

    public void save(T entity) {
        try (Session session = openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.persist(entity);
                tx.commit();
            } catch (Exception e) {
                rollback(tx, e);
                throw new RuntimeException("Failed to save entity", e);
            }
        }
    }

    // ── UPDATE ─────────────────────────────────────────

    public void update(T entity) {
        try (Session session = openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.merge(entity);
                tx.commit();
            } catch (Exception e) {
                rollback(tx, e);
                throw new RuntimeException("Failed to update entity", e);
            }
        }
    }

    // ── DELETE ─────────────────────────────────────────

    public void delete(T entity) {
        try (Session session = openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                T managed = session.merge(entity);
                session.remove(managed);
                tx.commit();
            } catch (Exception e) {
                rollback(tx, e);
                throw new RuntimeException("Failed to delete entity", e);
            }
        }
    }

    private void rollback(Transaction tx, Exception original) {
        if (tx != null && tx.isActive()) {
            try {
                tx.rollback();
            } catch (Exception rollbackError) {
                original.addSuppressed(rollbackError);
            }
        }
    }

    // ── READ ───────────────────────────────────────────

    public T findById(Long id) {
        try (Session session = openSession()) {
            return session.get(entityClass, id);
        }
    }

    public List<T> findAll() {
        try (Session session = openSession()) {
            return session.createQuery("FROM " + entityClass.getSimpleName(), entityClass)
                          .list();
        }
    }
}
