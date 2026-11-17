package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The dashboard, the Excel workbook and the PDF report show exactly the same
 * numbers for the closed demo study (spec 006, T513; R-533).
 *
 * @author mlanderoc
 */
@AutoConfigureMockMvc
class IndicatorCrossConsistencyIT extends PostgresIntegrationTest {

    /** Decimals of weights, percentages and density, and of the PPC (R-511). */
    private static final int SCALE = 2;
    private static final int PPC_SCALE = 3;
    /** Indicator as written in the Excel summary and the PDF, its field in the dashboard JSON and its decimals. */
    private record Indicator(String label, String field, int scale) {
    }

    private static final List<Indicator> SUMMARY = List.of(
            new Indicator("Generación diaria promedio", "averageDailyGeneration", SCALE),
            new Indicator("PPC promedio", "averagePpc", PPC_SCALE),
            new Indicator("Desviación estándar de la PPC", "ppcStandardDeviation", PPC_SCALE),
            new Indicator("CV de la PPC", "ppcVariationCoefficient", SCALE),
            new Indicator("Densidad promedio", "averageDensity", SCALE),
            new Indicator("Fracción aprovechable", "recoverableFraction", SCALE),
            new Indicator("Generación aprovechable estimada", "recoverableDailyGeneration", SCALE));

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private ApiClient teacher;
    private long study;

    @BeforeEach
    void loadDemoData() throws Exception {
        runScript("002_seed.sql");
        teacher = ApiClient.loggedIn(mvc, "docente.demo@unicesar.edu.co", "EcoPoint2026");
        study = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'CERRADO'", Long.class);
    }

    @Test
    void r533_dashboardExcelAndPdfShowTheSameNumbers() throws Exception {
        JsonNode dashboard = json.readTree(download("indicators"));
        String pdf = withoutSpaces(String.join("", PdfReportServiceIT.pagesOf(download("reports/pdf"))));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(download("reports/excel")))) {
            Sheet summary = workbook.getSheet("Resumen Indicadores");
            for (Indicator indicator : SUMMARY) {
                BigDecimal shown = shown(dashboard.path("summary").path(indicator.field()), indicator.scale());
                assertEquals(shown, excelValue(summary, 0, indicator.label(), 1, indicator.scale()), indicator.label());
                assertPdfLine(pdf, indicator.label() + " " + text(shown));
            }

            Sheet sources = workbook.getSheet("Generación por Fuente");
            for (JsonNode source : dashboard.path("sources")) {
                BigDecimal shown = shown(source.path("averageDailyGeneration"), SCALE);
                String code = source.path("code").asText();
                assertEquals(shown, excelValue(sources, 0, code, 2, SCALE), code);
                assertPdfLine(pdf, code + " " + source.path("name").asText() + " " + text(shown));
            }
        }
        for (JsonNode group : dashboard.path("composition").path("groups")) {
            assertPdfLine(pdf, group.path("name").asText() + " " + text(shown(group.path("weight"), SCALE))
                    + " " + text(shown(group.path("percentage"), SCALE)));
        }
        assertPdfLine(pdf, dashboard.path("composition").path("note").asText());
    }

    private byte[] download(String route) throws Exception {
        return teacher.perform(get("/api/v1/studies/" + study + "/" + route))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private static BigDecimal excelValue(Sheet sheet, int keyColumn, String key, int valueColumn, int scale) {
        for (Row row : sheet) {
            if (row.getCell(keyColumn) != null && key.equals(row.getCell(keyColumn).toString())) {
                return BigDecimal.valueOf(row.getCell(valueColumn).getNumericCellValue())
                        .setScale(scale, RoundingMode.HALF_UP);
            }
        }
        throw new AssertionError("El Excel no tiene la fila " + key);
    }

    /**
     * Checks that a table row of the PDF reads as expected. The text extractor
     * joins some cells without a space, so the spaces are ignored.
     */
    private static void assertPdfLine(String pdf, String line) {
        assertTrue(pdf.contains(withoutSpaces(line)), "El PDF no tiene la línea: " + line);
    }

    private static String withoutSpaces(String text) {
        return text.replaceAll("\\s+", "");
    }

    /**
     * Value of the dashboard with the decimals it is shown with. The JSON reader
     * drops trailing zeros (6.50 becomes 6.5), so the scale is set again.
     */
    private static BigDecimal shown(JsonNode value, int scale) {
        return value.decimalValue().setScale(scale, RoundingMode.UNNECESSARY);
    }

    private static String text(BigDecimal value) {
        return value.toPlainString().replace('.', ',');
    }
}
