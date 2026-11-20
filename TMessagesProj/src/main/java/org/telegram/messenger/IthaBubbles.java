package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * IthaGram: estilo de las burbujas de mensajes. 0 = Telegram, 2 = Material 3 (sin cola).
 */
public class IthaBubbles {

    private static int cached = -1;

    public static int style() {
        if (cached < 0) {
            try {
                cached = prefs().getInt("style", 0);
            } catch (Throwable t) {
                cached = 0;
            }
            if (cached != 2) {
                cached = 0;
            }
        }
        return cached;
    }

    public static void set(int value) {
        cached = value == 2 ? 2 : 0;
        try {
            prefs().edit().putInt("style", cached).apply();
        } catch (Throwable ignore) {
        }
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("itha_bubbles", Context.MODE_PRIVATE);
    }
}
