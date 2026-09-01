package com.longx.intelligent.android.lib.longcolorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class ColorPickerView extends View {
    private float hue = 0f;
    private float sat = 1f;
    private float val = 1f;
    private float alpha = 1f;
    private float hueX;
    private float svX;
    private float svY;
    private float alphaX;
    private boolean alphaEnabled = true;
    private int hueBarHeight;
    private int svPanelHeight;
    private int alphaBarHeight;
    private final RectF satValRect = new RectF();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader hueShader;
    private Shader svShader;
    private Shader valueShader;
    private Shader satShader;
    private static final int NONE = -1;
    private static final int HUE = 0;
    private static final int SV = 1;
    private static final int ALPHA = 2;
    private int activeRegion = NONE;

    public interface OnColorChangeYier {
        void onColorChanged(int color);
    }

    private OnColorChangeYier onColorChangeYier;

    public void setOnColorChangeYier(OnColorChangeYier l) {
        setOnColorChangeYier(l, false);
    }

    public void setOnColorChangeYier(OnColorChangeYier l, boolean triggerImmediately) {
        this.onColorChangeYier = l;
        if (triggerImmediately && this.onColorChangeYier != null) {
            this.onColorChangeYier.onColorChanged(getColor());
        }
    }

    public ColorPickerView(Context c) {
        this(c, null);
    }

    public ColorPickerView(Context c, AttributeSet a) {
        this(c, a, 0);
    }

    public ColorPickerView(Context c, AttributeSet a, int defStyleAttr) {
        super(c, a, defStyleAttr);
        init(c, a);
    }

    private void init(Context context, AttributeSet attrs) {
        int defaultBarHeight = dpToPx(30.3f);
        int defaultSvHeight = dpToPx(240f);
        int defaultThickness = dpToPx(1.7f);
        int defaultIndicatorColor = Color.WHITE;
        int defaultInitColor = Color.TRANSPARENT;
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ColorPickerView);
            alphaEnabled = ta.getBoolean(R.styleable.ColorPickerView_cpv_alphaEnabled, true);
            hueBarHeight = ta.getDimensionPixelSize(R.styleable.ColorPickerView_cpv_hueBarHeight, defaultBarHeight);
            svPanelHeight = ta.getDimensionPixelSize(R.styleable.ColorPickerView_cpv_svPanelHeight, defaultSvHeight);
            alphaBarHeight = ta.getDimensionPixelSize(R.styleable.ColorPickerView_cpv_alphaBarHeight, defaultBarHeight);
            defaultIndicatorColor = ta.getColor(R.styleable.ColorPickerView_cpv_indicatorColor, defaultIndicatorColor);
            float thickness = ta.getDimension(R.styleable.ColorPickerView_cpv_indicatorThickness, defaultThickness);
            defaultInitColor = ta.getColor(R.styleable.ColorPickerView_cpv_defaultColor, defaultInitColor);
            indicatorPaint.setStrokeWidth(thickness);
            ta.recycle();
        } else {
            hueBarHeight = defaultBarHeight;
            svPanelHeight = defaultSvHeight;
            alphaBarHeight = defaultBarHeight;
            indicatorPaint.setStrokeWidth(defaultThickness);
        }
        indicatorPaint.setStyle(Paint.Style.STROKE);
        indicatorPaint.setColor(defaultIndicatorColor);
        setColor(defaultInitColor, false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = dpToPx(240);
        int desiredHeight = hueBarHeight + svPanelHeight + (alphaEnabled ? alphaBarHeight : 0);
        desiredWidth += getPaddingLeft() + getPaddingRight();
        desiredHeight += getPaddingTop() + getPaddingBottom();
        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int currentAlphaHeight = alphaEnabled ? alphaBarHeight : 0;
        satValRect.set(0, hueBarHeight, w, h - currentAlphaHeight);
        buildHueShader(w);
        buildSvShader(w, h);
        hueX = clampX((hue / 360f) * w, w);
        svX = satValRect.left + sat * satValRect.width();
        svY = satValRect.top + (1f - val) * satValRect.height();
        alphaX = clampX(alpha * w, w);
    }

    private void buildHueShader(int w) {
        hueShader = new LinearGradient(
                0, 0, w, 0,
                new int[]{
                        Color.RED, Color.YELLOW, Color.GREEN,
                        Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
                },
                null,
                Shader.TileMode.CLAMP
        );
    }

    private void buildSvShader(int w, int h) {
        valueShader = new LinearGradient(
                0, satValRect.top,
                0, satValRect.bottom,
                Color.WHITE,
                Color.BLACK,
                Shader.TileMode.CLAMP
        );
        satShader = new LinearGradient(
                0, 0,
                satValRect.width(), 0,
                Color.WHITE,
                Color.HSVToColor(new float[]{hue, 1f, 1f}),
                Shader.TileMode.CLAMP
        );
        svShader = new ComposeShader(
                valueShader,
                satShader,
                PorterDuff.Mode.MULTIPLY
        );
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        paint.setShader(hueShader);
        canvas.drawRect(0, 0, w, hueBarHeight, paint);
        paint.setShader(null);
        canvas.save();
        canvas.clipRect(0, 0, w, hueBarHeight);
        drawHueIndicator(canvas);
        canvas.restore();
        paint.setShader(svShader);
        canvas.drawRect(satValRect, paint);
        paint.setShader(null);
        canvas.save();
        canvas.clipRect(satValRect);
        drawSvIndicator(canvas);
        canvas.restore();
        if (alphaEnabled) {
            drawAlpha(canvas, w, h);
            canvas.save();
            canvas.clipRect(0, h - alphaBarHeight, w, h);
            drawAlphaIndicator(canvas);
            canvas.restore();
        }
    }

    private void drawAlpha(Canvas canvas, int w, int h) {
        int top = h - alphaBarHeight;
        Shader alphaShader = new LinearGradient(
                0, top,
                w, top,
                new int[]{
                        Color.TRANSPARENT,
                        Color.HSVToColor(new float[]{hue, sat, val})
                },
                null,
                Shader.TileMode.CLAMP
        );
        paint.setShader(alphaShader);
        canvas.drawRect(0, top, w, h, paint);
        paint.setShader(null);
    }

    private void drawHueIndicator(Canvas canvas) {
        canvas.drawLine(hueX, 0, hueX, hueBarHeight, indicatorPaint);
    }

    private void drawSvIndicator(Canvas canvas) {
        canvas.drawCircle(svX, svY, dpToPx(4.8f), indicatorPaint);
    }

    private void drawAlphaIndicator(Canvas canvas) {
        float top = getHeight() - alphaBarHeight;
        canvas.drawLine(alphaX, top, alphaX, getHeight(), indicatorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        int w = getWidth();
        int h = getHeight();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (y < hueBarHeight) {
                    activeRegion = HUE;
                } else if (alphaEnabled && y > h - alphaBarHeight) {
                    activeRegion = ALPHA;
                } else {
                    activeRegion = SV;
                }
                handleMove(x, y, w);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (activeRegion != NONE) {
                    handleMove(x, y, w);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeRegion = NONE;
                return true;
        }
        return true;
    }

    private void handleMove(float x, float y, int w) {
        if (activeRegion == HUE) {
            x = clampX(x, w);
            hue = (x / w) * 360f;
            hueX = x;
            buildSvShader(w, getHeight());
        } else if (activeRegion == SV) {
            float cx = clamp(x, satValRect.left, satValRect.right);
            float cy = clamp(y, satValRect.top, satValRect.bottom);
            sat = (cx - satValRect.left) / satValRect.width();
            val = 1f - (cy - satValRect.top) / satValRect.height();
            svX = cx;
            svY = cy;
        } else if (activeRegion == ALPHA) {
            x = clampX(x, w);
            alpha = x / w;
            alphaX = x;
        }
        if (onColorChangeYier != null) {
            onColorChangeYier.onColorChanged(getColor());
        }
        postInvalidateOnAnimation();
    }

    public int getColor() {
        return Color.HSVToColor((int) (alpha * 255), new float[]{hue, sat, val});
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(v, max));
    }

    private float clampX(float x, float w) {
        return Math.max(0, Math.min(x, w));
    }

    public void setColor(int color, boolean triggerListener) {
        alpha = Color.alpha(color) / 255f;
        if (!alphaEnabled) {
            alpha = 1f;
        }
        float[] hsv = new float[3];
        Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        int w = getWidth();
        int h = getHeight();
        if (w > 0 && h > 0) {
            hueX = clampX((hue / 360f) * w, w);
            svX = satValRect.left + sat * satValRect.width();
            svY = satValRect.top + (1f - val) * satValRect.height();
            alphaX = clampX(alpha * w, w);
            buildSvShader(w, h);
            postInvalidateOnAnimation();
        }
        if (triggerListener && onColorChangeYier != null) {
            onColorChangeYier.onColorChanged(getColor());
        }
    }

    public void setColor(int color) {
        setColor(color, true);
    }

    public void setColor(String hexColor) {
        try {
            setColor(Color.parseColor(hexColor), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAlphaEnabled(boolean enabled) {
        if (this.alphaEnabled == enabled) return;
        this.alphaEnabled = enabled;
        if (!enabled) this.alpha = 1f;
        requestLayout();
        invalidate();
    }

    public boolean isAlphaEnabled() {
        return alphaEnabled;
    }

    public void setHueBarHeight(int px) {
        this.hueBarHeight = px;
        requestLayout();
        invalidate();
    }

    public void setSvPanelHeight(int px) {
        this.svPanelHeight = px;
        requestLayout();
        invalidate();
    }

    public void setAlphaBarHeight(int px) {
        this.alphaBarHeight = px;
        requestLayout();
        invalidate();
    }

    public void setIndicatorColor(int color) {
        this.indicatorPaint.setColor(color);
        invalidate();
    }

    public void setIndicatorThickness(float px) {
        this.indicatorPaint.setStrokeWidth(px);
        invalidate();
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}