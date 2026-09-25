package com.timetable.util;

import com.timetable.model.ClassSchedule;
import com.timetable.model.TimePeriod;
import com.timetable.model.WorkingDay;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PDFExportUtil — generates a printable timetable PDF using Apache PDFBox 3.x.
 *
 * Layout: one page per class/section.
 *         Rows    = Time Periods
 *         Columns = Working Days
 *         Cell content: Subject Code / Subject Name / Teacher / Room
 */
public class PDFExportUtil {

    // ── Colours ────────────────────────────────────────
    private static final Color COLOR_HEADER_BG = new Color(0x16, 0x21, 0x3e);
    private static final Color COLOR_DAY_BG    = new Color(0x23, 0x23, 0x40);
    private static final Color COLOR_PERIOD_BG = new Color(0x1e, 0x1e, 0x36);
    private static final Color COLOR_CELL_BG   = new Color(0xf8, 0xf8, 0xff);
    private static final Color COLOR_CELL_ALT  = new Color(0xed, 0xed, 0xfa);
    private static final Color COLOR_BORDER    = new Color(0x3d, 0x3d, 0x56);
    private static final Color COLOR_TEXT_DARK = new Color(0x1a, 0x1a, 0x2e);
    private static final Color COLOR_ACCENT    = new Color(0x7c, 0x3a, 0xed);
    private static final Color COLOR_MUTED     = new Color(0xaa, 0xaa, 0xcc);
    private static final Color COLOR_LIGHT     = new Color(0x44, 0x44, 0x66);
    private static final Color COLOR_EMPTY     = new Color(0xbb, 0xbb, 0xcc);
    private static final Color COLOR_FOOTER    = new Color(0x88, 0x88, 0x99);

    // ── A4 Landscape Dimensions ────────────────────────
    // A4 portrait: width=595.28pt, height=841.89pt
    // Landscape:   page width = 841.89, page height = 595.28
    private static final float PAGE_W       = PDRectangle.A4.getHeight(); // 841.89 (long side)
    private static final float PAGE_H       = PDRectangle.A4.getWidth();  // 595.28 (short side)
    private static final float MARGIN       = 36f;
    private static final float HEADER_H     = 58f;
    private static final float DAY_HDR_H    = 26f;
    private static final float ROW_H        = 48f;
    private static final float PERIOD_COL_W = 90f;

    // ────────────────────────────────────────────────────────────────────────────
    // Public API
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Exports the given list of ClassSchedule entries to a PDF file at {@code filePath}.
     * One page is generated per distinct class/section name.
     *
     * @param schedules non-null, non-empty list of schedule entries
     * @param filePath  absolute path for the output PDF file
     * @throws IOException if PDF creation or file writing fails
     */
    public static void export(List<ClassSchedule> schedules, String filePath) throws IOException {
        if (schedules == null || schedules.isEmpty()) return;

        // Group by class name (sorted alphabetically)
        Map<String, List<ClassSchedule>> byClass = schedules.stream()
            .collect(Collectors.groupingBy(
                ClassSchedule::getClassName,
                TreeMap::new,
                Collectors.toList()));

        // All distinct days + periods across the whole dataset (sorted)
        List<WorkingDay> days = schedules.stream()
            .map(ClassSchedule::getWorkingDay)
            .distinct()
            .sorted(Comparator.comparing(WorkingDay::getDayName))
            .collect(Collectors.toList());

        List<TimePeriod> periods = schedules.stream()
            .map(ClassSchedule::getTimePeriod)
            .distinct()
            .sorted(Comparator.comparingInt(TimePeriod::getPeriodNumber))
            .collect(Collectors.toList());

        try (PDDocument doc = new PDDocument()) {
            for (Map.Entry<String, List<ClassSchedule>> entry : byClass.entrySet()) {
                renderClassPage(doc, entry.getKey(), entry.getValue(), days, periods);
            }
            doc.save(filePath);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Page Renderer
    // ────────────────────────────────────────────────────────────────────────────

    private static void renderClassPage(
            PDDocument doc,
            String className,
            List<ClassSchedule> schedules,
            List<WorkingDay> days,
            List<TimePeriod> periods) throws IOException {

        // Create fonts inside document scope (required by PDFBox 3.x)
        PDType1Font fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font fontItalic  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        // Landscape page
        PDPage page = new PDPage(new PDRectangle(PAGE_W, PAGE_H));
        doc.addPage(page);

        float tableW   = PAGE_W - 2 * MARGIN;
        float dayColW  = (tableW - PERIOD_COL_W) / Math.max(days.size(), 1);
        float tableTop = PAGE_H - MARGIN - HEADER_H;

        // Pre-build lookup: "dayId_periodId" → schedule entry
        Map<String, ClassSchedule> lookup = new HashMap<>();
        for (ClassSchedule s : schedules) {
            String key = s.getWorkingDay().getId() + "_" + s.getTimePeriod().getId();
            lookup.put(key, s);
        }

        String generated = DateTimeFormatter
            .ofPattern("dd-MM-yyyy HH:mm")
            .format(LocalDateTime.now());

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

            // ── Page Header ──────────────────────────────
            fillRect(cs, MARGIN, tableTop, tableW, HEADER_H, COLOR_HEADER_BG);
            drawText(cs, fontBold,   16, "CLASS TIMETABLE  -  " + className.toUpperCase(),
                     MARGIN + 16, tableTop + HEADER_H - 26, Color.WHITE);
            drawText(cs, fontItalic,  9, "Generated: " + generated,
                     MARGIN + 16, tableTop + HEADER_H - 44, COLOR_MUTED);

            // ── Day Column Headers ───────────────────────
            float hdrTop = tableTop - DAY_HDR_H;

            // "PERIOD / DAY" corner cell
            fillRect(cs, MARGIN, hdrTop, PERIOD_COL_W, DAY_HDR_H, COLOR_PERIOD_BG);
            drawBorder(cs, MARGIN, hdrTop, PERIOD_COL_W, DAY_HDR_H);
            drawCenteredText(cs, fontBold, 8, "PERIOD / DAY",
                             MARGIN, hdrTop, PERIOD_COL_W, DAY_HDR_H, Color.WHITE);

            // One column per working day
            for (int d = 0; d < days.size(); d++) {
                float x = MARGIN + PERIOD_COL_W + d * dayColW;
                fillRect(cs, x, hdrTop, dayColW, DAY_HDR_H, COLOR_DAY_BG);
                drawBorder(cs, x, hdrTop, dayColW, DAY_HDR_H);
                drawCenteredText(cs, fontBold, 10,
                                 days.get(d).getDayName().toUpperCase(),
                                 x, hdrTop, dayColW, DAY_HDR_H, COLOR_ACCENT);
            }

            // ── Schedule Grid ────────────────────────────
            for (int p = 0; p < periods.size(); p++) {
                TimePeriod period = periods.get(p);
                float rowTop = hdrTop - (p + 1) * ROW_H;
                Color rowBg  = (p % 2 == 0) ? COLOR_CELL_BG : COLOR_CELL_ALT;

                // Period label cell (left column)
                fillRect(cs, MARGIN, rowTop, PERIOD_COL_W, ROW_H, COLOR_PERIOD_BG);
                drawBorder(cs, MARGIN, rowTop, PERIOD_COL_W, ROW_H);
                drawText(cs, fontBold,   9, "P" + period.getPeriodNumber(),
                         MARGIN + 6, rowTop + ROW_H - 16, Color.WHITE);
                drawText(cs, fontItalic, 8, period.getStartTime().toString(),
                         MARGIN + 6, rowTop + ROW_H - 28, COLOR_MUTED);
                drawText(cs, fontItalic, 8, period.getEndTime().toString(),
                         MARGIN + 6, rowTop + ROW_H - 39, COLOR_MUTED);

                // One cell per working day
                for (int d = 0; d < days.size(); d++) {
                    float cellX = MARGIN + PERIOD_COL_W + d * dayColW;
                    String key  = days.get(d).getId() + "_" + period.getId();
                    ClassSchedule entry = lookup.get(key);

                    fillRect(cs, cellX, rowTop, dayColW, ROW_H, rowBg);
                    drawBorder(cs, cellX, rowTop, dayColW, ROW_H);

                    if (entry != null) {
                        // Subject code — bold, purple
                        drawText(cs, fontBold, 9,
                                 sanitize(entry.getSubject().getCode()),
                                 cellX + 5, rowTop + ROW_H - 14, COLOR_ACCENT);
                        // Subject name — regular, clipped
                        drawText(cs, fontRegular, 7,
                                 sanitize(truncate(entry.getSubject().getName(), 24)),
                                 cellX + 5, rowTop + ROW_H - 25, COLOR_TEXT_DARK);
                        // Teacher name
                        drawText(cs, fontItalic, 7,
                                 sanitize("T: " + truncate(entry.getTeacher().getName(), 22)),
                                 cellX + 5, rowTop + ROW_H - 36, COLOR_LIGHT);
                        // Room number
                        drawText(cs, fontItalic, 7,
                                 sanitize("R: " + entry.getClassroom().getRoomNumber()),
                                 cellX + 5, rowTop + ROW_H - 46, COLOR_LIGHT);
                    } else {
                        // Empty slot
                        drawCenteredText(cs, fontItalic, 9, "-",
                                         cellX, rowTop, dayColW, ROW_H, COLOR_EMPTY);
                    }
                }
            }

            // ── Footer ──────────────────────────────────
            drawText(cs, fontItalic, 8,
                     "Automatic Class Timetable and Schedule Generator  |  " + className,
                     MARGIN, MARGIN / 2f, COLOR_FOOTER);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Drawing Helpers
    // ────────────────────────────────────────────────────────────────────────────

    /** Fills a rectangle with the given colour (no stroke). */
    private static void fillRect(PDPageContentStream cs,
                                  float x, float y, float w, float h,
                                  Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    /** Draws a hairline border rectangle (no fill). */
    private static void drawBorder(PDPageContentStream cs,
                                    float x, float y, float w, float h) throws IOException {
        cs.setStrokingColor(COLOR_BORDER);
        cs.setLineWidth(0.4f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    /** Draws text at an absolute (x, y) position with the given font, size and colour. */
    private static void drawText(PDPageContentStream cs,
                                  PDType1Font font, float size,
                                  String text, float x, float y,
                                  Color color) throws IOException {
        cs.beginText();
        cs.setNonStrokingColor(color);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
    }

    /**
     * Draws text centred horizontally and vertically within the given cell rectangle.
     */
    private static void drawCenteredText(PDPageContentStream cs,
                                          PDType1Font font, float size,
                                          String text,
                                          float cellX, float cellY,
                                          float cellW, float cellH,
                                          Color color) throws IOException {
        String safe  = sanitize(text);
        float textW  = font.getStringWidth(safe) / 1000f * size;
        float x = cellX + (cellW - textW) / 2f;
        float y = cellY + (cellH - size) / 2f;
        drawText(cs, font, size, safe, x, y, color);
    }

    /**
     * Truncates a string to {@code maxLen} characters, appending "..." if cut.
     * Uses ASCII "..." so it survives the Latin-1 sanitizer.
     */
    private static String truncate(String s, int maxLen) {
        if (s == null || s.length() <= maxLen) return s == null ? "" : s;
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Strips any non-Latin-1 characters (e.g., emoji) that PDFBox Type1 fonts
     * cannot encode — prevents {@link IllegalArgumentException} at render time.
     */
    private static String sanitize(String s) {
        if (s == null) return "";
        // Keep only U+0000–U+00FF (Latin-1 / ISO-8859-1 range)
        return s.replaceAll("[^\u0000-\u00FF]", "");
    }
}
