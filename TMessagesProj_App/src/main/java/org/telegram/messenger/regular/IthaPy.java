package org.telegram.messenger.regular;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Keep;

import com.exteragram.messenger.plugins.PluginsController;
import com.exteragram.messenger.plugins.PythonPluginsEngine;

// IthaGram: arranque del motor de plugins (portado de exteraGram/Nagram). Se llama por reflexion desde ApplicationLoader.
@Keep
public class IthaPy {
    @Keep
    public static void start(final Context context) {
        try {
            // solo en el proceso principal (no en el de widgets u otros)
            if (Build.VERSION.SDK_INT >= 28 && !context.getPackageName().equals(Application.getProcessName())) {
                return;
            }
            Log.i("IthaDiag", "motor Nagram: iniciando");
            PluginsController.getInstance().init(new Runnable() {
                @Override
                public void run() {
                    Log.i("IthaDiag", "motor Nagram: listo, disponible=" + PythonPluginsEngine.INSTANCE.isEngineAvailable()
                            + " plugins=" + PluginsController.getInstance().getPlugins().size());
                    IthaBridge.install();
                    installDropped(context.getApplicationContext());
                }
            });
        } catch (Throwable t) {
            Log.e("IthaDiag", "motor Nagram: FALLO", t);
        }
    }

    // Prueba: instala los .plugin que haya en Android/data/<paquete>/files/plugins (y los renombra a .instalado)
    private static void installDropped(Context app) {
        try {
            java.io.File ext = app.getExternalFilesDir(null);
            if (ext == null) return;
            java.io.File dir = new java.io.File(ext, "plugins");
            dir.mkdirs();
            java.io.File[] files = dir.listFiles();
            if (files == null) return;
            for (final java.io.File f : files) {
                if (!f.getName().endsWith(".plugin")) continue;
                Log.i("IthaDiag", "instalando " + f.getName());
                com.exteragram.messenger.plugins.ui.components.InstallPluginBottomSheet.installAndEnable(f.getAbsolutePath(), new org.telegram.messenger.Utilities.Callback<String>() {
                    @Override
                    public void run(String error) {
                        if (error == null) {
                            f.renameTo(new java.io.File(f.getParentFile(), f.getName() + ".instalado"));
                        }
                    }
                });
            }
        } catch (Throwable t) {
            Log.e("IthaDiag", "installDropped FALLO", t);
        }
    }
}
