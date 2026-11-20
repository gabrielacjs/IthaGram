package org.telegram.messenger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;

/**
 * IthaGram: estilo de cada parte de la app. Por ahora: encabezado del chat (0 = Telegram, 1 = Material 3).
 */
public class IthaStyles {

    private static int chatHeader = -1;
    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static int barColor;

    /** Color de la barra del encabezado del chat en Material 3 (0 si todavia no se dibujo). */
    public static int barColor() {
        return barColor;
    }

    public static int chatHeader() {
        if (chatHeader < 0) {
            try {
                chatHeader = ApplicationLoader.applicationContext.getSharedPreferences("itha_styles", Context.MODE_PRIVATE).getInt("chat_header", 0);
            } catch (Throwable t) {
                chatHeader = 0;
            }
            if (chatHeader != 1) {
                chatHeader = 0;
            }
        }
        return chatHeader;
    }

    public static void setChatHeader(int value) {
        chatHeader = value == 1 ? 1 : 0;
        try {
            ApplicationLoader.applicationContext.getSharedPreferences("itha_styles", Context.MODE_PRIVATE).edit().putInt("chat_header", chatHeader).apply();
        } catch (Throwable ignore) {
        }
    }

    /** Barra solida de Material 3 detras del encabezado del chat (en lugar de las capsulas de vidrio). */
    public static void drawChatHeaderBar(Canvas canvas, int width, int height, int glassColor) {
        final int under = Theme.getColor(Theme.key_windowBackgroundWhite);
        int base = glassColor == 0 ? under : ColorUtils.compositeColors(glassColor, under);
        base |= 0xFF000000;
        barColor = base;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(base);
        canvas.drawRect(0, 0, width, height, paint);
        paint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 31));
        canvas.drawRect(0, height - AndroidUtilities.dp(1), width, height, paint);
    }
}
