package org.telegram.messenger;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ReplacementSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// IthaGram: cuadradito con el color al lado de los codigos #RRGGBB y #AARRGGBB en los mensajes (como exteraGram).
// No cambia el texto: solo agrega un span que dibuja el texto y el cuadrado.
public class IthaColorPreview {
    private static final Pattern HEX = Pattern.compile("(?<![\\w#&])#([0-9a-fA-F]{8}|[0-9a-fA-F]{6})(?![\\w])");
    private static final int MAX_PER_MESSAGE = 30;

    public static void apply(CharSequence text) {
        if (!(text instanceof Spannable)) {
            return;
        }
        try {
            int length = text.length();
            if (length < 7 || length > 20000) {
                return;
            }
            boolean hasHash = false;
            for (int i = 0; i < length; i++) {
                if (text.charAt(i) == '#') {
                    hasHash = true;
                    break;
                }
            }
            if (!hasHash) {
                return;
            }
            Spannable spannable = (Spannable) text;
            if (spannable.getSpans(0, length, ColorSwatchSpan.class).length > 0) {
                return;
            }
            Matcher matcher = HEX.matcher(text);
            int count = 0;
            while (count < MAX_PER_MESSAGE && matcher.find()) {
                String hex = matcher.group(1);
                long value = Long.parseLong(hex, 16);
                int color = hex.length() == 8 ? (int) value : (0xff000000 | (int) value);
                spannable.setSpan(new ColorSwatchSpan(color), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                count++;
            }
        } catch (Throwable ignore) {
        }
    }

    public static class ColorSwatchSpan extends ReplacementSpan {
        private final int color;
        private final Paint swatchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        public ColorSwatchSpan(int color) {
            this.color = color;
        }

        private static float gap() {
            return AndroidUtilities.dp(4);
        }

        private static float side(Paint paint) {
            return Math.max(AndroidUtilities.dp(12), paint.getTextSize() * 0.9f);
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            return (int) Math.ceil(paint.measureText(text, start, end) + gap() + side(paint));
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
            canvas.drawText(text, start, end, x, y, paint);
            float textWidth = paint.measureText(text, start, end);
            float side = side(paint);
            float left = x + textWidth + gap();
            float centerY = y - paint.getTextSize() * 0.32f;
            rect.set(left, centerY - side / 2f, left + side, centerY + side / 2f);
            float radius = side * 0.28f;
            swatchPaint.setStyle(Paint.Style.FILL);
            swatchPaint.setColor(color);
            canvas.drawRoundRect(rect, radius, radius, swatchPaint);
            swatchPaint.setStyle(Paint.Style.STROKE);
            swatchPaint.setStrokeWidth(AndroidUtilities.dp(0.5f));
            swatchPaint.setColor(0x40808080);
            canvas.drawRoundRect(rect, radius, radius, swatchPaint);
        }
    }
}
