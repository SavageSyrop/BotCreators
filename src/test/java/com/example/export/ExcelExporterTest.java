package com.example.export;

import com.example.core.model.ResultBundle;
import com.example.core.model.UserEntry;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

class ExcelExporterTest {

    @Test
    void createsWorkbookWithThreeSheetsAndHeaders(@TempDir Path temp) throws Exception {
        var participants = new LinkedHashSet<UserEntry>();
        participants.add(new UserEntry("alice", "Alice", "https://t.me/alice"));

        var mentions = new LinkedHashSet<UserEntry>();
        mentions.add(new UserEntry("bob", null, "https://t.me/bob"));

        var channels = new LinkedHashSet<UserEntry>();
        channels.add(new UserEntry("my_channel", null, "https://t.me/my_channel"));

        ResultBundle bundle = new ResultBundle(participants, mentions, channels);

        Path xlsx = ExcelExporter.export(bundle, temp);
        assertTrue(Files.exists(xlsx), "XLSX must be created");

        try (InputStream in = Files.newInputStream(xlsx); XSSFWorkbook wb = new XSSFWorkbook(in)) {
            assertNotNull(wb.getSheet("participants"));
            assertNotNull(wb.getSheet("mentions"));
            assertNotNull(wb.getSheet("channels"));

            var sheet = wb.getSheet("participants");
            // header is on row index 2
            var headerRow = sheet.getRow(2);
            assertNotNull(headerRow);
            assertEquals("Дата экспорта", headerRow.getCell(0).getStringCellValue());
            assertEquals("Username", headerRow.getCell(1).getStringCellValue());
            assertEquals("Имя и фамилия", headerRow.getCell(2).getStringCellValue());
        }
    }
}
