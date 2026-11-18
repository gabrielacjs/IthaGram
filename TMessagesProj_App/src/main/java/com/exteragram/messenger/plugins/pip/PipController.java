package com.exteragram.messenger.plugins.pip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** IthaGram: stub. Los plugins que piden dependencias pip no estan soportados todavia. */
public class PipController {
    public interface InstallerDelegate {
        boolean isCancelled();
        void onProgress(String text);
    }

    public static final PipController INSTANCE = new PipController();

    private PipController() {
    }

    public List<String> installDependencies(List<String> requirements, String pluginId, InstallerDelegate delegate) throws Exception {
        // La app ya trae "requests" (y sus dependencias) dentro del propio Python: se aceptan sin instalar nada.
        List<String> missing = new ArrayList<>();
        if (requirements != null) {
            for (String r : requirements) {
                String name = r == null ? "" : r.trim().split("[\\s\\[<>=!~;@]", 2)[0].toLowerCase().replace('_', '-');
                if (!(name.equals("requests") || name.equals("urllib3") || name.equals("certifi") || name.equals("idna") || name.equals("charset-normalizer"))) {
                    missing.add(r);
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new UnsupportedDependencyException("pip no soportado todavia: " + missing);
        }
        return new ArrayList<>();
    }

    public void uninstallDependencies(String pluginId) {
    }

    public static class InstallationCancelledException extends IOException {
        public InstallationCancelledException() {
            super("cancelled");
        }
    }

    public static class UnsupportedDependencyException extends IOException {
        public UnsupportedDependencyException(String message) {
            super(message);
        }
    }
}
