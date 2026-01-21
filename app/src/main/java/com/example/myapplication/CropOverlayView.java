package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
public class CropOverlayView extends View {

    private RectF cropRect;
    private final Paint borderPaint;
    private final Paint dimPaint;
    private final Paint gridPaint;
    private final Paint handlePaint;

    private final float handleRadius;
    private final float minSize;
    private boolean lockAspect = false; // 1:1 toggle

    private float lastX, lastY;
    private long lastTapTime = 0;

    private Mode mode = Mode.NONE;
    private enum Mode { NONE, MOVE, TL, TR, BL, BR }

    public CropOverlayView(Context c, AttributeSet a) {
        super(c, a);

        minSize = dp(120);
        handleRadius = dp(12);

        borderPaint = paint(Color.WHITE, 2, Paint.Style.STROKE);
        gridPaint = paint(0x80FFFFFF, 1, Paint.Style.STROKE);
        handlePaint = paint(Color.WHITE, 0, Paint.Style.FILL);

        dimPaint = new Paint();
        dimPaint.setColor(0x88000000);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        float size = Math.min(w, h) * 0.65f;
        cropRect = new RectF(
                (w - size) / 2,
                (h - size) / 2,
                (w + size) / 2,
                (h + size) / 2
        );
    }

    @Override
    protected void onDraw(Canvas c) {
        // dim
        c.drawRect(0, 0, getWidth(), cropRect.top, dimPaint);
        c.drawRect(0, cropRect.bottom, getWidth(), getHeight(), dimPaint);
        c.drawRect(0, cropRect.top, cropRect.left, cropRect.bottom, dimPaint);
        c.drawRect(cropRect.right, cropRect.top, getWidth(), cropRect.bottom, dimPaint);

        // grid (rule of thirds)
        float w = cropRect.width() / 3;
        float h = cropRect.height() / 3;

        c.drawLine(cropRect.left + w, cropRect.top, cropRect.left + w, cropRect.bottom, gridPaint);
        c.drawLine(cropRect.left + 2 * w, cropRect.top, cropRect.left + 2 * w, cropRect.bottom, gridPaint);
        c.drawLine(cropRect.left, cropRect.top + h, cropRect.right, cropRect.top + h, gridPaint);
        c.drawLine(cropRect.left, cropRect.top + 2 * h, cropRect.right, cropRect.top + 2 * h, gridPaint);

        // border
        c.drawRect(cropRect, borderPaint);

        // handles
        c.drawCircle(cropRect.left, cropRect.top, handleRadius, handlePaint);
        c.drawCircle(cropRect.right, cropRect.top, handleRadius, handlePaint);
        c.drawCircle(cropRect.left, cropRect.bottom, handleRadius, handlePaint);
        c.drawCircle(cropRect.right, cropRect.bottom, handleRadius, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (System.currentTimeMillis() - lastTapTime < 300) reset();
            lastTapTime = System.currentTimeMillis();

            mode = detect(x, y);
            lastX = x; lastY = y;
            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = x - lastX, dy = y - lastY;
            if (mode == Mode.MOVE) cropRect.offset(dx, dy);
            else resize(dx, dy);

            constrain();
            lastX = x; lastY = y;
            invalidate();
        }

        if (e.getAction() == MotionEvent.ACTION_UP) mode = Mode.NONE;
        return true;
    }

    private void resize(float dx, float dy) {
        if (lockAspect) dy = dx;

        switch (mode) {
            case TL: cropRect.left += dx; cropRect.top += dy; break;
            case TR: cropRect.right += dx; cropRect.top += dy; break;
            case BL: cropRect.left += dx; cropRect.bottom += dy; break;
            case BR: cropRect.right += dx; cropRect.bottom += dy; break;
        }

        if (cropRect.width() < minSize)
            cropRect.right = cropRect.left + minSize;
        if (cropRect.height() < minSize)
            cropRect.bottom = cropRect.top + minSize;
    }

    private Mode detect(float x, float y) {
        if (near(x, y, cropRect.left, cropRect.top)) return Mode.TL;
        if (near(x, y, cropRect.right, cropRect.top)) return Mode.TR;
        if (near(x, y, cropRect.left, cropRect.bottom)) return Mode.BL;
        if (near(x, y, cropRect.right, cropRect.bottom)) return Mode.BR;
        if (cropRect.contains(x, y)) return Mode.MOVE;
        return Mode.NONE;
    }

    private void reset() { onSizeChanged(getWidth(), getHeight(), 0, 0); invalidate(); }
    private void constrain() {
        if (cropRect.left < 0) cropRect.offset(-cropRect.left, 0);
        if (cropRect.top < 0) cropRect.offset(0, -cropRect.top);
        if (cropRect.right > getWidth()) cropRect.offset(getWidth() - cropRect.right, 0);
        if (cropRect.bottom > getHeight()) cropRect.offset(0, getHeight() - cropRect.bottom);
    }

    private boolean near(float x1, float y1, float x2, float y2) {
        return Math.hypot(x1 - x2, y1 - y2) < handleRadius * 2;
    }

    private Paint paint(int c, int w, Paint.Style s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(c); p.setStrokeWidth(dp(w)); p.setStyle(s);
        return p;
    }

    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    public void setAspectLocked(boolean lock) { lockAspect = lock; }
    public RectF getCropRect() { return new RectF(cropRect); }
}
