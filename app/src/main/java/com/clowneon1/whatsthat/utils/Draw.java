package com.clowneon1.whatsthat.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class Draw extends View {
    private Rect rect;
    private String text;

    private Paint boundaryPaint;
    private Paint textPaint;

    public Draw(Context context, Rect rect, String text) {
        super(context);
        this.rect = rect;
        this.text = text;

        init();
    }



    //drawing rectangle around the object.
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawText(text, rect.centerX(), rect.centerY(), textPaint);
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, boundaryPaint );
    }

    private void init(){
        //Initialize boundary paint
        boundaryPaint = new Paint();
        boundaryPaint.setColor(Color.YELLOW);
        boundaryPaint.setStrokeWidth(10f);
        boundaryPaint.setStyle(Paint.Style.STROKE);

        //Initialize text paint
        textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(50f);
        textPaint.setStyle(Paint.Style.FILL);

    }
}

