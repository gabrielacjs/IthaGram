package org.telegram.ui;

/**
 * IthaGram: nombres de los iconos de la app (fijos, no pasan por el sistema de idiomas de Telegram).
 */
public class IthaIconNames {
    public static String get(LauncherIconController.LauncherIcon icon) {
        switch (icon) {
            case CORAL:
                return "IthaCoral";
            case HIELO:
                return "IthaIce";
            case MENTA:
                return "IthaMint";
            case AURORA:
                return "IthaAurora";
            case DORADO:
                return "IthaGold";
            default:
                return "IthaBlue";
        }
    }
}
