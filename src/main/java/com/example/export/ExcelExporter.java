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
import java.util.List;

public final class ExcelExporter {
    private ExcelExporter() {}

    private static final List<String> HEADER = List.of(
            "Дата экспорта",
            "Username",
            "Имя и фамилия",
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

            writeSheet(wb, "Участники", result.participants(), headerStyle);
            writeSheet(wb, "Упоминания", result.mentions(), headerStyle);
            writeSheet(wb, "Каналы", result.channels(), headerStyle);

            try (OutputStream os = Files.newOutputStream(out)) {
                wb.write(os);
            }
        }

        return out;
    }

    private static void writeSheet(Workbook wb, String name, Iterable<UserEntry> rows, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet(name);
        String exportAt = DATE_FMT.format(Instant.now());

        // Row 0: export date info
        Row meta = sheet.createRow(0);
        meta.createCell(0).setCellValue("Дата экспорта");
        meta.createCell(1).setCellValue(exportAt);

        // Row 2: header
        Row header = sheet.createRow(2);
        for (int i = 0; i < HEADER.size(); i++) {
            Cell c = header.createCell(i);
            c.setCellValue(HEADER.get(i));
            c.setCellStyle(headerStyle);
        }

        int r = 3;
        for (UserEntry u : rows) {
            Row row = sheet.createRow(r++);
            // Дата экспорта — повторяем для удобства
            row.createCell(0).setCellValue(exportAt);
            row.createCell(1).setCellValue(u.username() != null ? "@" + u.username() : "");
            row.createCell(2).setCellValue(u.displayName() != null ? u.displayName() : "");
            row.createCell(3).setCellValue("");
            row.createCell(4).setCellValue("");
            row.createCell(5).setCellValue("");
        }

        // autosize columns (умеренно)
        for (int i = 0; i < HEADER.size(); i++) {
            sheet.autoSizeColumn(i);
            int width = Math.min(sheet.getColumnWidth(i), 12000);
            sheet.setColumnWidth(i, width);
        }
        // Freeze pane under the header (row index 2), so headers stay visible while scrolling.
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
