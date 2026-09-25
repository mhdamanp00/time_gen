package com.timetable.dao;

import com.timetable.model.AdminAccount;
import org.hibernate.Session;

public class AdminAccountDAO {
    public boolean exists() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("SELECT COUNT(a) FROM AdminAccount a", Long.class).uniqueResult() > 0;
        }
    }

    public AdminAccount findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM AdminAccount a WHERE LOWER(a.username) = :username", AdminAccount.class)
                    .setParameter("username", username.trim().toLowerCase())
                    .uniqueResult();
        }
    }

    public void create(AdminAccount account) {
        org.hibernate.Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            if (session.createQuery("SELECT COUNT(a) FROM AdminAccount a", Long.class).uniqueResult() > 0)
                throw new IllegalStateException("An administrator account is already configured.");
            session.persist(account);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null) tx.rollback();
            throw e;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Could not create administrator account.", e);
        }
    }
}
