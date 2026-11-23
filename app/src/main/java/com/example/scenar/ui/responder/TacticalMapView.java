package com.example.scenar.ui.responder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Locale;

public class TacticalMapView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint incidentPinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint incidentPulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint unitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint poiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private double incidentLat = 37.77492;
    private double incidentLng = -122.41941;
    private String incidentLabel = "Incident Site";
    private double distanceKm = 1.4;

    public TacticalMapView(Context context) {
        super(context);
        init();
    }

    public TacticalMapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TacticalMapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(0x223B82F6);
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);

        ringPaint.setColor(0x3310B981);
        ringPaint.setStrokeWidth(2f);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        incidentPinPaint.setColor(0xFFEF4444);
        incidentPinPaint.setStyle(Paint.Style.FILL);

        incidentPulsePaint.setColor(0x44EF4444);
        incidentPulsePaint.setStyle(Paint.Style.FILL);

        unitPaint.setColor(0xFF06B6D4);
        unitPaint.setStyle(Paint.Style.FILL);

        linePaint.setColor(0xAA38BDF8);
        linePaint.setStrokeWidth(3f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setPathEffect(new DashPathEffect(new float[]{12, 8}, 0));

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextSize(28f);
        textPaint.setFakeBoldText(true);

        poiPaint.setColor(0xFF10B981);
        poiPaint.setStyle(Paint.Style.FILL);
    }

    public void setLocation(double lat, double lng, String label) {
        this.incidentLat = lat;
        this.incidentLng = lng;
        this.incidentLabel = label != null ? label : "Incident Site";
        invalidate();
    }

    public void setDistanceKm(double km) {
        this.distanceKm = km;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(width * 0.58f); // 16:9ish tactical ratio
        if (height < 220) height = 220;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // Tactical dark background
        canvas.drawColor(0xFF0F172A);

        // Subtle tactical grid
        int step = 60;
        for (int x = 0; x < w; x += step) {
            canvas.drawLine(x, 0, x, h, gridPaint);
        }
        for (int y = 0; y < h; y += step) {
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        // Center crosshair / range rings
        float centerX = w * 0.5f;
        float centerY = h * 0.5f;
        canvas.drawCircle(centerX, centerY, h * 0.25f, ringPaint);
        canvas.drawCircle(centerX, centerY, h * 0.45f, ringPaint);

        // Incident Target Location (Red beacon with radiating rings)
        float incX = w * 0.62f;
        float incY = h * 0.38f;

        canvas.drawCircle(incX, incY, 32f, incidentPulsePaint);
        canvas.drawCircle(incX, incY, 18f, incidentPulsePaint);
        canvas.drawCircle(incX, incY, 10f, incidentPinPaint);

        // Responder Unit Position (Cyan beacon)
        float unitX = w * 0.28f;
        float unitY = h * 0.72f;

        canvas.drawCircle(unitX, unitY, 14f, unitPaint);
        canvas.drawCircle(unitX, unitY, 6f, incidentPinPaint); // Center indicator

        // Vector path between responder and incident
        canvas.drawLine(unitX, unitY, incX, incY, linePaint);

        // Nearest Hospital POI (Green cross)
        float hospX = w * 0.82f;
        float hospY = h * 0.70f;
        drawHospitalCross(canvas, hospX, hospY);

        // Overlay HUD text labels
        textPaint.setTextSize(24f);
        textPaint.setColor(0xFFEF4444);
        canvas.drawText("INCIDENT: " + incidentLabel, incX + 16f, incY - 12f, textPaint);

        textPaint.setColor(0xFF38BDF8);
        canvas.drawText("UNIT 12 (RESPONDER)", unitX + 16f, unitY + 10f, textPaint);

        textPaint.setColor(0xFF10B981);
        canvas.drawText("ER / TRAUMA CENTER", hospX - 100f, hospY + 32f, textPaint);

        // Tactical HUD telemetry bar at bottom
        textPaint.setTextSize(22f);
        textPaint.setColor(0xFF94A3B8);
        String telemetry = String.format(Locale.ROOT,
                "GPS: %.4f, %.4f | DISTANCE: %.1f KM | ETA: ~%d MIN",
                incidentLat, incidentLng, distanceKm, Math.max(1, (int) Math.round(distanceKm * 2.5)));
        canvas.drawText(telemetry, 24f, h - 20f, textPaint);

        // Compass / Mode badge in top left
        textPaint.setTextSize(20f);
        textPaint.setColor(0xFF38BDF8);
        canvas.drawText("TACTICAL RADAR VIEW — LIVE GPS LOCK", 24f, 36f, textPaint);
    }

    private void drawHospitalCross(Canvas canvas, float cx, float cy) {
        float arm = 14f;
        float thick = 5f;
        canvas.drawRoundRect(new RectF(cx - thick, cy - arm, cx + thick, cy + arm), 2f, 2f, poiPaint);
        canvas.drawRoundRect(new RectF(cx - arm, cy - thick, cx + arm, cy + thick), 2f, 2f, poiPaint);
    }
}
