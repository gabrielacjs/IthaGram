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
        return chatHeader() == 1 ? barColor : 0;
    }

    private static int pinned = -1;
    private static int input = -1;
    private static int list = -1;

    /** Estilo de la lista de chats (0 = Telegram, 1 = Material 3: tarjetas). Si nunca se eligio, sigue al encabezado. */
    public static int list() {
        if (list < 0) {
            list = read("list");
            if (list != 0 && list != 1) {
                list = -2;
            }
        }
        return list == -2 ? chatHeader() : list;
    }

    public static void setList(int value) {
        list = value == 1 ? 1 : 0;
        write("list", list);
    }

    /** Tarjeta tonal de Material 3 detras de cada chat de la lista. */
    public static void drawListCard(Canvas canvas, android.graphics.RectF rect, boolean pinned, boolean selected) {
        final int base = Theme.getColor(Theme.key_windowBackgroundWhite) | 0xFF000000;
        final int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ColorUtils.blendARGB(base, accent, selected ? 0.30f : 0.10f) | 0xFF000000);
        final float r = Math.min(AndroidUtilities.dp(20), rect.height() / 2f);
        canvas.drawRoundRect(rect, r, r, paint);
    }

    private static int read(String key) {
        try {
            return ApplicationLoader.applicationContext.getSharedPreferences("itha_styles", Context.MODE_PRIVATE).getInt(key, -1);
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void write(String key, int value) {
        try {
            ApplicationLoader.applicationContext.getSharedPreferences("itha_styles", Context.MODE_PRIVATE).edit().putInt(key, value).apply();
        } catch (Throwable ignore) {
        }
    }

    /** Estilo del mensaje fijado (0 = Telegram, 1 = Material 3). Si nunca se eligio, sigue al encabezado. */
    public static int pinned() {
        if (pinned < 0) {
            pinned = read("pinned");
            if (pinned != 0 && pinned != 1) {
                pinned = -2;
            }
        }
        return pinned == -2 ? chatHeader() : pinned;
    }

    public static void setPinned(int value) {
        pinned = value == 1 ? 1 : 0;
        write("pinned", pinned);
    }

    /** Estilo de la barra de mensaje (0 = Telegram, 1 = Material 3). Si nunca se eligio, sigue al encabezado. */
    public static int input() {
        if (input < 0) {
            input = read("input");
            if (input != 0 && input != 1) {
                input = -2;
            }
        }
        return input == -2 ? chatHeader() : input;
    }

    public static void setInput(int value) {
        input = value == 1 ? 1 : 0;
        write("input", input);
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
