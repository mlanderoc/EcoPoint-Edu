package com.ecopoint.service.report;

import com.ecopoint.model.CharacterizationSettings;
import com.ecopoint.exceptions.PersistenceException;
import com.ecopoint.model.User;
import com.ecopoint.service.IndicatorService;
import com.ecopoint.service.StudyIndicators;
import com.ecopoint.service.StudyReport;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Formal baseline report of a study in PDF (spec 006, R-530, R-532, CA-506,
 * CA-507): cover with the logos, summarized methodology, baseline indicators,
 * composition, generation by source, comparison by shift and data quality. A
 * study that is not closed carries the "PRELIMINAR" watermark on every page.
 * The numbers are the same rounded values the dashboard shows (R-533).
 *
 * @author mlanderoc
 * @version 1.0
 */
@Service
public class PdfReportService {

    /** Section titles, in order (R-530). */
    public static final List<String> SECTIONS = List.of("1. Metodología", "2. Indicadores de línea base",
            "3. Composición física", "4. Generación por fuente", "5. Comparación por turno", "6. Calidad del dato");

    static final Color BRAND = new Color(0x1E, 0x84, 0x49);
    static final Color BRAND_DARK = new Color(0x0F, 0x3D, 0x23);
    private static final Color LIGHT = new Color(0xE8, 0xF5, 0xE9);
    private static final Font TITLE = new Font(Font.HELVETICA, 22, Font.BOLD, BRAND_DARK);
    private static final Font SUBTITLE = new Font(Font.HELVETICA, 13, Font.NORMAL, BRAND_DARK);
    private static final Font HEADING = new Font(Font.HELVETICA, 14, Font.BOLD, BRAND_DARK);
    private static final Font BODY = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font BOLD = new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);
    private static final Font HEADER = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font CELL = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy",
            Locale.forLanguageTag("es-CO"));
    private static final String[] LOGOS = {"reports/logo_upc.jpg", "logo_ecopoint.jpeg"};
    private static final String METHODOLOGY = "El estudio sigue el protocolo de caracterización participativa: "
            + "8 días de muestreo con turnos de mañana y tarde. Los residuos del día 1 se acumularon antes del "
            + "estudio, por eso se pesan pero no entran en los cálculos (n = 7 días). Cada bolsa se pesa por fuente "
            + "de generación; la composición se obtiene por cuarteo de una submuestra de %s a %s kg "
            + "(NMX-AA-015) y la densidad con un recipiente de volumen conocido (NMX-AA-019). Un cuarteo es "
            + "conforme si la suma clasificada difiere de la submuestra en %s %% o menos. Cada dato lo verifica una "
            + "persona distinta de quien lo digitó.";

    private final IndicatorService indicators;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param indicators indicator service that loads and calculates the data
     * @param clock      clock of the application, for the generation date
     */
    public PdfReportService(IndicatorService indicators, Clock clock) {
        this.indicators = indicators;
        this.clock = clock;
    }

    /**
     * Builds the PDF report of a study.
     *
     * @param actor   user asking
     * @param studyId study id
     * @return the .pdf file
     */
    public byte[] generate(User actor, Long studyId) {
        return write(indicators.report(actor, studyId), LocalDateTime.now(clock));
    }

    /**
     * Writes the PDF report of already loaded report data.
     *
     * @param report      data of the study
     * @param generatedAt date and time printed on every page
     * @return the .pdf file
     */
    public byte[] write(StudyReport report, LocalDateTime generatedAt) {
        byte[] draft = render(report, generatedAt, 0);
        try (PdfReader reader = new PdfReader(draft)) {
            return render(report, generatedAt, reader.getNumberOfPages());
        } catch (IOException e) {
            throw new PersistenceException("No se pudo generar el informe PDF.", e);
        }
    }

    /**
     * Renders the report. It is rendered twice: the first pass counts the pages
     * so that the second one prints "Página X de Y" as plain text.
     */
    private static byte[] render(StudyReport report, LocalDateTime generatedAt, int totalPages) {
        Document document = new Document(PageSize.LETTER, 50, 50, 60, 60);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new ReportPageEvents(!report.indicators().official(), generatedAt, totalPages));
            document.addTitle("Informe de línea base - Estudio " + report.study().getId());
            document.addCreator("EcoPoint Edu");
            document.open();
            cover(document, report, generatedAt.toLocalDate());
            document.newPage();
            methodology(document, report.settings());
            baseline(document, report.indicators());
            composition(document, report.indicators().composition());
            sources(document, report.indicators());
            shifts(document, report.indicators());
            quality(document, report.indicators().quality());
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new PersistenceException("No se pudo generar el informe PDF.", e);
        }
    }

    private static void cover(Document document, StudyReport report, LocalDate generatedOn) throws DocumentException, IOException {
        PdfPTable logos = new PdfPTable(LOGOS.length);
        logos.setWidthPercentage(70);
        for (String logo : LOGOS) {
            PdfPCell cell = new PdfPCell(logo(logo), false);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setFixedHeight(90);
            logos.addCell(cell);
        }
        document.add(logos);
        document.add(spaced(report.institutionName(), SUBTITLE, Element.ALIGN_CENTER, 30));
        document.add(spaced("Informe de línea base", TITLE, Element.ALIGN_CENTER, 20));
        document.add(spaced("Caracterización de residuos sólidos", SUBTITLE, Element.ALIGN_CENTER, 6));

        PdfPTable data = new PdfPTable(new float[]{1, 2});
        data.setWidthPercentage(80);
        data.setSpacingBefore(40);
        row(data, "Estudio", "N.º " + report.study().getId());
        row(data, "Período", text(report.study().getStartDate()) + " a " + text(report.study().getEndDate()));
        row(data, "Estado del informe", report.indicators().official()
                ? "Oficial" : "Preliminar – datos sin depurar");
        row(data, "Equipo responsable", report.crews().isEmpty() ? "Sin cuadrillas registradas"
                : String.join(", ", report.crews()));
        row(data, "Fecha de generación", text(generatedOn));
        document.add(data);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static void methodology(Document document, CharacterizationSettings settings) throws DocumentException {
        document.add(heading(SECTIONS.get(0)));
        Paragraph paragraph = new Paragraph(METHODOLOGY.formatted(plain(settings.minSampleWeight()),
                plain(settings.maxSampleWeight()), plain(settings.qualityTolerancePercent())), BODY);
        paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
        paragraph.setLeading(14);
        document.add(paragraph);
    }

    private static void baseline(Document document, StudyIndicators indicators) throws DocumentException {
        StudyIndicators.Summary summary = indicators.summary();
        document.add(heading(SECTIONS.get(1)));
        PdfPTable table = table(new float[]{3, 1.4f, 1.6f}, "Indicador", "Valor", "Unidad");
        values(table, "Generación diaria promedio", number(ReportNumbers.weight(summary.averageDailyGeneration())), "kg/día");
        values(table, "PPC promedio", number(ReportNumbers.ppc(summary.averagePpc())), "kg/persona/día");
        values(table, "Desviación estándar de la PPC", number(ReportNumbers.ppc(summary.ppcStandardDeviation())), "kg/persona/día");
        values(table, "CV de la PPC", number(ReportNumbers.weight(summary.ppcVariationCoefficient())), "%");
        values(table, "Densidad promedio", number(ReportNumbers.weight(summary.averageDensity())), "kg/m³");
        values(table, "Fracción aprovechable", number(ReportNumbers.weight(summary.recoverableFraction())), "%");
        values(table, "Generación aprovechable estimada", number(ReportNumbers.weight(summary.recoverableDailyGeneration())), "kg/día");
        values(table, "Días que entran en el cálculo", String.valueOf(summary.countedDays()), "días");
        document.add(table);
    }

    private static void composition(Document document, StudyIndicators.Composition composition) throws DocumentException {
        document.add(heading(SECTIONS.get(2)));
        document.add(new Paragraph(ReportNumbers.compositionNote(composition), BODY));
        PdfPTable groups = table(new float[]{2, 1, 1}, "Grupo", "Peso (kg)", "Porcentaje (%)");
        for (StudyIndicators.GroupShare group : composition.groups()) {
            values(groups, group.group().getDisplayName(), number(ReportNumbers.weight(group.weight())),
                    number(ReportNumbers.weight(group.percentage())));
        }
        document.add(groups);
        PdfPTable subcategories = table(new float[]{2.4f, 1.2f, 1, 1}, "Subcategoría", "Grupo", "Peso (kg)", "Porcentaje (%)");
        for (StudyIndicators.SubcategoryShare share : composition.subcategories()) {
            values(subcategories, share.name(), share.group().getDisplayName(),
                    number(ReportNumbers.weight(share.weight())), number(ReportNumbers.weight(share.percentage())));
        }
        document.add(subcategories);
    }

    private static void sources(Document document, StudyIndicators indicators) throws DocumentException {
        document.add(heading(SECTIONS.get(3)));
        PdfPTable table = table(new float[]{1, 3, 1.6f}, "Código", "Fuente", "Promedio diario (kg)");
        for (StudyIndicators.SourceGeneration source : indicators.sources()) {
            values(table, source.code(), source.name(), number(ReportNumbers.weight(source.averageDailyGeneration())));
        }
        document.add(table);
    }

    private static void shifts(Document document, StudyIndicators indicators) throws DocumentException {
        document.add(heading(SECTIONS.get(4)));
        PdfPTable table = table(new float[]{1, 1.3f, 1.2f, 1.3f, 1.3f},
                "Turno", "Generación (kg)", "Población", "PPC (kg/persona)", "Densidad (kg/m³)");
        for (StudyIndicators.ShiftTypeComparison shift : indicators.shiftTypes()) {
            values(table, shift.type().getDisplayName(), number(ReportNumbers.weight(shift.averageGeneration())),
                    number(ReportNumbers.weight(shift.averagePopulation())), number(ReportNumbers.ppc(shift.averagePpc())),
                    number(ReportNumbers.weight(shift.averageDensity())));
        }
        document.add(table);
    }

    private static void quality(Document document, StudyIndicators.DataQuality quality) throws DocumentException {
        document.add(heading(SECTIONS.get(5)));
        PdfPTable table = table(new float[]{3, 1}, "Aspecto", "Valor");
        values(table, "Registros", String.valueOf(quality.totalRecords()));
        values(table, "Registros verificados", String.valueOf(quality.verifiedRecords()));
        values(table, "Porcentaje verificado (%)", number(ReportNumbers.weight(quality.verifiedPercentage())));
        values(table, "Registros corregidos", String.valueOf(quality.correctedRecords()));
        values(table, "Cuarteos no conformes", String.valueOf(quality.nonConformingQuarterings()));
        document.add(table);
    }

    /**
     * Writes a number as the dashboard shows it: comma as decimal separator, and
     * a dash when there is no value.
     *
     * @param value rounded value, or null
     * @return the text, e.g. "14,21"
     */
    static String number(BigDecimal value) {
        return value == null ? "—" : value.toPlainString().replace('.', ',');
    }

    private static boolean isNumber(String value) {
        return value.equals("—") || value.matches("-?\\d+(,\\d+)?");
    }

    private static String text(LocalDate date) {
        return date == null ? "sin fecha" : DATE.format(date);
    }

    private static Image logo(String path) throws IOException, DocumentException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            Image image = Image.getInstance(in.readAllBytes());
            image.scaleToFit(120, 80);
            return image;
        }
    }

    private static Paragraph heading(String title) {
        Paragraph heading = new Paragraph(title, HEADING);
        heading.setSpacingBefore(18);
        heading.setSpacingAfter(8);
        return heading;
    }

    private static Paragraph spaced(String text, Font font, int alignment, float before) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(alignment);
        paragraph.setSpacingBefore(before);
        return paragraph;
    }

    private static void row(PdfPTable table, String label, String value) {
        PdfPCell name = new PdfPCell(new Phrase(label, BOLD));
        name.setBackgroundColor(LIGHT);
        name.setPadding(7);
        table.addCell(name);
        PdfPCell content = new PdfPCell(new Phrase(value, BODY));
        content.setPadding(7);
        table.addCell(content);
    }

    private static PdfPTable table(float[] widths, String... headers) throws DocumentException {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(6);
        table.setHeaderRows(1);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER));
            cell.setBackgroundColor(BRAND);
            cell.setPadding(5);
            table.addCell(cell);
        }
        return table;
    }

    private static void values(PdfPTable table, String... values) {
        for (int column = 0; column < values.length; column++) {
            PdfPCell cell = new PdfPCell(new Phrase(new Chunk(values[column], CELL)));
            cell.setPadding(4);
            cell.setHorizontalAlignment(isNumber(values[column]) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }
}
