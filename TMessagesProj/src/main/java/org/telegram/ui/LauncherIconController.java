package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.drawable.ic_ithagram_background, R.drawable.ic_ithagram_foreground, R.string.AppIconIthaAzul),
        CORAL("CoralIcon", R.drawable.ic_ithagram_bg_coral, R.drawable.ic_ithagram_fg_coral, R.string.AppIconIthaCoral),
        HIELO("HieloIcon", R.drawable.ic_ithagram_bg_hielo, R.drawable.ic_ithagram_fg_hielo, R.string.AppIconIthaHielo),
        MENTA("MentaIcon", R.drawable.ic_ithagram_bg_menta, R.drawable.ic_ithagram_fg_menta, R.string.AppIconIthaMenta),
        AURORA("AuroraIcon", R.drawable.ic_ithagram_bg_aurora, R.drawable.ic_ithagram_fg_aurora, R.string.AppIconIthaAurora),
        DORADO("DoradoIcon", R.drawable.ic_ithagram_bg_dorado, R.drawable.ic_ithagram_fg_dorado, R.string.AppIconIthaDorado);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
