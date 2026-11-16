package org.telegram.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.IthaConfig;

/** IthaGram: reemplaza el logo "Telegram" de la fila de historias por el titulo propio (texto). */
public class IthaTitleDrawable extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String text;
    private int textWidth;

    public IthaTitleDrawable() {
        paint.setColor(0xffffffff); // el color real lo pone el filtro (MULTIPLY) de la vista
        paint.setTypeface(AndroidUtilities.bold());
        paint.setTextSize(AndroidUtilities.dp(20));
        measure();
    }

    private void measure() {
        text = IthaConfig.getTitleText();
        textWidth = (int) Math.ceil(paint.measureText(text)) + AndroidUtilities.dp(2);
    }

    /** true si el texto cambio (hay que volver a medir la vista). */
    public boolean refresh() {
        if (!IthaConfig.getTitleText().equals(text)) {
            measure();
            invalidateSelf();
            return true;
        }
        return false;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        Paint.FontMetrics fm = paint.getFontMetrics();
        float y = b.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, b.left, y, paint);
    }

    @Override
    public int getIntrinsicWidth() {
        return textWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return AndroidUtilities.dp(22);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
