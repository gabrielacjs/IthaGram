package com.exteragram.messenger.plugins.ui.components;

import com.exteragram.messenger.plugins.PluginsController;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.BaseFragment;

import tw.nekomimi.nekogram.helpers.MessageHelper;

/** IthaGram: stub (sin hoja de instalacion todavia). */
public class InstallPluginBottomSheet {
    public static class PluginInstallParams {
        public final String filePath;
        public final boolean trusted;

        public PluginInstallParams(String filePath, boolean trusted) {
            this.filePath = filePath;
            this.trusted = trusted;
        }

        public static PluginInstallParams of(MessageObject messageObject) {
            return new PluginInstallParams(MessageHelper.getPathToMessage(messageObject), false);
        }
    }

    public InstallPluginBottomSheet(BaseFragment fragment, PluginsController.PluginValidationResult result, PluginInstallParams params) {
        this.params = params;
    }

    private final PluginInstallParams params;

    public void show() {
        installAndEnable(params.filePath, null);
    }

    /** IthaGram: instala un .plugin y lo activa. onDone recibe null si salio bien, o el error. */
    public static void installAndEnable(final String filePath, final org.telegram.messenger.Utilities.Callback<String> onDone) {
        com.exteragram.messenger.plugins.PythonPluginsEngine.INSTANCE.loadPluginFromFile(filePath, null, new org.telegram.messenger.Utilities.Callback<String>() {
            @Override
            public void run(final String error) {
                if (error != null) {
                    android.util.Log.e("IthaDiag", "instalar " + filePath + " FALLO: " + error);
                    if (onDone != null) onDone.run(error);
                    return;
                }
                String id = new java.io.File(filePath).getName();
                if (id.endsWith(".plugin")) id = id.substring(0, id.length() - 7);
                final String pluginId = idFromFile(filePath, id);
                com.exteragram.messenger.plugins.PluginsController.getInstance().setPluginEnabled(pluginId, true, new org.telegram.messenger.Utilities.Callback<String>() {
                    @Override
                    public void run(String enableError) {
                        android.util.Log.i("IthaDiag", "plugin " + pluginId + (enableError == null ? " instalado y activado" : " instalado, no se pudo activar: " + enableError));
                        if (onDone != null) onDone.run(enableError);
                    }
                });
            }
        });
    }

    private static String idFromFile(String filePath, String fallback) {
        try {
            com.exteragram.messenger.plugins.PluginsController.PluginValidationResult r =
                    com.exteragram.messenger.plugins.PythonPluginsEngine.INSTANCE.validatePluginFromFile(filePath);
            if (r != null && r.plugin != null) {
                return r.plugin.getId();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }
}
