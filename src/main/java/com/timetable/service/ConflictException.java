package com.timetable.service;

/**
 * Thrown when a schedule entry conflicts with an existing one.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
