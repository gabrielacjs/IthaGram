package org.telegram.ui;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import static org.telegram.messenger.AndroidUtilities.dp;

/**
 * IthaGram: ayudas del estilo Material 3 (opcional). Si esta apagado todo se ve como siempre.
 */
public class IthaMd3 {

    public static boolean on() {
        return IthaSettingsActivity.isMd3();
    }

    /** Color de las tarjetas: si el tema usa el mismo color que el fondo, se aclara un poco para que se note la forma. */
    public static int cardColor() {
        int base = Theme.getColor(Theme.key_windowBackgroundWhite);
        if (base == Theme.getColor(Theme.key_windowBackgroundGray)) {
            return androidx.core.graphics.ColorUtils.blendARGB(base, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 0.07f);
        }
        return base;
    }

    /** Fondo redondeado. En Material 3 las esquinas son mas grandes. */
    public static Drawable roundBg(int classicRadius) {
        if (on()) {
            return Theme.createRoundRectDrawable(dp(classicRadius + 8), cardColor());
        }
        return Theme.createRoundRectDrawable(dp(classicRadius), Theme.getColor(Theme.key_windowBackgroundWhite));
    }

    /** Fondo de una fila de ajustes. */
    public static void style(View v) {
        if (on()) {
            v.setBackground(Theme.createRoundRectDrawable(dp(20), cardColor()));
        } else {
            v.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        }
    }

    /** Agrega la fila a la lista: a todo el ancho (clasico) o como tarjeta separada (Material 3). */
    public static void add(LinearLayout list, View v) {
        if (on()) {
            list.addView(v, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 3, 12, 3));
        } else {
            list.addView(v, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
    }
}
