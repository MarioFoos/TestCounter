package com.mlf.testcounter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

import java.util.Locale;

// TextView autoajustable para mostrar número desde -99 hasta 999
public class AutoSizeCounter extends AppCompatTextView
{
    public static final String LOG_TAG = "AppLog";
    private static final String DEC_TEXT = "<";
    private static final String INC_TEXT = ">";
    private static final int ACTION_NONE = 0;
    private static final int ACTION_DEC = 1;
    private static final int ACTION_INC = 2;

    private static final int GRAY_25 = Color.rgb(64, 64, 64);
    private static final int GRAY_75 = Color.rgb(192, 192, 192);
    private static final int MIN_HEIGHT = 60;
    private static final int MIN_WIDTH = 120;
    private static final int VALUE_DEFAULT = 40;
    private static final long TIME_STEP_MAX = 200;
    private static final long TIME_STEP_MIN = 80;
    private static final long TIME_STEP_DELTA = 20;
    private static final long TIME_DELTA = 3000;

    private final Paint paintFg;    // Relleno
    private final Paint paintSt;    // Borde
    private final Paint paintDFg;    // Relleno
    private final Paint paintDSt;    // Borde
    private final Paint paintAux;

    private float textSize = 20f;   // Tamaño de texto mínimo
    private float butSize = 20f;    // Tamaño de botones
    private float deltaSize = 10f;    // Tamaño de botones
    private int value = VALUE_DEFAULT;
    private int delta = 0;
    private boolean deltaShowed = false;

    private final Runnable runDec;
    private final Runnable runInc;
    private final Runnable runHideDelta;
    private Thread threadDec, threadInc, threadDelta;
    private int action = ACTION_NONE;

    private Rect rcDec, rcInc, rcNumber, rcDelta;

    public AutoSizeCounter(Context context)
    {
        this(context, null);
    }

    public AutoSizeCounter(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public AutoSizeCounter(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        Typeface typeBold = ResourcesCompat.getFont(context, R.font.consolebold);
        Typeface typeNormal = ResourcesCompat.getFont(context, R.font.console);

        paintFg = new Paint();
        paintFg.setTypeface(typeBold);
        paintFg.setStyle(Paint.Style.FILL);
        paintFg.setTextAlign(Paint.Align.LEFT);

        paintSt = new Paint();
        paintSt.setTypeface(typeBold);
        paintSt.setStyle(Paint.Style.STROKE);
        paintSt.setTextAlign(Paint.Align.LEFT);
        paintSt.setStrokeWidth(2);

        paintDFg = new Paint();
        paintDFg.setTypeface(typeNormal);
        paintDFg.setStyle(Paint.Style.FILL);
        paintDFg.setTextAlign(Paint.Align.LEFT);

        paintDSt = new Paint();
        paintDSt.setTypeface(typeNormal);
        paintDSt.setStyle(Paint.Style.STROKE);
        paintDSt.setTextAlign(Paint.Align.LEFT);
        paintDSt.setStrokeWidth(1);

        paintAux = new Paint();
        paintAux.setTypeface(typeBold);
        paintAux.setStyle(Paint.Style.STROKE);
        paintAux.setTextAlign(Paint.Align.LEFT);
        paintAux.setColor(Color.RED);
        paintAux.setStrokeWidth(1);

        setValue(40);
        calcColors();
        setLineSpacing(0.0f, 1.0f);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        runDec = new Runnable()
        {
            @Override
            public void run()
            {
                long step = TIME_STEP_MAX;
                long next = System.currentTimeMillis() + step;
                deltaShowed = true;
                decrement();
                while(action == ACTION_DEC)
                {
                    if(System.currentTimeMillis() >= next)
                    {
                        decrement();
                        if(step > TIME_STEP_MIN)
                        {
                            step -= TIME_STEP_DELTA;
                        }
                        next = System.currentTimeMillis() + step;
                    }
                }
            }
        };

        runInc = new Runnable()
        {
            @Override
            public void run()
            {
                long step = TIME_STEP_MAX;
                long next = System.currentTimeMillis() + step;
                deltaShowed = true;
                increment();
                while(action == ACTION_INC)
                {
                    if(System.currentTimeMillis() >= next)
                    {
                        increment();
                        if(step > TIME_STEP_MIN)
                        {
                            step -= TIME_STEP_DELTA;
                        }
                        next = System.currentTimeMillis() + step;
                    }
                }
            }
        };

        runHideDelta = new Runnable()
        {
            @Override
            public void run()
            {
                long next = System.currentTimeMillis() + TIME_DELTA;
                while(action == ACTION_NONE)
                {
                    if(System.currentTimeMillis() >= next)
                    {
                        delta = 0;
                        deltaShowed = false;
                        invalidate();
                    }
                }
            }
        };

        setOnTouchListener(new OnTouchListener()
        {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                int x = (int) event.getX();
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        if(action == ACTION_NONE)
                        {
                            if(x < rcNumber.exactCenterX())
                            {
                                action = ACTION_DEC;
                                threadDec = new Thread(runDec);
                                threadDec.start();
                            }
                            else
                            {
                                action = ACTION_INC;
                                threadInc = new Thread(runInc);
                                threadInc.start();
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(action != ACTION_NONE)
                        {
                            Log.e(LOG_TAG, "runHideDelta");
                            action = ACTION_NONE;
                            threadDelta = new Thread(runHideDelta);
                            threadDelta.start();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter)
    {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if(width < MIN_WIDTH)
        {
            width = MIN_WIDTH;
        }
        if(height < MIN_HEIGHT)
        {
            height = MIN_HEIGHT;
        }
        calcAreas(width, height);
        calcTextSize();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void calcAreas(int width, int height)
    {
        int padding = (int)Math.round(width*0.025f);
        int space =  (int)Math.round(width*0.15f);
        int wbut =(int)Math.round(width*0.15f - padding);
        int hbut = height - 2*padding;
        int hdelta = hbut/4;
        rcDec = new Rect(padding, padding, padding + wbut, padding + hbut);
        rcInc = new Rect(width - padding - wbut, rcDec.top, width - padding, rcDec.bottom);
        rcNumber = new Rect(rcDec.right + space, rcDec.top, rcInc.left - space, rcDec.bottom);
        //rcDelta = new Rect(rcNumber.right + padding/2, rcDec.top, rcInc.left - padding/2, rcDec.top + hdelta);
        rcDelta = new Rect(rcNumber.right, rcDec.top, rcInc.left, rcDec.top + hdelta);
    }

    public void calcTextSize()
    {
        String text = "44";
        textSize = 20f;
        int width = rcNumber.width(), height = rcNumber.height();
        Rect bounds = getTextRect(text, textSize);
        while(bounds.height() < height && bounds.width() < width)
        {
            ++textSize;
            bounds = getTextRect(text, textSize);
        }
        --textSize;

        text = "+";
        butSize = 20f;
        width = rcDec.width();
        height = rcDec.height();
        bounds = getTextRect(text, butSize);
        while(bounds.height() < height && bounds.width() < width)
        {
            ++butSize;
            bounds = getTextRect(text, butSize);
        }
        --butSize;

        text = "+44";
        deltaSize = 10f;
        width = rcDelta.width();
        height = rcDelta.height();
        bounds = getTextRect(text, deltaSize);
        while(bounds.height() < height && bounds.width() < width)
        {
            ++deltaSize;
            bounds = getTextRect(text, deltaSize);
        }
        --deltaSize;
    }

    private Rect getTextRect(String text, float textSize)
    {
        TextPaint paintCopy = new TextPaint(paintFg);
        Rect bounds = new Rect();
        paintCopy.setTextSize(textSize);
        paintCopy.getTextBounds(text, 0, text.length(), bounds);
        return bounds;
    }

    private void drawNumber(Canvas canvas)
    {
        drawText(canvas, getText().toString(), rcNumber, textSize);
    }

    private void drawButtons(Canvas canvas)
    {
        drawText(canvas, INC_TEXT, rcInc, butSize);
        drawText(canvas, DEC_TEXT, rcDec, butSize);
    }

    private void drawDelta(Canvas canvas)
    {
        if(deltaShowed)
        {
            drawText(canvas, getDelta(), rcDelta, deltaSize);
        }
    }

    private void drawText(Canvas canvas, String text, Rect rc, float textSize)
    {
        Rect bounds = getTextRect(text, textSize);
        float x = rc.exactCenterX() - bounds.exactCenterX();
        float y = rc.exactCenterY() - bounds.exactCenterY();

        paintFg.setTextSize(textSize);
        paintSt.setTextSize(textSize);

        canvas.drawText(text, x, y, paintFg);
        canvas.drawText(text, x, y, paintSt);

        //canvas.drawRect(rc, paintAux);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        canvas.drawColor(getBackgroundColor());
        drawNumber(canvas);
        drawButtons(canvas);
        drawDelta(canvas);
    }

    private int getGray(int color)
    {
        return (int) Math.round(0.299f*Color.red(color) + 0.587f*Color.green(color) + 0.114f*Color.blue(color));
    }

    private void calcColors()
    {
        int clText;     // Color del texto
        int clStroke;   // Color de línea
        if(getGray(getBackgroundColor()) < 128)
        {
            clText = Color.WHITE;
            clStroke = GRAY_75;
        }
        else
        {
            clText = Color.BLACK;
            clStroke = GRAY_25;
        }
        paintFg.setColor(clText);
        paintSt.setColor(clStroke);
        paintDFg.setColor(clText);
        paintDSt.setColor(clStroke);
    }

    @Override
    public void setText(CharSequence text, BufferType type)
    {
        int newValue;
        if((text == null) || (text.length() == 0))
        {
            newValue = 0;
        }
        else
        {
            try
            {
                newValue = Integer.parseInt(text.toString());
            }
            catch(Exception e)
            {
                newValue = 0;
                e.printStackTrace();
            }
        }
        setValue(newValue);
    }

    @Override
    public CharSequence getText()
    {
        return String.format(Locale.US, "%d", value);
    }

    public String getDelta()
    {
        if(delta == 0)
        {
            return "=";
        }
        return String.format(Locale.US, (delta > 0) ? "+%d" : "%d", delta);
    }

    private boolean setValue(int newValue)
    {
        if(newValue < 100 && newValue > -100 && newValue != value)
        {
            this.value = newValue;
            invalidate();
            return true;
        }
        return false;
    }

    public int getValue()
    {
        return value;
    }

    public void increment()
    {
        if(setValue(value + 1))
        {
            ++delta;
        }
    }

    public void decrement()
    {
        if(setValue(value - 1))
        {
            --delta;
        }
    }

    @Override
    public void setBackgroundColor(int color)
    {
        super.setBackgroundColor(color);
        calcColors();
    }

    public int getBackgroundColor()
    {
        ColorDrawable cl = (ColorDrawable) getBackground();
        return cl.getColor();
    }
}