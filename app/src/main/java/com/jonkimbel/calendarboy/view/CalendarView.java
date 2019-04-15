package com.jonkimbel.calendarboy.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.jonkimbel.calendarboy.R;
import com.jonkimbel.calendarboy.model.Event;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

public class CalendarView extends View {
    // Attributes defined via XML.
    private final Paint backgroundColor;
    private final Paint backgroundStroke;
    private float backgroundRadius;

    private final Paint dividerStroke;

    private final Paint contentColor;
    private final Paint contentStroke;
    private float contentRadius;
    private float contentPadding;

    // Data.
    private List<Event> data;

    // Calculated layout.
    private RectF containerRect;
    private List<RectF> contentRects = new ArrayList<>();

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
            backgroundRadius = attrs.getDimension(
                    R.styleable.CalendarView_backgroundRadius,
                    res.getDimension(R.dimen.CalendarView_defaultBackgroundRadius));

            dividerStroke.setColor(attrs.getColor(
                    R.styleable.CalendarView_dividerStroke,
                    res.getColor(R.color.CalendarView_defaultDividerStroke)));

            contentColor.setColor(attrs.getColor(
                    R.styleable.CalendarView_contentFill,
                    res.getColor(R.color.CalendarView_defaultContentFill)));
            contentStroke.setColor(attrs.getColor(
                    R.styleable.CalendarView_contentStroke,
                    res.getColor(R.color.CalendarView_defaultContentStroke)));
            contentRadius = attrs.getDimension(
                    R.styleable.CalendarView_contentRadius,
                    res.getDimension(R.dimen.CalendarView_defaultContentRadius));
            contentPadding = attrs.getDimension(
                    R.styleable.CalendarView_contentPadding,
                    res.getDimension(R.dimen.CalendarView_defaultContentPadding));
        } finally {
            attrs.recycle();
        }

        contentRects.add(new RectF());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        float right = (float) (w - (getPaddingLeft() + getPaddingRight()));
        float bottom = (float) (h - (getPaddingTop() + getPaddingBottom()));

        containerRect = new RectF(
                getPaddingLeft(), getPaddingTop(),
                right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRoundRect(
                containerRect, backgroundRadius, backgroundRadius, backgroundColor);
        canvas.drawRoundRect(containerRect, backgroundRadius, backgroundRadius, backgroundStroke);
        for (RectF contentRect : contentRects) {
            Log.d("CalendarView", String.format("drawing rect: %f, %f, %f, %f",
                    contentRect.top,
                    contentRect.left,
                    contentRect.bottom,
                    contentRect.right));
            canvas.drawRoundRect(contentRect, contentRadius, contentRadius, contentColor);
            canvas.drawRoundRect(contentRect, contentRadius, contentRadius, contentStroke);
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

    // Consider implementing onMeasure:
    // https://developer.android.com/training/custom-views/custom-drawing#layouteevent

    public void setDividerStroke(@ColorInt int dividerStroke) {
        this.dividerStroke.setColor(dividerStroke);
        invalidate();
        requestLayout();
    }

    public void setContentColor(@ColorInt int contentColor) {
        this.contentColor.setColor(contentColor);
        invalidate();
        requestLayout();
    }

    public void setBackgroundRadius(float backgroundRadius) {
        this.backgroundRadius = backgroundRadius;
        invalidate();
        requestLayout();
    }

    public void setContentRadius(float contentRadius) {
        this.contentRadius = contentRadius;
        invalidate();
        requestLayout();
    }

    public void setContentPadding(float contentPadding) {
        this.contentPadding = contentPadding;
        invalidate();
        requestLayout();
    }

    private void updateContentLayout() {
        List<RectF> newContentRects = new ArrayList<>();
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
                    (containerRect.height() - 2 * contentPadding)
                            / (maxTimeMillis - minTimeMillis);
            float[] pxWidthPerNumOtherColumns = new float[columnsToRender];
            for (int i = 0; i < columnsToRender; i++) {
                pxWidthPerNumOtherColumns[i] =
                        (containerRect.width() - 2 * contentPadding - i * contentPadding)
                                / (i + 1);
            }

            for (FinalRep finalRep : finalRepresentations) {
                float left = containerRect.left + contentPadding +
                        (pxWidthPerNumOtherColumns[finalRep.numColumns - 1] + contentPadding) *
                                finalRep.columnPosition;
                float top = containerRect.top + contentPadding +
                        pxHeightPerMillisecond * (finalRep.data.getStartTimeMillis() - minTimeMillis);
                float right = left + pxWidthPerNumOtherColumns[finalRep.numColumns - 1];
                float bottom = top +
                        pxHeightPerMillisecond *
                                (finalRep.data.getEndTimeMillis() - finalRep.data.getStartTimeMillis());
                newContentRects.add(new RectF(left, top, right, bottom));
            }
        }
        Log.d("CalendarView", Integer.toString(newContentRects.size()));
        this.contentRects = newContentRects;
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
}
