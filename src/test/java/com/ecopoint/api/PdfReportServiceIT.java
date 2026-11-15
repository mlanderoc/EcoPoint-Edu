package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import com.ecopoint.service.report.PdfReportService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Baseline report in PDF of the demo studies (spec 006, T510 and T511; R-530,
 * R-532, CA-506, CA-507).
 *
 * @author mlanderoc
 */
@AutoConfigureMockMvc
class PdfReportServiceIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "EcoPoint2026";
    private static final String WATERMARK = "PRELIMINAR";

    @Autowired
    private MockMvc mvc;

    private ApiClient teacher;

    @BeforeEach
    void loadDemoData() throws Exception {
        runScript("002_seed.sql");
        teacher = ApiClient.loggedIn(mvc, "docente.demo@unicesar.edu.co", PASSWORD);
    }

    @Test
    void ca506_closedStudyPdfContainsAllSectionsAndMatchingData() throws Exception {
        long closed = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'CERRADO'", Long.class);
        List<String> pages = pagesOf(download(closed));
        String all = String.join("\n", pages);

        assertTrue(all.contains("Informe de línea base"));
        assertTrue(all.contains("I.E. Prudencia Daza"));
        assertTrue(all.contains("Oficial"));
        for (String section : PdfReportService.SECTIONS) {
            assertTrue(all.contains(section), "Falta la sección " + section);
        }
        assertTrue(all.contains("NMX-AA-015") && all.contains("NMX-AA-019"));
        assertTrue(all.contains("Página 1 de " + pages.size()));
        assertTrue(pages.stream().noneMatch(page -> page.contains(WATERMARK)));
    }

    @Test
    void ca507_preliminaryPdfIncludesWatermarkOnAllPages() throws Exception {
        long running = jdbc.queryForObject("SELECT id FROM ESTUDIO WHERE estado = 'EN_EJECUCION'", Long.class);
        List<String> pages = pagesOf(download(running));

        assertTrue(pages.size() >= 2);
        assertTrue(pages.stream().allMatch(page -> page.contains(WATERMARK)));
        assertFalse(String.join("\n", pages).contains("Estado del informe Oficial"));
    }

    @Test
    void ca207_unknownStudyIsNotFound() throws Exception {
        teacher.perform(get("/api/v1/studies/999999/reports/pdf")).andExpect(status().isNotFound());
    }

    private byte[] download(long study) throws Exception {
        return teacher.perform(get("/api/v1/studies/" + study + "/reports/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/pdf")))
                .andExpect(header().string("Content-Disposition",
                        containsString("ecopoint-informe-linea-base-" + study + ".pdf")))
                .andReturn().getResponse().getContentAsByteArray();
    }

    /** Text of every page, in order. */
    static List<String> pagesOf(byte[] pdf) throws IOException {
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            List<String> pages = new ArrayList<>();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                pages.add(extractor.getTextFromPage(page));
            }
            return pages;
        }
    }
}
