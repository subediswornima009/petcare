package com.swornima.petcare;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Small dependency-free bar chart used on the Expenses screen to visualise
 * recent monthly pet-care spending.
 */
public class ExpenseBarChartView extends View {

    public static class Bar {
        final String label;
        final double value;

        public Bar(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Bar> bars = new ArrayList<>();

    public ExpenseBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(Color.parseColor("#12726A"));
        accentPaint.setColor(Color.parseColor("#F58B3D"));
        textPaint.setColor(Color.parseColor("#6B7A72"));
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        gridPaint.setColor(Color.parseColor("#E7EFE9"));
        gridPaint.setStrokeWidth(2f);
    }

    public void setBars(List<Bar> values) {
        bars.clear();
        if (values != null) {
            bars.addAll(values);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bars.isEmpty()) {
            canvas.drawText("No monthly data yet", getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }

        float left = 28f;
        float right = getWidth() - 28f;
        float top = 20f;
        float bottom = getHeight() - 42f;
        float chartHeight = bottom - top;
        double max = 0;
        for (Bar bar : bars) {
            max = Math.max(max, bar.value);
        }
        if (max <= 0) max = 1;

        canvas.drawLine(left, bottom, right, bottom, gridPaint);

        float slot = (right - left) / bars.size();
        float barWidth = Math.max(18f, slot * 0.48f);
        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            float center = left + slot * i + slot / 2f;
            float barHeight = (float) ((bar.value / max) * chartHeight);
            float barTop = bottom - barHeight;
            Paint paint = i == bars.size() - 1 ? accentPaint : barPaint;
            canvas.drawRoundRect(center - barWidth / 2f, barTop, center + barWidth / 2f, bottom, 10f, 10f, paint);
            canvas.drawText(bar.label, center, getHeight() - 10f, textPaint);
            if (bar.value > 0) {
                canvas.drawText(String.format(Locale.getDefault(), "%.0f", bar.value), center, barTop - 8f, textPaint);
            }
        }
    }
}
