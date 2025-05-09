package edu.cuhk.csci3310.buddyconnect.Record_scheduler;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.util.Set;
import android.graphics.Typeface;

public class EventDecorator implements DayViewDecorator {
    private final int color;
    private final Set<CalendarDay> dates;
    private float position; // Position between 0.0f and 1.0f
    private final boolean isOwner;

    public EventDecorator(int color, Set<CalendarDay> dates, float position, boolean isOwner) {
        this.color = color;
        this.dates = dates;
        this.position = position;
        this.isOwner = isOwner;
    }

    public void setPosition(float position) {
        this.position = position;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new PositionedDotSpan(10, color, position, isOwner)); // Fixed radius to 10
    }

    // Inner class for drawing the dot using LineBackgroundSpan
    private static class PositionedDotSpan implements LineBackgroundSpan {
        private final int color;
        private final float radius;
        private final float position;
        private final boolean isOwner;

        public PositionedDotSpan(float radius, int color, float position, boolean isOwner) {
            this.radius = radius;
            this.color = color;
            this.position = position;
            this.isOwner = isOwner;
        }

        @Override
        public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
            paint.setColor(color);
            float centerX = left + (right - left) * position;
            float centerY = bottom + radius;
            if (isOwner) {
                canvas.drawCircle(centerX, centerY, radius, paint);
            } else {
                paint.setTextSize(radius * 3);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                canvas.drawText("*", centerX - radius, centerY + radius, paint);
            }
        }
    }
}