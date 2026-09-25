package com.timetable.dao;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.Map;

/**
 * Singleton wrapper around the Hibernate SessionFactory.
 * Reads hibernate.cfg.xml from the classpath on first access.
 */
public class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() { /* utility class */ }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration();
                configuration.configure("hibernate.cfg.xml");
                Map<String, String> environment = System.getenv();
                override(configuration, environment, "TIMETABLE_DB_URL", "hibernate.connection.url");
                override(configuration, environment, "TIMETABLE_DB_USERNAME", "hibernate.connection.username");
                override(configuration, environment, "TIMETABLE_DB_PASSWORD", "hibernate.connection.password");
                sessionFactory = configuration.buildSessionFactory();
            } catch (Exception e) {
                System.err.println("SessionFactory creation failed: " + e.getMessage());
                e.printStackTrace();
                throw new ExceptionInInitializerError(e);
            }
        }
        return sessionFactory;
    }

    private static void override(Configuration configuration, Map<String, String> environment,
                                 String environmentKey, String property) {
        String value = environment.get(environmentKey);
        if (value != null && !value.isBlank()) configuration.setProperty(property, value);
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}
