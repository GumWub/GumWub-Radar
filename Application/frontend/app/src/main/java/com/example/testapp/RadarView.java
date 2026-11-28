package com.example.testapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

public class RadarView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float targetDistance;
    private float targetAngle;
    private boolean sweepEnabled;

    public RadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setReading(float distanceCm, float angleDegrees, boolean scanning) {
        targetDistance = distanceCm;
        targetAngle = angleDegrees;
        sweepEnabled = scanning;
        invalidate();
    }

    public void setSweepEnabled(boolean enabled) {
        sweepEnabled = enabled;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() * 0.82f;
        float radius = Math.min(getWidth() * 0.44f, getHeight() * 0.68f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(Color.rgb(42, 68, 86));

        for (int i = 1; i <= 4; i++) {
            float ringRadius = radius * i / 4f;
            canvas.drawArc(cx - ringRadius, cy - ringRadius,
                    cx + ringRadius, cy + ringRadius,
                    180f, 180f, false, paint);
        }

        for (int i = 0; i <= 6; i++) {
            float angle = (float) Math.toRadians(180f - i * 30f);
            float ex = cx + (float) Math.cos(angle) * radius;
            float ey = cy - (float) Math.sin(angle) * radius;
            canvas.drawLine(cx, cy, ex, ey, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(9));
        paint.setColor(Color.rgb(105, 137, 156));
        canvas.drawText("180", cx - radius - dp(6), cy + dp(14), paint);
        canvas.drawText("90", cx, cy - radius - dp(8), paint);
        canvas.drawText("0", cx + radius + dp(6), cy + dp(14), paint);

        if (sweepEnabled) {
            float sweepRadians = (float) Math.toRadians(180f - targetAngle);
            paint.setColor(Color.rgb(54, 214, 229));
            paint.setStrokeWidth(dp(2));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(cx, cy,
                    cx + (float) Math.cos(sweepRadians) * radius,
                    cy - (float) Math.sin(sweepRadians) * radius,
                    paint);
        }

        float normalizedDistance = Math.max(0f, Math.min(1f, targetDistance / 120f));
        float pointRadius = radius * normalizedDistance;
        float pointRadians = (float) Math.toRadians(180f - targetAngle);
        float px = cx + (float) Math.cos(pointRadians) * pointRadius;
        float py = cy - (float) Math.sin(pointRadians) * pointRadius;

        paint.setColor(Color.rgb(79, 235, 173));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(px, py, dp(5), paint);

        paint.setColor(Color.rgb(54, 214, 229));
        canvas.drawCircle(cx, cy, dp(4), paint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
