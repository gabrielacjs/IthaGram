package org.telegram.messenger.regular;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;

import androidx.annotation.Keep;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

// IthaGram: arranque de Python (plugins). Se llama por reflexion desde ApplicationLoader.
@Keep
public class IthaPy {
    @Keep
    public static void start(Context context) {
        try {
            // solo en el proceso principal (no en el de widgets u otros)
            if (Build.VERSION.SDK_INT >= 28 && !context.getPackageName().equals(Application.getProcessName())) {
                return;
            }
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(context));
            }
            String r = Python.getInstance().getModule("ithahello").callAttr("hello").toString();
            Log.i("IthaPy", "OK: " + r);
            org.telegram.messenger.IthaPlugins.setHost(new IthaPyHost());
            startPlugins(context);
        } catch (Throwable t) {
            Log.e("IthaPy", "FALLO", t);
        }
    }

    private static void startPlugins(final Context context) {
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File ext = app.getExternalFilesDir(null);
                    String extDir = ext != null ? new File(ext, "plugins").getAbsolutePath() : "";
                    String intDir = new File(app.getFilesDir(), "plugins").getAbsolutePath();
                    String stateDir = new File(app.getFilesDir(), "plugins_data").getAbsolutePath();
                    new File(intDir).mkdirs();
                    if (ext != null) {
                        new File(extDir).mkdirs();
                    }
                    Python.getInstance().getModule("ithaengine").callAttr("start", new String[]{extDir, intDir}, stateDir);
                } catch (Throwable t) {
                    Log.e("IthaPy", "motor de plugins FALLO", t);
                }
            }
        }, "IthaPlugins").start();
    }
}
