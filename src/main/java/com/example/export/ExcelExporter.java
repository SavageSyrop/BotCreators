package com.example.export;

import com.example.core.model.ResultBundle;
import com.example.core.model.UserEntry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

/**
 * Экспорт результата в Excel.
 *
 * Требование ТЗ: разные вкладки содержат разные наборы полей:
 * - Участники: без поля Username (в JSON-экспорте Telegram Desktop username автора обычно недоступен)
 * - Упоминания: без поля "Имя и фамилия"
 * - Каналы: без поля "Имя и фамилия"
 */
public final class ExcelExporter {
    private ExcelExporter() {}

    private static final List<String> HEADER_PARTICIPANTS = List.of(
            "Дата экспорта",
            "Имя и фамилия",
            "Описание (Bio)",
            "Дата регистрации",
            "Наличие канала"
    );

    private static final List<String> HEADER_MENTIONS = List.of(
            "Дата экспорта",
            "Username",
            "Описание (Bio)",
            "Дата регистрации",
            "Наличие канала"
    );

    private static final List<String> HEADER_CHANNELS = List.of(
            "Дата экспорта",
            "Username",
            "Описание (Bio)",
            "Дата регистрации",
            "Наличие канала"
    );

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    public static Path export(ResultBundle result, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Path out = outputDir.resolve("participants.xlsx");

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(wb);

            writeParticipantsSheet(wb, result.participants(), headerStyle);
            writeMentionsSheet(wb, result.mentions(), headerStyle);
            writeChannelsSheet(wb, result.channels(), headerStyle);

            try (OutputStream os = Files.newOutputStream(out)) {
                wb.write(os);
            }
        }

        return out;
    }

    private static void writeParticipantsSheet(Workbook wb, Collection<UserEntry> rows, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("Участники");
        String exportAt = DATE_FMT.format(Instant.now());

        writeMeta(sheet, exportAt);
        writeHeader(sheet, HEADER_PARTICIPANTS, headerStyle);

        int r = 3;
        for (UserEntry u : rows) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(exportAt);
            row.createCell(1).setCellValue(u.displayName() != null ? u.displayName() : "");
            row.createCell(2).setCellValue("");
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue("");
        }

        autosizeAndFreeze(sheet, HEADER_PARTICIPANTS.size());
    }

    private static void writeMentionsSheet(Workbook wb, Collection<UserEntry> rows, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("Упоминания");
        String exportAt = DATE_FMT.format(Instant.now());

        writeMeta(sheet, exportAt);
        writeHeader(sheet, HEADER_MENTIONS, headerStyle);

        int r = 3;
        for (UserEntry u : rows) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(exportAt);

            // Для mentions ожидаем username; если вдруг null — оставим пусто
            String username = u.username();
            row.createCell(1).setCellValue(username != null ? "@" + username : "");

            row.createCell(2).setCellValue("");
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue("");
        }

        autosizeAndFreeze(sheet, HEADER_MENTIONS.size());
    }

    private static void writeChannelsSheet(Workbook wb, Collection<UserEntry> rows, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("Каналы");
        String exportAt = DATE_FMT.format(Instant.now());

        writeMeta(sheet, exportAt);
        writeHeader(sheet, HEADER_CHANNELS, headerStyle);

        int r = 3;
        for (UserEntry u : rows) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(exportAt);

            // Для channels иногда есть только ссылка (t.me/...), поэтому используем username, а если его нет — link
            String value = "";
            if (u.username() != null && !u.username().isBlank()) {
                value = "@" + u.username();
            } else if (u.link() != null) {
                value = u.link();
            }
            row.createCell(1).setCellValue(value);

            row.createCell(2).setCellValue("");
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue("");
        }

        autosizeAndFreeze(sheet, HEADER_CHANNELS.size());
    }

    private static void writeMeta(Sheet sheet, String exportAt) {
        Row meta = sheet.createRow(0);
        meta.createCell(0).setCellValue("Дата экспорта");
        meta.createCell(1).setCellValue(exportAt);
    }

    private static void writeHeader(Sheet sheet, List<String> header, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(2);
        for (int i = 0; i < header.size(); i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(header.get(i));
            c.setCellStyle(headerStyle);
        }
    }

    private static void autosizeAndFreeze(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            int width = Math.min(sheet.getColumnWidth(i), 12000);
            sheet.setColumnWidth(i, width);
        }
        sheet.createFreezePane(0, 3);
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);

        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
