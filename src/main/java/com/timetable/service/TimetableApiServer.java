package com.timetable.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.timetable.model.ClassSchedule;
import com.timetable.util.PDFExportUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/** Optional read-only API for remote JavaFX user installations; never exposes MySQL credentials. */
public final class TimetableApiServer implements AutoCloseable {
    private final HttpServer server;

    public TimetableApiServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/health", exchange -> respond(exchange, 200, "text/plain; charset=utf-8", "ok"));
        server.createContext("/api/v1/schedules", this::handleSchedules);
        server.setExecutor(Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "timetable-api");
            thread.setDaemon(true);
            return thread;
        }));
    }

    public void start() { server.start(); }

    private void handleSchedules(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "text/plain; charset=utf-8", "GET only");
            return;
        }
        try {
            Map<String, String> filters = query(exchange.getRequestURI().getRawQuery());
            List<ClassSchedule> rows = new ScheduleService().findAll().stream()
                    .filter(s -> contains(s.getClassName(), filters.get("class")))
                    .filter(s -> contains(s.getTeacher().getName(), filters.get("teacher")))
                    .filter(s -> contains(s.getSubject().getCode(), filters.get("subject")))
                    .filter(s -> contains(s.getWorkingDay().getDayName(), filters.get("day")))
                    .sorted(Comparator.comparing((ClassSchedule s) -> s.getWorkingDay().getDayName())
                            .thenComparing(s -> s.getTimePeriod().getPeriodNumber())
                            .thenComparing(ClassSchedule::getClassName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            if (exchange.getRequestURI().getPath().endsWith(".pdf")) {
                if (rows.isEmpty()) {
                    respond(exchange, 404, "text/plain; charset=utf-8", "No matching timetable entries.");
                    return;
                }
                byte[] pdf = PDFExportUtil.exportToBytes(rows);
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"timetable.pdf\"");
                respond(exchange, 200, "application/pdf", pdf);
            } else {
                StringBuilder payload = new StringBuilder();
                for (ClassSchedule s : rows) {
                    append(payload, s.getClassName(), id(s.getSubject().getId()), s.getSubject().getCode(),
                            s.getSubject().getName(), Integer.toString(s.getSubject().getCredits()),
                            id(s.getTeacher().getId()), s.getTeacher().getName(),
                            id(s.getClassroom().getId()), s.getClassroom().getRoomNumber(),
                            Integer.toString(s.getClassroom().getCapacity()),
                            id(s.getWorkingDay().getId()), s.getWorkingDay().getDayName(),
                            id(s.getTimePeriod().getId()), Integer.toString(s.getTimePeriod().getPeriodNumber()),
                            s.getTimePeriod().getStartTime().toString(), s.getTimePeriod().getEndTime().toString());
                }
                respond(exchange, 200, "text/plain; charset=utf-8", payload.toString());
            }
        } catch (Exception e) {
            System.err.println("Timetable API request failed: " + e.getMessage());
            respond(exchange, 500, "text/plain; charset=utf-8", "Timetable service is temporarily unavailable.");
        }
    }

    private static Map<String, String> query(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        return java.util.Arrays.stream(raw.split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(
                        pair -> decode(pair[0]), pair -> pair.length > 1 ? decode(pair[1]) : "",
                        (first, ignored) -> first));
    }

    private static String decode(String value) { return URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private static boolean contains(String value, String part) {
        return part == null || part.isBlank() || value.toLowerCase().contains(part.toLowerCase());
    }
    private static String id(Long value) { return value == null ? "" : value.toString(); }
    private static void append(StringBuilder output, String... fields) {
        Base64.Encoder encoder = Base64.getEncoder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) output.append('\t');
            output.append(encoder.encodeToString(fields[i].getBytes(StandardCharsets.UTF_8)));
        }
        output.append('\n');
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        respond(exchange, status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) { output.write(body); }
    }

    @Override public void close() { server.stop(1); }
}
