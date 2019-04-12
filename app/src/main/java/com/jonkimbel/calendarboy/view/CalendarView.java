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

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

public class CalendarView extends View {
    // Color.
    private Paint dividerColor;
    private Paint contentColor;
    private Paint backgroundColor;

    // Layout.
    private RectF containerRect;
    private float calendarCornerRadius;
    private float contentCornerRadius;

    // Input.
    private boolean hasBeenTouched = false;
    private float previousTouchX = -1;
    private float previousTouchY = -1;

    public CalendarView(Context context, @Nullable AttributeSet untypedAttrs) {
        super(context, untypedAttrs);
        TypedArray attrs = context.getTheme().obtainStyledAttributes(
                untypedAttrs,
                R.styleable.CalendarView,
                0, 0);

        Resources res = context.getResources();
        backgroundColor = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundColor.setStyle(Paint.Style.FILL);
        dividerColor = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerColor.setStyle(Paint.Style.STROKE);
        contentColor = new Paint(Paint.ANTI_ALIAS_FLAG);
        contentColor.setStyle(Paint.Style.FILL);

        try {
            backgroundColor.setColor(attrs.getColor(
                    R.styleable.CalendarView_backgroundColor,
                    res.getColor(R.color.CalendarView_defaultBackgroundColor)));
            dividerColor.setColor(attrs.getColor(
                    R.styleable.CalendarView_dividerColor,
                    res.getColor(R.color.CalendarView_defaultDividerColor)));
            contentColor.setColor(attrs.getColor(
                    R.styleable.CalendarView_contentColor,
                    res.getColor(R.color.CalendarView_defaultContentColor)));
            calendarCornerRadius = attrs.getDimension(
                    R.styleable.CalendarView_calendarCornerRadius,
                    res.getDimension(R.dimen.CalendarView_defaultCalendarCornerRadius));
            contentCornerRadius = attrs.getDimension(
                    R.styleable.CalendarView_contentCornerRadius,
                    res.getDimension(R.dimen.CalendarView_defaultContentCornerRadius));
        } finally {
            attrs.recycle();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float right = (float) (w - (getPaddingLeft() + getPaddingRight()));
        float bottom = (float) (h - (getPaddingTop() + getPaddingBottom()));

        containerRect = new RectF(
                getPaddingLeft(), getPaddingTop(),
                right, bottom);
    }

    // Consider implementing onMeasure:
    // https://developer.android.com/training/custom-views/custom-drawing#layouteevent

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRoundRect(
                containerRect, calendarCornerRadius, calendarCornerRadius, backgroundColor);
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

            Log.d("CalendarView", String.format(
                    "dX: %f, dY: %f", dX, dY));

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

    public void setDividerColor(@ColorInt int dividerColor) {
        this.dividerColor.setColor(dividerColor);
        invalidate();
        requestLayout();
    }

    public void setContentColor(@ColorInt int contentColor) {
        this.contentColor.setColor(contentColor);
        invalidate();
        requestLayout();
    }

    public void setCalendarCornerRadius(float calendarCornerRadius) {
        this.calendarCornerRadius = calendarCornerRadius;
        invalidate();
        requestLayout();
    }

    public void setContentCornerRadius(float contentCornerRadius) {
        this.contentCornerRadius = contentCornerRadius;
        invalidate();
        requestLayout();
    }
}
