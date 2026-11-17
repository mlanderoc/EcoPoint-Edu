package com.ecopoint.service.report;

import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.Correction;
import com.ecopoint.model.Quartering;
import com.ecopoint.model.QuarteringResult;
import com.ecopoint.model.SamplingShift;
import com.ecopoint.model.User;
import com.ecopoint.model.WasteBag;
import com.ecopoint.service.IndicatorService;
import com.ecopoint.service.StudyIndicators;
import com.ecopoint.service.StudyReport;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Excel workbook of a study with one sheet per topic (spec 006, R-531,
 * CA-508). The numbers are the same rounded values the dashboard shows (R-533).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Service
public class ExcelReportService {

    /** Sheet names, in order (R-531). */
    public static final List<String> SHEETS = List.of("Resumen Indicadores", "Generación Diaria",
            "Generación por Fuente", "Población Censada", "Registro de Bolsas", "Cuarteos y Composición",
            "Densidades de Turno", "Historial de Correcciones");

    private final IndicatorService indicators;

    /**
     * Creates the service.
     *
     * @param indicators indicator service that loads and calculates the data
     */
    public ExcelReportService(IndicatorService indicators) {
        this.indicators = indicators;
    }

    /**
     * Builds the workbook of a study.
     *
     * @param actor   user asking
     * @param studyId study id
     * @return the .xlsx file
     */
    public byte[] generate(User actor, Long studyId) {
        return write(indicators.report(actor, studyId));
    }

    /**
     * Writes the workbook of already loaded report data.
     *
     * @param report data of the study
     * @return the .xlsx file
     */
    public byte[] write(StudyReport report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            summary(workbook.createSheet(SHEETS.get(0)), styles, report);
            daily(workbook.createSheet(SHEETS.get(1)), styles, report.indicators());
            sources(workbook.createSheet(SHEETS.get(2)), styles, report.indicators());
            population(workbook.createSheet(SHEETS.get(3)), styles, report.shifts());
            bags(workbook.createSheet(SHEETS.get(4)), styles, report.shifts());
            quarterings(workbook.createSheet(SHEETS.get(5)), styles, report.shifts());
            densities(workbook.createSheet(SHEETS.get(6)), styles, report.shifts());
            corrections(workbook.createSheet(SHEETS.get(7)), styles, report.corrections());
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new PersistenceException("No se pudo generar el archivo Excel.", e);
        }
    }

    private static void summary(Sheet sheet, Styles styles, StudyReport report) {
        StudyIndicators.Summary summary = report.indicators().summary();
        String[][] info = {
                {"Institución", report.institutionName()},
                {"Estudio", String.valueOf(report.study().getId())},
                {"Fechas", report.study().getStartDate() + " a " + report.study().getEndDate()},
                {"Estado", report.indicators().official() ? "Oficial" : "Preliminar – datos sin depurar"},
        };
        int row = 0;
        for (String[] line : info) {
            Row current = sheet.createRow(row++);
            styles.text(current, 0, line[0], styles.header);
            styles.text(current, 1, line[1], null);
        }
        row++;
        header(sheet, styles, row++, "Indicador", "Valor", "Unidad");
        row = indicator(sheet, styles, row, "Generación diaria promedio", ReportNumbers.weight(summary.averageDailyGeneration()), "kg/día", false);
        row = indicator(sheet, styles, row, "PPC promedio", ReportNumbers.ppc(summary.averagePpc()), "kg/persona/día", true);
        row = indicator(sheet, styles, row, "Desviación estándar de la PPC", ReportNumbers.ppc(summary.ppcStandardDeviation()), "kg/persona/día", true);
        row = indicator(sheet, styles, row, "CV de la PPC", ReportNumbers.weight(summary.ppcVariationCoefficient()), "%", false);
        row = indicator(sheet, styles, row, "Densidad promedio", ReportNumbers.weight(summary.averageDensity()), "kg/m³", false);
        row = indicator(sheet, styles, row, "Fracción aprovechable", ReportNumbers.weight(summary.recoverableFraction()), "%", false);
        row = indicator(sheet, styles, row, "Generación aprovechable estimada", ReportNumbers.weight(summary.recoverableDailyGeneration()), "kg/día", false);
        indicator(sheet, styles, row, "Días que entran en el cálculo", BigDecimal.valueOf(summary.countedDays()), "días", false);
        sheet.createRow(row + 2).createCell(0).setCellValue(ReportNumbers.compositionNote(report.indicators().composition()));
        fit(sheet, 3);
    }

    private static int indicator(Sheet sheet, Styles styles, int row, String name, BigDecimal value, String unit, boolean ppc) {
        Row current = sheet.createRow(row);
        styles.text(current, 0, name, null);
        styles.number(current, 1, value, ppc ? styles.ppc : styles.decimal);
        styles.text(current, 2, unit, null);
        return row + 1;
    }

    private static void daily(Sheet sheet, Styles styles, StudyIndicators indicators) {
        header(sheet, styles, 0, "Día", "Fecha", "Mañana (kg)", "Tarde (kg)", "Total (kg)", "Población", "PPC (kg/persona/día)", "Observación");
        int row = 1;
        for (StudyIndicators.DailyGeneration day : indicators.days()) {
            Row current = sheet.createRow(row++);
            styles.number(current, 0, BigDecimal.valueOf(day.dayNumber()), null);
            styles.text(current, 1, day.date() == null ? "" : day.date().toString(), null);
            styles.number(current, 2, ReportNumbers.weight(day.morningGeneration()), styles.decimal);
            styles.number(current, 3, ReportNumbers.weight(day.afternoonGeneration()), styles.decimal);
            styles.number(current, 4, ReportNumbers.weight(day.generation()), styles.decimal);
            styles.number(current, 5, BigDecimal.valueOf(day.population()), null);
            styles.number(current, 6, ReportNumbers.ppc(day.ppc()), styles.ppc);
            styles.text(current, 7, day.excluded() ? "Día de descarte: no entra en los cálculos" : "", null);
        }
        fit(sheet, 8);
    }

    private static void sources(Sheet sheet, Styles styles, StudyIndicators indicators) {
        header(sheet, styles, 0, "Código", "Fuente", "Generación promedio (kg/día)");
        int row = 1;
        for (StudyIndicators.SourceGeneration source : indicators.sources()) {
            Row current = sheet.createRow(row++);
            styles.text(current, 0, source.code(), null);
            styles.text(current, 1, source.name(), null);
            styles.number(current, 2, ReportNumbers.weight(source.averageDailyGeneration()), styles.decimal);
        }
        fit(sheet, 3);
    }

    private static void population(Sheet sheet, Styles styles, List<SamplingShift> shifts) {
        header(sheet, styles, 0, "Turno", "Estudiantes", "Docentes", "Personal", "Total", "Verificación");
        int row = 1;
        for (SamplingShift shift : shifts) {
            if (shift.getPopulation() == null) {
                continue;
            }
            Row current = sheet.createRow(row++);
            styles.text(current, 0, shift.getCode(), null);
            styles.number(current, 1, BigDecimal.valueOf(shift.getPopulation().getStudents()), null);
            styles.number(current, 2, BigDecimal.valueOf(shift.getPopulation().getTeachers()), null);
            styles.number(current, 3, BigDecimal.valueOf(shift.getPopulation().getStaff()), null);
            styles.number(current, 4, BigDecimal.valueOf(shift.getPopulation().getTotal()), null);
            styles.text(current, 5, shift.getPopulation().getVerification().getStatus().name(), null);
        }
        fit(sheet, 6);
    }

    private static void bags(Sheet sheet, Styles styles, List<SamplingShift> shifts) {
        header(sheet, styles, 0, "Día", "Turno", "Fuente", "Rótulo", "Peso bruto (kg)", "Tara (kg)", "Peso neto (kg)",
                "Volumen (m³)", "Atípica", "Verificación");
        int row = 1;
        for (SamplingShift shift : shifts) {
            for (WasteBag bag : shift.getBags()) {
                Row current = sheet.createRow(row++);
                styles.number(current, 0, BigDecimal.valueOf(shift.getDayNumber()), null);
                styles.text(current, 1, shift.getCode(), null);
                styles.text(current, 2, bag.getSourceCode(), null);
                styles.text(current, 3, bag.getLabelText(), null);
                styles.number(current, 4, bag.getGrossWeight(), styles.decimal);
                styles.number(current, 5, bag.getTare(), styles.decimal);
                styles.number(current, 6, bag.getNetWeight(), styles.decimal);
                styles.number(current, 7, bag.getVolume(), styles.ppc);
                styles.text(current, 8, bag.isAtypical() ? "Sí" : "No", null);
                styles.text(current, 9, bag.getVerification().getStatus().name(), null);
            }
        }
        fit(sheet, 10);
    }

    private static void quarterings(Sheet sheet, Styles styles, List<SamplingShift> shifts) {
        header(sheet, styles, 0, "Turno", "Estado", "Submuestra (kg)", "Subcategoría", "Grupo", "Peso (kg)");
        int row = 1;
        for (SamplingShift shift : shifts) {
            Quartering quartering = shift.getQuartering();
            if (quartering == null) {
                continue;
            }
            for (QuarteringResult result : quartering.getResults()) {
                Row current = sheet.createRow(row++);
                styles.text(current, 0, shift.getCode(), null);
                styles.text(current, 1, quartering.getQualityStatus() == null ? "" : quartering.getQualityStatus().name(), null);
                styles.number(current, 2, quartering.getSampleWeight(), styles.decimal);
                styles.text(current, 3, result.getCategoryName(), null);
                styles.text(current, 4, result.getGroup().getDisplayName(), null);
                styles.number(current, 5, result.getWeight(), styles.decimal);
            }
        }
        fit(sheet, 6);
    }

    private static void densities(Sheet sheet, Styles styles, List<SamplingShift> shifts) {
        header(sheet, styles, 0, "Turno", "Volumen del recipiente (m³)", "Peso neto (kg)", "Densidad (kg/m³)", "Verificación");
        int row = 1;
        for (SamplingShift shift : shifts) {
            if (shift.getDensity() == null) {
                continue;
            }
            Row current = sheet.createRow(row++);
            styles.text(current, 0, shift.getCode(), null);
            styles.number(current, 1, shift.getDensity().getContainerVolume(), styles.ppc);
            styles.number(current, 2, shift.getDensity().getNetWeight(), styles.decimal);
            styles.number(current, 3, ReportNumbers.weight(shift.getDensity().getDensity()), styles.decimal);
            styles.text(current, 4, shift.getDensity().getVerification().getStatus().name(), null);
        }
        fit(sheet, 5);
    }

    private static void corrections(Sheet sheet, Styles styles, List<Correction> history) {
        header(sheet, styles, 0, "Fecha", "Registro", "Id", "Campo", "Valor anterior", "Valor nuevo", "Motivo", "Usuario");
        int row = 1;
        for (Correction correction : history) {
            Row current = sheet.createRow(row++);
            styles.text(current, 0, correction.at().toString(), null);
            styles.text(current, 1, correction.recordType(), null);
            styles.number(current, 2, BigDecimal.valueOf(correction.recordId()), null);
            styles.text(current, 3, correction.field(), null);
            styles.text(current, 4, correction.oldValue(), null);
            styles.text(current, 5, correction.newValue(), null);
            styles.text(current, 6, correction.reason(), null);
            styles.number(current, 7, BigDecimal.valueOf(correction.userId()), null);
        }
        fit(sheet, 8);
    }

    private static void header(Sheet sheet, Styles styles, int row, String... titles) {
        Row current = sheet.createRow(row);
        for (int column = 0; column < titles.length; column++) {
            styles.text(current, column, titles[column], styles.header);
        }
        sheet.createFreezePane(0, row + 1);
    }

    private static void fit(Sheet sheet, int columns) {
        for (int column = 0; column < columns; column++) {
            sheet.autoSizeColumn(column);
        }
    }

    /** Cell styles of the workbook: green header, thin borders, 0.00 and 0.000. */
    private static final class Styles {

        private final CellStyle header;
        private final CellStyle decimal;
        private final CellStyle ppc;

        Styles(Workbook workbook) {
            Font bold = workbook.createFont();
            bold.setBold(true);
            bold.setColor(IndexedColors.WHITE.getIndex());
            header = bordered(workbook);
            header.setFont(bold);
            header.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            decimal = bordered(workbook);
            decimal.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            ppc = bordered(workbook);
            ppc.setDataFormat(workbook.createDataFormat().getFormat("0.000"));
        }

        private static CellStyle bordered(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            return style;
        }

        void text(Row row, int column, String value, CellStyle style) {
            var cell = row.createCell(column);
            cell.setCellValue(value == null ? "" : value);
            if (style != null) {
                cell.setCellStyle(style);
            }
        }

        void number(Row row, int column, BigDecimal value, CellStyle style) {
            var cell = row.createCell(column);
            if (value != null) {
                cell.setCellValue(value.doubleValue());
            }
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }
}
