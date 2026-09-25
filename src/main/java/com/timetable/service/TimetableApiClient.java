package com.timetable.service;

import com.timetable.model.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

/** Client for the timetable's read-only Java API. */
public final class TimetableApiClient {
    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public TimetableApiClient(String baseUrl) {
        this.baseUrl = baseUrl.trim().replaceAll("/+$", "");
    }

    public static Optional<TimetableApiClient> fromEnvironment() {
        String url = System.getenv("TIMETABLE_API_URL");
        return url == null || url.isBlank() ? Optional.empty() : Optional.of(new TimetableApiClient(url));
    }

    public List<ClassSchedule> findSchedules(String className, String teacher, String subject, String day) throws Exception {
        URI uri = endpoint("/api/v1/schedules", className, teacher, subject, day);
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) throw new IllegalStateException(response.body());
        List<ClassSchedule> rows = new ArrayList<>();
        for (String line : response.body().split("\\R")) {
            if (!line.isBlank()) rows.add(decode(line));
        }
        return rows;
    }

    public byte[] downloadPdf(String className, String teacher, String subject, String day) throws Exception {
        URI uri = endpoint("/api/v1/schedules.pdf", className, teacher, subject, day);
        HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new IllegalStateException(new String(response.body(), StandardCharsets.UTF_8));
        return response.body();
    }

    private URI endpoint(String path, String className, String teacher, String subject, String day) {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("class", className); filters.put("teacher", teacher);
        filters.put("subject", subject); filters.put("day", day);
        String query = filters.entrySet().stream().filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        return URI.create(baseUrl + path + (query.isEmpty() ? "" : "?" + query));
    }

    private static ClassSchedule decode(String row) {
        String[] values = row.split("\\t", -1);
        if (values.length != 16) throw new IllegalArgumentException("The timetable server returned an invalid row.");
        String[] f = Arrays.stream(values).map(value -> new String(DECODER.decode(value), StandardCharsets.UTF_8)).toArray(String[]::new);
        Subject subject = new Subject(f[3], f[2], integer(f[4])); subject.setId(longValue(f[1]));
        Teacher teacher = new Teacher(f[6], ""); teacher.setId(longValue(f[5]));
        Classroom room = new Classroom(f[8], integer(f[9])); room.setId(longValue(f[7]));
        WorkingDay workingDay = new WorkingDay(f[11]); workingDay.setId(longValue(f[10]));
        TimePeriod period = new TimePeriod(LocalTime.parse(f[14]), LocalTime.parse(f[15]), integer(f[13]));
        period.setId(longValue(f[12]));
        ClassSchedule schedule = new ClassSchedule();
        schedule.setClassName(f[0]); schedule.setSubject(subject); schedule.setTeacher(teacher);
        schedule.setClassroom(room); schedule.setWorkingDay(workingDay); schedule.setTimePeriod(period);
        return schedule;
    }

    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static Long longValue(String value) { return value.isBlank() ? null : Long.parseLong(value); }
    private static int integer(String value) { return Integer.parseInt(value); }
}
