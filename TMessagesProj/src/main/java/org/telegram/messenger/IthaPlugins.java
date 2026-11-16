package org.telegram.messenger;

import android.view.ViewGroup;

import org.telegram.ui.ActionBar.BaseFragment;

import java.io.File;
import java.util.List;

/**
 * IthaGram: puente entre la pantalla de plugins (esta libreria) y el motor de Python (modulo de la app).
 */
public class IthaPlugins {

    public static class Info {
        public String id;
        public String name;
        public String version;
        public String author;
        public String description;
        public boolean enabled;
        public boolean loaded;
        public boolean hasSettings;
    }

    public interface Host {
        List<Info> list();

        /** Devuelve null si salio bien, o el mensaje de error. */
        String setEnabled(String id, boolean enabled);

        String importPlugin(String path);

        String remove(String id);

        /** Llena `container` con los ajustes del plugin (los arma el motor de Python). */
        void fillSettings(String id, ViewGroup container, BaseFragment fragment);

        void closeSettings(String id);
    }

    private static volatile Host host;

    public static void setHost(Host h) {
        host = h;
    }

    public static Host getHost() {
        return host;
    }

    public static File pluginsDir() {
        File dir = new File(ApplicationLoader.applicationContext.getFilesDir(), "plugins");
        dir.mkdirs();
        return dir;
    }
}
