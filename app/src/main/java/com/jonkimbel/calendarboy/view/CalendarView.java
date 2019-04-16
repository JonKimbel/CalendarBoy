package com.jonkimbel.calendarboy.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.jonkimbel.calendarboy.R;
import com.jonkimbel.calendarboy.model.Event;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

public class CalendarView extends View {
    // Attributes defined via XML.
    private final Paint backgroundColor;
    private final Paint backgroundStroke;
    private float backgroundRadiusPx;

    private final Paint dividerStroke;
    private float dividerStrokeWidthPx;
    private float dividerDashOnDistancePx;
    private float dividerDashOffDistancePx;
    private float dividerDashPhase;

    private final Paint contentColor;
    private final Paint contentStroke;
    private float contentRadiusPx;
    private float contentPaddingPx;

    private final Paint textColor;
    private float textSizePx;

    // Data.
    private List<Event> data;

    // Calculated layout.
    private RectF containerRect;
    private List<DrawableEvent> drawableEvents = new ArrayList<>();
    private List<Float> dividerLineYPositions = new ArrayList<>();

    // Input.
    private boolean hasBeenTouched = false;
    private float previousTouchX = -1;
    private float previousTouchY = -1;
    private long maxTimeToDisplay;
    private long minTimeToDisplay;

    public CalendarView(Context context, @Nullable AttributeSet untypedAttrs) {
        super(context, untypedAttrs);

        backgroundColor = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundColor.setStyle(Paint.Style.FILL);
        backgroundStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundStroke.setStyle(Paint.Style.STROKE);

        dividerStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerStroke.setStyle(Paint.Style.STROKE);

        contentColor = new Paint(Paint.ANTI_ALIAS_FLAG);
        contentColor.setStyle(Paint.Style.FILL);
        contentStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        contentStroke.setStyle(Paint.Style.STROKE);

        textColor = new Paint(Paint.ANTI_ALIAS_FLAG);
        textColor.setStyle(Paint.Style.FILL);

        TypedArray attrs = context.getTheme().obtainStyledAttributes(
                untypedAttrs,
                R.styleable.CalendarView,
                0, 0);
        Resources res = context.getResources();
        try {
            backgroundColor.setColor(attrs.getColor(
                    R.styleable.CalendarView_backgroundFill,
                    res.getColor(R.color.CalendarView_defaultBackgroundFill)));
            backgroundStroke.setColor(attrs.getColor(
                    R.styleable.CalendarView_backgroundStroke,
                    res.getColor(R.color.CalendarView_defaultBackgroundStroke)));
            backgroundRadiusPx = attrs.getDimension(
                    R.styleable.CalendarView_backgroundRadius,
                    res.getDimension(R.dimen.CalendarView_defaultBackgroundRadius));

            dividerStroke.setColor(attrs.getColor(
                    R.styleable.CalendarView_dividerStroke,
                    res.getColor(R.color.CalendarView_defaultDividerStroke)));
            dividerStrokeWidthPx = attrs.getDimension(
                    R.styleable.CalendarView_dividerStrokeWidth,
                    res.getDimension(R.dimen.CalendarView_defaultDividerStrokeWidth));
            dividerDashOnDistancePx = attrs.getDimension(
                    R.styleable.CalendarView_dividerDashOnDistance,
                    res.getDimension(R.dimen.CalendarView_defaultDividerDashOnDistance));
            dividerDashOffDistancePx = attrs.getDimension(
                    R.styleable.CalendarView_dividerDashOffDistance,
                    res.getDimension(R.dimen.CalendarView_defaultDividerDashOffDistance));
            dividerStroke.setStrokeWidth(dividerStrokeWidthPx);
            dividerStroke.setPathEffect(new DashPathEffect(
                    new float[]{dividerDashOnDistancePx, dividerDashOffDistancePx}, 0));

            contentColor.setColor(attrs.getColor(
                    R.styleable.CalendarView_contentFill,
                    res.getColor(R.color.CalendarView_defaultContentFill)));
            contentStroke.setColor(attrs.getColor(
                    R.styleable.CalendarView_contentStroke,
                    res.getColor(R.color.CalendarView_defaultContentStroke)));
            contentRadiusPx = attrs.getDimension(
                    R.styleable.CalendarView_contentRadius,
                    res.getDimension(R.dimen.CalendarView_defaultContentRadius));
            contentPaddingPx = attrs.getDimension(
                    R.styleable.CalendarView_contentPadding,
                    res.getDimension(R.dimen.CalendarView_defaultContentPadding));

            textColor.setColor(attrs.getColor(
                    R.styleable.CalendarView_textFill,
                    res.getColor(R.color.CalendarView_defaultTextFill)));
            textSizePx = attrs.getDimension(R.styleable.CalendarView_textSize,
                    res.getDimension(R.dimen.CalendarView_defaultTextSize));
            textColor.setTextSize(textSizePx);
        } finally {
            attrs.recycle();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        float right = (float) (w - (getPaddingLeft() + getPaddingRight()));
        float bottom = (float) (h - (getPaddingTop() + getPaddingBottom()));

        containerRect = new RectF(
                getPaddingLeft(), getPaddingTop(),
                right, bottom);

        dividerStroke.setPathEffect(new DashPathEffect(
                new float[]{dividerDashOnDistancePx, dividerDashOffDistancePx},
                calculateDashPhase()));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRoundRect(
                containerRect, backgroundRadiusPx, backgroundRadiusPx, backgroundColor);
        for (Float dividerLineYPosition : dividerLineYPositions) {
            canvas.drawLine(containerRect.left, dividerLineYPosition, containerRect.right,
                    dividerLineYPosition, dividerStroke);
        }
        canvas.drawRoundRect(containerRect, backgroundRadiusPx, backgroundRadiusPx,
                backgroundStroke);
        for (DrawableEvent drawableEvent : drawableEvents) {
            Log.d("CalendarView", String.format("drawing rect: %f, %f, %f, %f",
                    drawableEvent.rect.top,
                    drawableEvent.rect.left,
                    drawableEvent.rect.bottom,
                    drawableEvent.rect.right));
            canvas.drawRoundRect(drawableEvent.rect, contentRadiusPx, contentRadiusPx,
                    contentColor);
            canvas.drawRoundRect(drawableEvent.rect, contentRadiusPx, contentRadiusPx,
                    contentStroke);

            // TODO: fix this for RTL.
            for (TextBreakPoint breakPoint :
                    breakTextMultiline(drawableEvent.data.getTitle(), drawableEvent.rect,
                            textColor, contentPaddingPx)) {
                canvas.drawText(breakPoint.textLine, 0, breakPoint.textLine.length(),
                        breakPoint.xPosition, breakPoint.yPosition, textColor);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Consider using GestureDetectors for flinging, tapping, zooming support.
        // See https://developer.android.com/training/custom-views/making-interactive#motion

        // Consider using Animators for smooth animation.
        // https://developer.android.com/training/custom-views/making-interactive#makesmooth
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float dX = 0, dY = 0;
            if (hasBeenTouched) {
                dX = event.getX() - previousTouchX;
                dY = event.getY() - previousTouchY;
            }
            previousTouchX = event.getX();
            previousTouchY = event.getY();
            hasBeenTouched = true;

            // TODO: do something with touch events.

            invalidate();
            requestLayout();
        }

        return true;
    }

    public void setBackgroundColor(@ColorInt int backgroundColor) {
        this.backgroundColor.setColor(backgroundColor);
        invalidate();
        requestLayout();
    }

    public void setData(List<Event> data) {
        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
        ZonedDateTime todayStart = now.toLocalDate().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime todayEnd = todayStart.plusDays(1).minusNanos(1000);

        this.minTimeToDisplay = todayStart.toEpochSecond() * 1000;
        this.maxTimeToDisplay = todayEnd.toEpochSecond() * 1000;
        this.data = data;
        updateContentLayout();
        invalidate();
        requestLayout();
    }

    public void setDividerStroke(@ColorInt int dividerStroke) {
        this.dividerStroke.setColor(dividerStroke);
        invalidate();
        requestLayout();
    }

    public void setDividerStrokeWidthDp(float dividerStrokeWidthDp) {
        this.dividerStrokeWidthPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dividerStrokeWidthDp, getResources().getDisplayMetrics());
        dividerStroke.setStrokeWidth(dividerStrokeWidthPx);
        invalidate();
        requestLayout();
    }

    // Consider implementing onMeasure:
    // https://developer.android.com/training/custom-views/custom-drawing#layouteevent

    public void setDividerDashOnDistanceDp(float dividerDashOnDistanceDp) {
        this.dividerDashOnDistancePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dividerDashOnDistanceDp, getResources().getDisplayMetrics());
        dividerStroke.setPathEffect(new DashPathEffect(
                new float[]{dividerDashOnDistancePx, dividerDashOffDistancePx},
                calculateDashPhase()));
        invalidate();
        requestLayout();
    }

    public void setDividerDashOffDistanceDp(float dividerDashOffDistanceDp) {
        this.dividerDashOffDistancePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dividerDashOffDistanceDp, getResources().getDisplayMetrics());
        dividerStroke.setPathEffect(new DashPathEffect(
                new float[]{dividerDashOnDistancePx, dividerDashOffDistancePx},
                calculateDashPhase()));
        invalidate();
        requestLayout();
    }

    public void setContentColor(@ColorInt int contentColor) {
        this.contentColor.setColor(contentColor);
        invalidate();
        requestLayout();
    }

    public void setBackgroundRadiusDp(float backgroundRadiusDp) {
        this.backgroundRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                backgroundRadiusDp, getResources().getDisplayMetrics());
        invalidate();
        requestLayout();
    }

    public void setContentRadiusDp(float contentRadiusDp) {
        this.contentRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                contentRadiusDp, getResources().getDisplayMetrics());
        invalidate();
        requestLayout();
    }

    public void setContentPaddingDp(float contentPaddingDp) {
        this.contentPaddingPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                contentPaddingDp, getResources().getDisplayMetrics());
        invalidate();
        requestLayout();
    }

    public void setTextColor(@ColorInt int textColor) {
        this.textColor.setColor(textColor);
        invalidate();
        requestLayout();
    }

    public void setTextSizeSp(float textSizeSp) {
        this.textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                textSizeSp, getResources().getDisplayMetrics());
        textColor.setTextSize(textSizePx);
        invalidate();
        requestLayout();
    }

    private float calculateDashPhase() {
        // TODO: figure out what exactly dash phase means, it seems to not work as advertised.
        return containerRect.width() % (dividerDashOnDistancePx + dividerDashOffDistancePx) / 2;
    }

    private static List<TextBreakPoint> breakTextMultiline(
            String text, RectF container, Paint textPaint, float textPaddingPx) {
        com.google.common.base.Preconditions.checkArgument(
                textPaint.getTextAlign() == Paint.Align.LEFT);
        List<TextBreakPoint> breakPoints = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder(text);

        final float availableWidth = container.width() - 2 * textPaddingPx;
        float currentYPositionInRect = container.top + textPaddingPx + textPaint.getTextSize();
        while (textBuilder.length() > 0
                && currentYPositionInRect < container.bottom - textPaddingPx) {
            int textIndex = textPaint.breakText(textBuilder.toString(), 0, textBuilder.length(),
                    true, availableWidth, null);
            breakPoints.add(new TextBreakPoint(textBuilder.substring(0, textIndex),
                    container.left + textPaddingPx, currentYPositionInRect));
            textBuilder.delete(0, textIndex);
            currentYPositionInRect += textPaint.getTextSize();
        }

        return breakPoints;
    }

    private void updateContentLayout() {
        List<DrawableEvent> newDrawableEvents = new ArrayList<>();
        List<Float> newDividerLineYPositions = new ArrayList<>();
        if (data.size() > 0) {
            long minTimeMillis = Math.max(data.get(0).getStartTimeMillis(), minTimeToDisplay);
            long maxTimeMillis = Math.min(data.get(0).getEndTimeMillis(), maxTimeToDisplay);
            List<IntermediateRep> intermediateReps = new ArrayList<>();

            // Figure out start of first event and end of last event.
            // Also, transform the data into a different format (IntermediateRep) to make it easier
            // to figure out how many columns to render.
            {
                for (Event entry : data) {
                    long cappedStartTime = entry.getStartTimeMillis() < minTimeToDisplay ?
                            minTimeToDisplay : entry.getStartTimeMillis();
                    long cappedEndTime = entry.getEndTimeMillis() > maxTimeToDisplay ?
                            maxTimeToDisplay : entry.getEndTimeMillis();
                    if (cappedStartTime < minTimeMillis) {
                        minTimeMillis = cappedStartTime;
                    }
                    if (cappedEndTime > maxTimeMillis) {
                        maxTimeMillis = cappedEndTime;
                    }
                    intermediateReps.add(new IntermediateRep(true, cappedStartTime, entry));
                    intermediateReps.add(new IntermediateRep(false, cappedEndTime, entry));
                }
                Preconditions.checkState(maxTimeMillis > minTimeMillis);
            }

            List<FinalRep> finalRepresentations = new ArrayList<>();
            int columnsToRender = 0;

            // Figure out how many columns to render.
            {
                Collections.sort(intermediateReps, IntermediateRep::compare);
                int currentSimultaneousColumns = 0;
                Map<Event, FinalRep> currentEvents = new HashMap<>();
                for (IntermediateRep entry : intermediateReps) {
                    if (entry.increased) {
                        currentSimultaneousColumns++;

                        FinalRep finalRep = new FinalRep(
                                entry.data, currentSimultaneousColumns - 1);
                        finalRepresentations.add(finalRep);
                        currentEvents.put(entry.data, finalRep);
                        if (currentSimultaneousColumns > columnsToRender) {
                            columnsToRender = currentSimultaneousColumns;
                        }

                        for (FinalRep event : currentEvents.values()) {
                            if (event.numColumns < currentSimultaneousColumns) {
                                event.numColumns = currentSimultaneousColumns;
                            }
                        }
                    } else {
                        currentEvents.remove(entry.data);
                        currentSimultaneousColumns--;
                    }
                }
                Preconditions.checkState(currentSimultaneousColumns == 0);
                Preconditions.checkState(columnsToRender > 0);
                Preconditions.checkState(currentEvents.size() == 0);
            }

            float pxHeightPerMillisecond =
                    (containerRect.height() - 2 * contentPaddingPx)
                            / (maxTimeMillis - minTimeMillis);
            float[] pxWidthPerNumOtherColumns = new float[columnsToRender];
            for (int i = 0; i < columnsToRender; i++) {
                pxWidthPerNumOtherColumns[i] =
                        (containerRect.width() - 2 * contentPaddingPx - i * contentPaddingPx)
                                / (i + 1);
            }

            for (FinalRep finalRep : finalRepresentations) {
                float left = containerRect.left + contentPaddingPx +
                        (pxWidthPerNumOtherColumns[finalRep.numColumns - 1] + contentPaddingPx) *
                                finalRep.columnPosition;
                float top = containerRect.top + contentPaddingPx +
                        pxHeightPerMillisecond * (finalRep.data.getStartTimeMillis() - minTimeMillis);
                float right = left + pxWidthPerNumOtherColumns[finalRep.numColumns - 1];
                float bottom = top +
                        pxHeightPerMillisecond *
                                (finalRep.data.getEndTimeMillis() - finalRep.data.getStartTimeMillis());
                newDrawableEvents.add(new DrawableEvent(new RectF(left, top, right, bottom),
                        finalRep.data));
            }

            ZonedDateTime firstDividerTime =
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(minTimeMillis),
                            ZoneId.systemDefault()).truncatedTo(ChronoUnit.HOURS).plusHours(1);
            ZonedDateTime lastDividerTime =
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(maxTimeMillis),
                            ZoneId.systemDefault()).truncatedTo(ChronoUnit.HOURS);

            for (long seconds = firstDividerTime.toEpochSecond();
                 seconds < lastDividerTime.toEpochSecond();
                 seconds += TimeUnit.HOURS.toSeconds(1)) {
                float yPosition = containerRect.top + contentPaddingPx +
                        pxHeightPerMillisecond * (TimeUnit.SECONDS.toMillis(seconds) - minTimeMillis);
                newDividerLineYPositions.add(yPosition);
            }
        }
        Log.d("CalendarView", Integer.toString(newDrawableEvents.size()));
        this.drawableEvents = newDrawableEvents;
        this.dividerLineYPositions = newDividerLineYPositions;
    }

    private static class TextBreakPoint {
        final String textLine;
        final float xPosition;
        final float yPosition;

        TextBreakPoint(String textLine, float xPosition, float yPosition) {
            this.textLine = textLine;
            this.xPosition = xPosition;
            this.yPosition = yPosition;
        }
    }

    private static class IntermediateRep {
        public final boolean increased;
        public final long time;
        public final Event data;

        IntermediateRep(boolean increased, long time, Event data) {
            this.increased = increased;
            this.time = time;
            this.data = data;
        }

        public static int compare(IntermediateRep lh, IntermediateRep rh) {
            if (lh.time < rh.time) {
                return -1;
            }
            if (lh.time > rh.time) {
                return 1;
            }
            // Time is equal.
            if (lh.increased == rh.increased) {
                return 0;
            }
            // Time is equal and one increases, the other decreases. Put the decrease first.
            if (lh.increased) {
                return 1;
            }
            return -1;
        }
    }

    private static class FinalRep {
        public int numColumns = 1;
        public final int columnPosition;
        public final Event data;

        FinalRep(Event data, int columnPosition) {
            this.data = data;
            this.columnPosition = columnPosition;
        }
    }

    private static class DrawableEvent {
        final RectF rect;
        final Event data;

        DrawableEvent(RectF rect, Event event) {
            this.rect = rect;
            this.data = event;
        }
    }
}
