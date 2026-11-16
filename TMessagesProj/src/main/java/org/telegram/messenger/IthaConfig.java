package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * IthaGram: ajustes propios (pantalla "Itha Ajustes"). Se guardan en SharedPreferences "itha_config".
 */
public class IthaConfig {

    public static final String DEFAULT_TITLE = "IthaGram";

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("itha_config", Context.MODE_PRIVATE);
    }

    /** Texto que el usuario escribio (vacio = usar el nombre por defecto). */
    public static String getTitleTextRaw() {
        String text = prefs().getString("title_text", "");
        return text == null ? "" : text;
    }

    /** Texto que se muestra como titulo de la lista de chats. */
    public static String getTitleText() {
        String text = getTitleTextRaw().trim();
        return text.isEmpty() ? DEFAULT_TITLE : text;
    }

    public static void setTitleText(String text) {
        prefs().edit().putString("title_text", text == null ? "" : text.trim()).apply();
    }

    /** Titulo centrado (por defecto: a la izquierda). */
    public static boolean isTitleCentered() {
        return prefs().getBoolean("title_centered", false);
    }

    public static void setTitleCentered(boolean value) {
        prefs().edit().putBoolean("title_centered", value).apply();
    }

    /** Ocultar la barra "Buscar chats" de la lista (queda solo la lupa arriba). */
    public static boolean isSearchBarHidden() {
        return prefs().getBoolean("search_bar_hidden", false);
    }

    public static void setSearchBarHidden(boolean value) {
        prefs().edit().putBoolean("search_bar_hidden", value).apply();
    }

    // ---- numero de telefono en Ajustes ----

    /** Ocultar el numero de telefono de la cabecera de Ajustes. */
    public static boolean isPhoneHidden() {
        return prefs().getBoolean("phone_hidden", false);
    }

    public static void setPhoneHidden(boolean value) {
        prefs().edit().putBoolean("phone_hidden", value).apply();
    }

    /** Devuelve el numero, o nada si la opcion de ocultarlo esta activa. */
    public static String maskPhone(String phone) {
        if (phone == null || !isPhoneHidden()) {
            return phone;
        }
        return "";
    }

    // ---- encabezado del chat ----

    /** Ocultar el avatar del encabezado dentro de los chats (se aplica al abrir un chat). */
    public static boolean isChatAvatarHidden() {
        return prefs().getBoolean("chat_avatar_hidden", false);
    }

    public static void setChatAvatarHidden(boolean value) {
        prefs().edit().putBoolean("chat_avatar_hidden", value).apply();
    }

    /** Ocultar el fondo (la pildora de vidrio) detras del titulo del chat. */
    public static boolean isChatPillHidden() {
        return prefs().getBoolean("chat_pill_hidden", false);
    }

    public static void setChatPillHidden(boolean value) {
        prefs().edit().putBoolean("chat_pill_hidden", value).apply();
    }

    /** Dice si esta barra es el encabezado de un chat (tiene el contenedor del avatar y titulo). */
    public static boolean hasChatHeader(android.view.ViewGroup bar) {
        for (int i = 0, n = bar.getChildCount(); i < n; i++) {
            if (bar.getChildAt(i) instanceof org.telegram.ui.Components.ChatAvatarContainer) {
                return true;
            }
        }
        return false;
    }

    /** Pildora compacta: el fondo del titulo del chat se ajusta mas al texto. */
    public static boolean isChatPillCompact() {
        return prefs().getBoolean("chat_pill_compact", false);
    }

    public static void setChatPillCompact(boolean value) {
        prefs().edit().putBoolean("chat_pill_compact", value).apply();
    }

    /** La lista de chats que esta abierta (para cambiar titulo y centrado al instante). */
    public static java.lang.ref.WeakReference<org.telegram.ui.DialogsActivity> dialogsRef;

    /** La pantalla de Ajustes que esta abierta (para refrescar su cabecera al instante). */
    public static java.lang.ref.WeakReference<org.telegram.ui.SettingsActivity> settingsRef;

    /** Si el numero esta oculto, saca el separador que quedaria sobrando al principio ("@usuario" solo). */
    public static CharSequence fixHeader(StringBuilder sb) {
        if (isPhoneHidden() && sb.indexOf(" \u2022 ") == 0) {
            return sb.substring(3);
        }
        return sb;
    }
}
