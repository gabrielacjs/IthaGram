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
    public static void start(Context context) {
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
                }
            });
        } catch (Throwable t) {
            Log.e("IthaDiag", "motor Nagram: FALLO", t);
        }
    }
}
