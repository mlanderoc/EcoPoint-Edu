package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import com.ecopoint.service.report.ExcelReportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Excel workbook of the demo study (spec 006, T508 and T509; R-531, CA-508).
 *
 * @author mlanderoc
 */
@AutoConfigureMockMvc
class ExcelReportServiceIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "EcoPoint2026";
    private static final BigDecimal COUNTED_DAYS = BigDecimal.valueOf(7);

    @Autowired
    private MockMvc mvc;

    private ApiClient teacher;
    private long closedStudy;

    @BeforeEach
    void loadDemoData() throws Exception {
        runScript("002_seed.sql");
        teacher = ApiClient.loggedIn(mvc, "docente.demo@unicesar.edu.co", PASSWORD);
        closedStudy = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'CERRADO'", Long.class);
    }

    @Test
    void ca508_excelSheetsAndBagsSumMatchesAverage() throws Exception {
        byte[] file = teacher.perform(get("/api/v1/studies/" + closedStudy + "/reports/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition",
                        containsString("ecopoint-reporte-estudio-" + closedStudy + ".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            for (int index = 0; index < ExcelReportService.SHEETS.size(); index++) {
                assertEquals(ExcelReportService.SHEETS.get(index), workbook.getSheetName(index));
            }
            BigDecimal bagsTotal = BigDecimal.ZERO;
            Sheet bags = workbook.getSheet("Registro de Bolsas");
            for (Row row : bags) {
                if (row.getRowNum() > 0 && row.getCell(0).getNumericCellValue() >= 2) {
                    bagsTotal = bagsTotal.add(BigDecimal.valueOf(row.getCell(6).getNumericCellValue()));
                }
            }
            assertTrue(bagsTotal.signum() > 0);
            BigDecimal expected = bagsTotal.divide(COUNTED_DAYS, 2, RoundingMode.HALF_UP);
            assertEquals(expected, summaryValue(workbook.getSheet("Resumen Indicadores"), "Generación diaria promedio"));
        }
    }

    @Test
    void ca207_unknownStudyIsNotFound() throws Exception {
        teacher.perform(get("/api/v1/studies/999999/reports/excel")).andExpect(status().isNotFound());
    }

    private static BigDecimal summaryValue(Sheet sheet, String indicator) {
        for (Row row : sheet) {
            if (row.getCell(0) != null && indicator.equals(row.getCell(0).getStringCellValue())) {
                return BigDecimal.valueOf(row.getCell(1).getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
            }
        }
        throw new AssertionError("Falta el indicador " + indicator);
    }
}
