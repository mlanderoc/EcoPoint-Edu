package com.ecopoint.service.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Phrase;
import com.lowagie.text.Font;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Decoration of every page of the PDF report (spec 006, R-532, CA-507): the
 * footer "Página X de Y" with the generation date and, when the study is not
 * closed, the diagonal watermark "PRELIMINAR – datos sin depurar".
 *
 * @author mlanderoc
 * @version 1.0
 */
class ReportPageEvents extends PdfPageEventHelper {

    /** Text of the watermark of a preliminary report (R-532). */
    static final String WATERMARK = "PRELIMINAR – datos sin depurar";

    private static final Font FOOTER = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final DateTimeFormatter GENERATED = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float WATERMARK_SIZE = 46;
    private static final float WATERMARK_OPACITY = 0.18f;

    private final boolean preliminary;
    private final String generatedAt;
    private final int totalPages;
    private BaseFont font;

    /**
     * Creates the page decoration.
     *
     * @param preliminary true to stamp the watermark on every page
     * @param generatedAt date and time of the report
     * @param totalPages  number of pages of the report, or 0 while it is still unknown
     */
    ReportPageEvents(boolean preliminary, LocalDateTime generatedAt, int totalPages) {
        this.preliminary = preliminary;
        this.generatedAt = GENERATED.format(generatedAt);
        this.totalPages = totalPages;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        font = FOOTER.getCalculatedBaseFont(false);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        if (preliminary) {
            watermark(writer.getDirectContent(), document.getPageSize());
        }
        footer(writer, document);
    }

    private void watermark(PdfContentByte canvas, Rectangle page) {
        canvas.saveState();
        PdfGState transparent = new PdfGState();
        transparent.setFillOpacity(WATERMARK_OPACITY);
        canvas.setGState(transparent);
        canvas.beginText();
        canvas.setFontAndSize(font, WATERMARK_SIZE);
        canvas.setColorFill(Color.GRAY);
        canvas.showTextAligned(Element.ALIGN_CENTER, WATERMARK, page.getWidth() / 2, page.getHeight() / 2, 45);
        canvas.endText();
        canvas.restoreState();
    }

    private void footer(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();
        float y = document.bottom() - 25;
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                new Phrase("EcoPoint Edu · Generado el " + generatedAt, FOOTER), document.left(), y, 0);
        String page = "Página " + writer.getPageNumber() + (totalPages > 0 ? " de " + totalPages : "");
        ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase(page, FOOTER), document.right(), y, 0);
    }
}
