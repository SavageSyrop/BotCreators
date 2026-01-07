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
            var participantsSheet = wb.getSheet("Участники");
            var mentionsSheet = wb.getSheet("Упоминания");
            var channelsSheet = wb.getSheet("Каналы");

            assertNotNull(participantsSheet);
            assertNotNull(mentionsSheet);
            assertNotNull(channelsSheet);

            // header is on row index 2
            var participantsHeader = participantsSheet.getRow(2);
            assertNotNull(participantsHeader);
            assertEquals("Дата экспорта", participantsHeader.getCell(0).getStringCellValue());
            assertEquals("Имя и фамилия", participantsHeader.getCell(1).getStringCellValue());
            assertEquals("Описание (Bio)", participantsHeader.getCell(2).getStringCellValue());
            assertEquals("Дата регистрации", participantsHeader.getCell(3).getStringCellValue());
            assertEquals("Наличие канала", participantsHeader.getCell(4).getStringCellValue());

            var mentionsHeader = mentionsSheet.getRow(2);
            assertNotNull(mentionsHeader);
            assertEquals("Дата экспорта", mentionsHeader.getCell(0).getStringCellValue());
            assertEquals("Username", mentionsHeader.getCell(1).getStringCellValue());
            assertEquals("Описание (Bio)", mentionsHeader.getCell(2).getStringCellValue());
            assertEquals("Дата регистрации", mentionsHeader.getCell(3).getStringCellValue());
            assertEquals("Наличие канала", mentionsHeader.getCell(4).getStringCellValue());

            var channelsHeader = channelsSheet.getRow(2);
            assertNotNull(channelsHeader);
            assertEquals("Дата экспорта", channelsHeader.getCell(0).getStringCellValue());
            assertEquals("Username", channelsHeader.getCell(1).getStringCellValue());
            assertEquals("Описание (Bio)", channelsHeader.getCell(2).getStringCellValue());
            assertEquals("Дата регистрации", channelsHeader.getCell(3).getStringCellValue());
            assertEquals("Наличие канала", channelsHeader.getCell(4).getStringCellValue());

            // spot-check data row (starts at row index 3)
            assertEquals("Alice", participantsSheet.getRow(3).getCell(1).getStringCellValue());
            assertEquals("@bob", mentionsSheet.getRow(3).getCell(1).getStringCellValue());
            assertEquals("@my_channel", channelsSheet.getRow(3).getCell(1).getStringCellValue());
        }
    }
}
