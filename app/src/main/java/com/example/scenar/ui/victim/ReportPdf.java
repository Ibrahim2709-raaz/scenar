package com.example.scenar.ui.victim;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;

final class ReportPdf {
    record Field(String label, String value) {
        String displayValue() {
            return value.trim().isEmpty() ? "Not provided" : value;
        }
    }

    private static final int PAGE_WIDTH = 612;
    private static final int PAGE_HEIGHT = 792;
    private static final int MARGIN = 48;
    private static final int BODY_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final int BODY_BOTTOM = PAGE_HEIGHT - 54;
    private static final int INK = 0xff18251f;
    private static final int GREEN = 0xff19533c;
    private static final int MUTED = 0xff4e5d54;

    private final PdfDocument document;
    private PdfDocument.Page page;
    private int pageNumber;
    private int y;

    static void write(OutputStream output, Instant generatedAt, String summary, List<Field> fields)
            throws IOException {
        PdfDocument document = new PdfDocument();
        try {
            ReportPdf report = new ReportPdf(document);
            try {
                report.startPage();
                report.writeText("SCENAR", 26, INK, Typeface.BOLD);
                report.y += 4;
                report.writeText("Practice incident report", 15, GREEN, Typeface.BOLD);
                report.writeText("Draft for practice only. This report was not sent to emergency services.",
                        10, MUTED, Typeface.NORMAL);
                report.writeText("Generated " + generatedAt, 10, MUTED, Typeface.NORMAL);
                report.y += 20;
                report.writeField(new Field("Report summary", summary));
                for (Field field : fields) report.writeField(field);
            } finally {
                report.finishPage();
            }
            document.writeTo(output);
        } finally {
            document.close();
        }
    }

    private ReportPdf(PdfDocument document) {
        this.document = document;
    }

    private void writeField(Field field) {
        if (y + 42 > BODY_BOTTOM) startPage();
        writeText(field.label(), 11, GREEN, Typeface.BOLD);
        y += 3;
        writeText(field.displayValue(), 11, INK, Typeface.NORMAL);
        y += 13;
    }

    private void writeText(String text, int size, int colour, int style) {
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(size);
        paint.setColor(colour);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, style));
        StaticLayout.Builder builder = StaticLayout.Builder.obtain(text, 0, text.length(), paint, BODY_WIDTH)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setUseLineSpacingFromFallbacks(true);
        }
        StaticLayout layout = builder.build();

        int first = 0;
        while (first < layout.getLineCount()) {
            int top = layout.getLineTop(first);
            int available = BODY_BOTTOM - y;
            int after = first;
            while (after < layout.getLineCount() && layout.getLineTop(after + 1) - top <= available) {
                after++;
            }
            if (after == first) {
                startPage();
                continue;
            }

            int height = layout.getLineTop(after) - top;
            Canvas canvas = page.getCanvas();
            int saved = canvas.save();
            canvas.clipRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + height);
            canvas.translate(MARGIN, y - top);
            layout.draw(canvas);
            canvas.restoreToCount(saved);
            y += height;
            first = after;
            if (first < layout.getLineCount()) startPage();
        }
    }

    private void startPage() {
        finishPage();
        page = document.startPage(new PdfDocument.PageInfo.Builder(
                PAGE_WIDTH, PAGE_HEIGHT, ++pageNumber).create());
        y = MARGIN;
        Paint footer = new Paint(Paint.ANTI_ALIAS_FLAG);
        footer.setColor(MUTED);
        footer.setTextSize(9);
        page.getCanvas().drawText("SCENAR  |  Practice report", MARGIN, PAGE_HEIGHT - 30, footer);
        footer.setTextAlign(Paint.Align.RIGHT);
        page.getCanvas().drawText("Page " + pageNumber, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 30, footer);
    }

    private void finishPage() {
        if (page == null) return;
        document.finishPage(page);
        page = null;
    }
}
