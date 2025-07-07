package com.example.tradeup.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.tradeup.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BarChartView extends View {

    private Paint barPaint;
    private Paint textPaint;
    private Paint linePaint;
    private Map<String, Long> chartData; // Key: Day (Mon, Tue), Value: Revenue
    private long maxRevenue = 0;
    private List<String> orderedKeys; // To maintain order of days

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint();
        barPaint.setColor(Color.parseColor("#F6D6AD")); // orange_bold
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24f); // Adjust text size as needed
        textPaint.setTextAlign(Paint.Align.CENTER);

        linePaint = new Paint();
        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(2f); // Thicker line for axis
    }

    public void setChartData(Map<String, Long> data) {
        this.chartData = data;
        maxRevenue = 0;
        orderedKeys = new ArrayList<>(data.keySet()); // Get keys to maintain order
        // Sort keys based on typical week order (Mon-Sun)
        Collections.sort(orderedKeys, new Comparator<String>() {
            private final List<String> dayOrder = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
            @Override
            public int compare(String o1, String o2) {
                return dayOrder.indexOf(o1) - dayOrder.indexOf(o2);
            }
        });

        // Calculate max revenue for scaling
        for (Long revenue : data.values()) {
            if (revenue > maxRevenue) {
                maxRevenue = revenue;
            }
        }
        // Add some padding to maxRevenue for better visual scaling
        if (maxRevenue > 0) {
            maxRevenue = (long) (maxRevenue * 1.2); // 20% padding
        } else {
            maxRevenue = 1000L; // Default max if no data
        }

        invalidate(); // Redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (chartData == null || chartData.isEmpty()) {
            // Display "No data" message
            String noDataText = getContext().getString(R.string.chart_placeholder_text);
            canvas.drawText(noDataText, getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }

        int padding = 50; // Padding from edges
        int axisTextPadding = 20; // Padding for axis labels
        int chartHeight = getHeight() - 2 * padding - (int)textPaint.getTextSize() - axisTextPadding;
        int chartWidth = getWidth() - 2 * padding;

        // Draw Y-axis (Revenue)
        canvas.drawLine(padding, padding, padding, padding + chartHeight, linePaint);
        // Draw X-axis (Days)
        canvas.drawLine(padding, padding + chartHeight, padding + chartWidth, padding + chartHeight, linePaint);

        // Draw Y-axis labels (min, mid, max revenue)
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        currencyFormat.setMaximumFractionDigits(0);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(20f); // Smaller text for axis labels

        // Max Y-axis label
        canvas.drawText(currencyFormat.format(maxRevenue), padding - axisTextPadding, padding + textPaint.getTextSize() / 2, textPaint);
        // Mid Y-axis label
        canvas.drawText(currencyFormat.format(maxRevenue / 2), padding - axisTextPadding, padding + chartHeight / 2 + textPaint.getTextSize() / 2, textPaint);
        // Min Y-axis label (0)
        canvas.drawText(currencyFormat.format(0), padding - axisTextPadding, padding + chartHeight + textPaint.getTextSize() / 2, textPaint);


        // Calculate bar width and spacing
        float barWidth = (float) chartWidth / (orderedKeys.size() * 1.5f); // Adjust 1.5f for spacing
        float gap = barWidth / 2; // Gap between bars

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(24f); // Reset to default for bar labels

        for (int i = 0; i < orderedKeys.size(); i++) {
            String day = orderedKeys.get(i);
            long revenue = chartData.getOrDefault(day, 0L);

            // Calculate bar height based on revenue and maxRevenue
            float barHeight = (float) chartHeight * revenue / maxRevenue;

            // Calculate bar position
            float left = padding + i * (barWidth + gap) + gap / 2;
            float right = left + barWidth;
            float top = padding + chartHeight - barHeight;
            float bottom = padding + chartHeight;

            // Draw the bar
            canvas.drawRect(left, top, right, bottom, barPaint);

            // Draw day label below the bar
            canvas.drawText(day, left + barWidth / 2, bottom + axisTextPadding + textPaint.getTextSize(), textPaint);

            // Draw revenue value on top of the bar
            if (revenue > 0) { // Only draw if revenue is positive
                textPaint.setTextSize(20f); // Smaller text for value
                canvas.drawText(currencyFormat.format(revenue), left + barWidth / 2, top - 10, textPaint);
                textPaint.setTextSize(24f); // Reset for next bar
            }
        }
    }
}
