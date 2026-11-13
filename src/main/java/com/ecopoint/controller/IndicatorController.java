package com.ecopoint.controller;

import com.ecopoint.dto.StudyIndicatorsDTO;
import com.ecopoint.security.EcoPointUserDetails;
import com.ecopoint.service.IndicatorService;
import com.ecopoint.service.report.ExcelReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Baseline indicators of a study (spec 006, R-520 to R-526). The permission
 * VIEW_INDICATORS is checked by the service (RN-081).
 *
 * @author craulgomez
 * @version 1.0
 */
@RestController
public class IndicatorController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final IndicatorService indicators;
    private final ExcelReportService excelReports;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param indicators   indicator service
     * @param excelReports Excel report service
     * @param currentUser  user of the session
     */
    public IndicatorController(IndicatorService indicators, ExcelReportService excelReports, CurrentUser currentUser) {
        this.indicators = indicators;
        this.excelReports = excelReports;
        this.currentUser = currentUser;
    }

    /**
     * Returns the indicators of a study, labelled "Preliminar" or "Oficial" (R-502).
     *
     * @param principal user of the session
     * @param id        study id
     * @return the rounded indicators
     */
    @GetMapping("/api/v1/studies/{id}/indicators")
    public StudyIndicatorsDTO getStudyIndicators(@AuthenticationPrincipal EcoPointUserDetails principal,
                                                 @PathVariable Long id) {
        return StudyIndicatorsDTO.from(indicators.calculate(currentUser.of(principal), id));
    }

    /**
     * Downloads the Excel workbook of a study (R-531, CA-508).
     *
     * @param principal user of the session
     * @param id        study id
     * @return the .xlsx file as an attachment
     */
    @GetMapping("/api/v1/studies/{id}/reports/excel")
    public ResponseEntity<byte[]> downloadExcel(@AuthenticationPrincipal EcoPointUserDetails principal,
                                                @PathVariable Long id) {
        return attachment(excelReports.generate(currentUser.of(principal), id), XLSX,
                "ecopoint-reporte-estudio-" + id + ".xlsx");
    }

    private static ResponseEntity<byte[]> attachment(byte[] file, MediaType type, String fileName) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(file);
    }
}
