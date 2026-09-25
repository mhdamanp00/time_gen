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
        Transaction tx = null;
        try (Session session = openSession()) {
            tx = session.beginTransaction();
            session.persist(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to save entity", e);
        }
    }

    // ── UPDATE ─────────────────────────────────────────

    public void update(T entity) {
        Transaction tx = null;
        try (Session session = openSession()) {
            tx = session.beginTransaction();
            session.merge(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to update entity", e);
        }
    }

    // ── DELETE ─────────────────────────────────────────

    public void delete(T entity) {
        Transaction tx = null;
        try (Session session = openSession()) {
            tx = session.beginTransaction();
            T managed = session.merge(entity);
            session.remove(managed);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Failed to delete entity", e);
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
