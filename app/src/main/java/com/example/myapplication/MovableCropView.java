package com.example.myapplication;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class MovableCropView extends View {

    private float dX, dY;

    public MovableCropView(Context context) { super(context); }
    public MovableCropView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); }
    public MovableCropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                dX = getX() - event.getRawX();
                dY = getY() - event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                setX(event.getRawX() + dX);
                setY(event.getRawY() + dY);
                break;
        }
        return true;
    }
}
