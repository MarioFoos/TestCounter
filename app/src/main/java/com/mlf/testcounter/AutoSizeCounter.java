package com.mlf.testcounter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.Log;
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

    private final Paint paintTextFill, paintTextSt;     // Relleno y borde de texto
    private final Paint paintBackSt;                    // Fondo
    private final Paint paintDeltaFill, paintDeltaSt;   // Relleno y borde del delta

    private Rect rcDec, rcInc, rcNumber, rcDelta;

    private float textSize = 20f;       // Tamaño de texto mínimo
    private float butSize = 20f;        // Tamaño de botones
    private float deltaSize = 10f;      // Tamaño de botones
    private int value = VALUE_DEFAULT;
    private int delta = 0;
    private boolean deltaShowed = false;

    private final Runnable runDec, runInc, runHideDelta;
    private Thread threadDec, threadInc, threadDelta;
    private int action = ACTION_NONE;

    // Fondo
    private Rect rcCanvas;
    private Bitmap bmpBg;

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

        // Fondo
        paintBackSt = new Paint();
        paintBackSt.setStyle(Paint.Style.STROKE);

        // Texto
        paintTextFill = new Paint();
        paintTextFill.setTypeface(typeBold);
        paintTextFill.setStyle(Paint.Style.FILL);
        paintTextFill.setTextAlign(Paint.Align.LEFT);

        paintTextSt = new Paint();
        paintTextSt.setTypeface(typeBold);
        paintTextSt.setStyle(Paint.Style.STROKE);
        paintTextSt.setTextAlign(Paint.Align.LEFT);
        paintTextSt.setStrokeWidth(2);

        // Delta
        paintDeltaFill = new Paint();
        paintDeltaFill.setTypeface(typeNormal);
        paintDeltaFill.setStyle(Paint.Style.FILL);
        paintDeltaFill.setTextAlign(Paint.Align.LEFT);

        paintDeltaSt = new Paint();
        paintDeltaSt.setTypeface(typeNormal);
        paintDeltaSt.setStyle(Paint.Style.STROKE);
        paintDeltaSt.setTextAlign(Paint.Align.LEFT);
        paintDeltaSt.setStrokeWidth(1);

        setColors();

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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int width = Math.max(MeasureSpec.getSize(widthMeasureSpec), MIN_WIDTH);
        int height = Math.max(MeasureSpec.getSize(heightMeasureSpec), MIN_HEIGHT);

        calcAreas(width, height);
        calcTextSize();
        buildBackground(width, height);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Calcular todos los rectándulos que necesito
     * @param width Ancho del canvas
     * @param height Alto    del canvas
     */
    public void calcAreas(int width, int height)
    {
        int padding = Math.round(width*0.025f);
        int space =  Math.round(width*0.15f);
        int wbut = Math.round(width*0.15f - padding);
        int hbut = height - 2*padding;
        int hdelta = hbut/4;

        rcCanvas = new Rect(0, 0, width, height);
        rcDec = new Rect(padding, padding, padding + wbut, padding + hbut);
        rcInc = new Rect(width - padding - wbut, rcDec.top, width - padding, rcDec.bottom);
        rcNumber = new Rect(rcDec.right + space, rcDec.top, rcInc.left - space, rcDec.bottom);
        rcDelta = new Rect(rcNumber.right, rcDec.top, rcInc.left, rcDec.top + hdelta);
    }

    /**
     * Encontrar el tamaño del texto que se ajueste al área
     */
    public float calcFontSize(String text, Rect rc, Paint paint)
    {
        Paint paintCalc = new Paint(paint);
        Rect bounds = new Rect();
        float size = 20f;
        int width = rc.width(), height = rc.height();
        int len = text.length();

        paintCalc.setTextSize(size);
        paintCalc.getTextBounds(text, 0, len, bounds);
        while(bounds.height() < height && bounds.width() < width)
        {
            ++size;
            paintCalc.setTextSize(size);
            paintCalc.getTextBounds(text, 0, len, bounds);
        }
        return size - 1;
    }

    /**
     * Encontrar el tamaño del texto que se ajueste al área
     */
    public void calcTextSize()
    {
        if(rcNumber == null || rcDec == null || rcDelta == null)
        {
            calcAreas(getWidth(), getHeight());
        }
        textSize = calcFontSize((getText().length() == 3) ? "-44" : "44", rcNumber, paintTextFill);
        butSize = calcFontSize("+", rcDec, paintTextFill);
        deltaSize = calcFontSize("+44", rcDelta, paintTextFill);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        // Fondo con los botones
        canvas.drawBitmap(bmpBg, rcCanvas, rcCanvas, paintBackSt);
        // Número
        drawText(canvas, getText().toString(), rcNumber, textSize);
        // Delta
        if(deltaShowed)
        {
            drawText(canvas, getDelta(), rcDelta, deltaSize);
        }
    }

    /**
     * Encontrar nivel de gris
     * @param color Color
     * @return Nivel de gris
     */
    private int getGray(int color)
    {
        return Math.round(0.299f*Color.red(color) + 0.587f*Color.green(color) + 0.114f*Color.blue(color));
    }

    /**
     * Establecer el color del texto
     */
    private void setColors()
    {
        int clText, clStroke;
        int clBack = getBackgroundColor();
        if(getGray(clBack) < 128)
        {
            clText = Color.WHITE;
            clStroke = GRAY_75;
        }
        else
        {
            clText = Color.BLACK;
            clStroke = GRAY_25;
        }
        // Fondo
        paintBackSt.setColor(clText);
        // Número y botones
        paintTextFill.setColor(clText);
        paintTextSt.setColor(clStroke);
        // Delta
        paintDeltaFill.setColor(clText);
        paintDeltaSt.setColor(clStroke);
    }

    private void buildBackground(int width, int height)
    {
        bmpBg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpBg);

        // Background
        canvas.drawColor(getBackgroundColor());
        // Buttons
        drawText(canvas, INC_TEXT, rcInc, butSize);
        drawText(canvas, DEC_TEXT, rcDec, butSize);
    }

    /**
     * Dibujar el texto en el rectángulo
     * @param canvas Canvas
     * @param text Texto
     * @param rc Rectángulo
     * @param textSize Tamaño de fuente
     */
    private void drawText(Canvas canvas, String text, Rect rc, float textSize)
    {
        Rect bounds = new Rect();
        paintTextFill.setTextSize(textSize);
        paintTextFill.getTextBounds(text, 0, text.length(), bounds);

        float x = rc.exactCenterX() - bounds.exactCenterX();
        float y = rc.exactCenterY() - bounds.exactCenterY();

        paintTextFill.setTextSize(textSize);
        paintTextSt.setTextSize(textSize);

        canvas.drawText(text, x, y, paintTextFill);
        canvas.drawText(text, x, y, paintTextSt);
    }

    @Override
    public CharSequence getText()
    {
        return String.format(Locale.US, "%d", value);
    }

    public boolean setValue(int newValue)
    {
        if(newValue < 100 && newValue > -100 && newValue != value)
        {
            String before = String.format(Locale.US, "%d", value);
            String after = String.format(Locale.US, "%d", newValue);
            if(before.length() != after.length())
            {
                calcTextSize();
            }
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

    public String getDelta()
    {
        if(delta == 0)
        {
            return "=";
        }
        return String.format(Locale.US, (delta > 0) ? "+%d" : "%d", delta);
    }

    @Override
    public void setBackgroundColor(int color)
    {
        super.setBackgroundColor(color);
        setColors();
    }

    public int getBackgroundColor()
    {
        ColorDrawable cl = (ColorDrawable) getBackground();
        return (cl != null) ? cl.getColor() : Color.TRANSPARENT;
    }
}