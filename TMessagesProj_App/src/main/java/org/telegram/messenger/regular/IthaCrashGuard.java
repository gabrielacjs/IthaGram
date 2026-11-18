package org.telegram.messenger.regular;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.exteragram.messenger.plugins.PluginsController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

// IthaGram: si la app se cierra por culpa de un plugin, guarda un registro y desactiva ese plugin
// (o todos si no se sabe cual fue) para que la proxima vez se pueda entrar.
public class IthaCrashGuard {
    public static final String PREF_LAST_LOG = "ithagram_last_crash_log";
    private static final String PREF_LAST_CRASH_MS = "ithagram_last_crash_ms";
    private static final String LOGS_DIR = "plugin_crash_logs";
    private static boolean installed;

    public static synchronized void install(final Context context) {
        if (installed || context == null) {
            return;
        }
        installed = true;
        final Context app = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        final Thread.UncaughtExceptionHandler past = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            try {
                handle(app, thread, ex);
            } catch (Throwable ignored) {
            }
            if (past != null) {
                past.uncaughtException(thread, ex);
            }
        });
    }

    private static boolean looksLikePlugin(String trace) {
        return trace.contains("com.chaquo.python")
                || trace.contains("<python>")
                || trace.contains("com.exteragram.messenger.plugins")
                || trace.contains("de.robv.android.xposed");
    }

    private static String findPluginId(String trace) {
        try {
            for (String id : PluginsController.getInstance().plugins.keySet()) {
                if (id == null || id.length() < 3) {
                    continue;
                }
                if (trace.contains("<python>." + id) || trace.contains(id + ".py") || trace.contains("/" + id + "/")) {
                    return id;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void handle(Context app, Thread thread, Throwable ex) throws Exception {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String trace = sw.toString();
        if (!looksLikePlugin(trace)) {
            return;
        }
        String pluginId = findPluginId(trace);

        SharedPreferences prefs = app.getSharedPreferences("plugin_settings", 0);
        long now = System.currentTimeMillis();
        long previous = prefs.getLong(PREF_LAST_CRASH_MS, 0);
        // dos fallos seguidos en menos de 2 minutos: no se sabe cual es, se desactivan todos
        boolean repeated = previous > 0 && now - previous < 120000;
        String blamed = repeated ? null : pluginId;

        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date(now));
        StringBuilder log = new StringBuilder();
        log.append("IthaGram - registro de fallo de plugin\n");
        log.append("Fecha: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(now))).append('\n');
        log.append("Plugin sospechoso: ").append(pluginId != null ? pluginId : "no identificado").append('\n');
        log.append("Accion: ").append(blamed != null ? "se desactivo ese plugin" : "se desactivaron todos los plugins").append('\n');
        log.append("Hilo: ").append(thread != null ? thread.getName() : "?").append('\n');
        log.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        log.append("Dispositivo: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append("\n\n");
        log.append(trace);

        File dir = new File(app.getFilesDir(), LOGS_DIR);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File file = new File(dir, "fallo_" + stamp + ".txt");
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(log.toString().getBytes("UTF-8"));
        } finally {
            out.close();
        }
        prune(dir);

        prefs.edit().putString(PREF_LAST_LOG, file.getAbsolutePath()).putLong(PREF_LAST_CRASH_MS, now).commit();
        PluginsController.markPendingSafeModeCrash(PluginsController.SafeModeReason.PLUGIN_CRASH, blamed);
        Log.e("IthaDiag", "fallo de plugin registrado: " + file.getName() + " plugin=" + pluginId);
    }

    private static void prune(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length <= 5) {
            return;
        }
        Arrays.sort(files, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
        for (int i = 5; i < files.length; i++) {
            //noinspection ResultOfMethodCallIgnored
            files[i].delete();
        }
    }
}
