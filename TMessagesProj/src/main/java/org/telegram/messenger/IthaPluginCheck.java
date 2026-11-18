package org.telegram.messenger;

/** IthaGram: gancho para revisar un plugin antes de instalarlo (lo conecta la app, que es quien tiene Python). */
public class IthaPluginCheck {
    public interface Checker {
        /** Recibe el texto del plugin y devuelve un informe en texto plano. */
        String check(String source);
    }

    private static volatile Checker checker;

    public static void setChecker(Checker c) {
        checker = c;
    }

    public static Checker getChecker() {
        return checker;
    }
}
