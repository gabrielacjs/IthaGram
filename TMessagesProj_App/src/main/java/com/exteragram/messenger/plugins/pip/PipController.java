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
        if (requirements != null && !requirements.isEmpty()) {
            throw new UnsupportedDependencyException("pip no soportado todavia: " + requirements);
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
