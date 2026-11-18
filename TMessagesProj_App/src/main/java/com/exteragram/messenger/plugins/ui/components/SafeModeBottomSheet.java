package com.exteragram.messenger.plugins.ui.components;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.plugins.PluginsController;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/** IthaGram: aviso tras un fallo causado por un plugin, con opcion de guardar el registro. */
public class SafeModeBottomSheet {
    private final BaseFragment fragment;

    public SafeModeBottomSheet(BaseFragment fragment) {
        this.fragment = fragment;
    }

    private static String readLog(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        try {
            File file = new File(path);
            if (!file.exists()) {
                return null;
            }
            FileInputStream in = new FileInputStream(file);
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                return out.toString("UTF-8");
            } finally {
                in.close();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public void show() {
        final Activity activity = fragment != null ? fragment.getParentActivity() : null;
        if (activity == null) {
            return;
        }
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("plugin_settings", 0);
        final String logPath = prefs.getString("ithagram_last_crash_log", null);
        final String logText = readLog(logPath);
        final boolean allOff = ExteraConfig.pluginsSafeMode;

        StringBuilder message = new StringBuilder();
        message.append(PluginsController.getSafeModeStatusText());
        message.append("\n");
        message.append(activity.getString(allOff ? R.string.IthaPl_SafeModeAllOff : R.string.IthaPl_SafeModeOnlyOne));
        if (logText != null) {
            message.append("\n\n").append(activity.getString(R.string.IthaPl_SafeModeAskSave));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.IthaPl_SafeModeTitle));
        builder.setMessage(message.toString());
        if (logText != null) {
            builder.setPositiveButton(activity.getString(R.string.IthaPl_SafeModeSaveLog), (dialog, which) -> saveAndShare(activity, logPath, logText));
        }
        if (allOff) {
            builder.setNeutralButton(activity.getString(R.string.IthaPl_SafeModeReenable), (dialog, which) -> {
                PluginsController.disableSafeMode();
                PluginsController.getInstance().restart();
            });
        }
        builder.setNegativeButton(activity.getString(R.string.IthaPl_SafeModeClose), null);
        builder.show();
    }

    private static void saveAndShare(Activity activity, String logPath, String logText) {
        // copia visible en Android/data/<paquete>/files/plugins/fallos
        try {
            File ext = activity.getExternalFilesDir(null);
            if (ext != null && logPath != null) {
                File dir = new File(new File(ext, "plugins"), "fallos");
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
                FileOutputStream out = new FileOutputStream(new File(dir, new File(logPath).getName()));
                try {
                    out.write(logText.getBytes("UTF-8"));
                } finally {
                    out.close();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            String text = logText.length() > 60000 ? logText.substring(0, 60000) : logText;
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "IthaGram plugin crash");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.IthaPl_SafeModeSaveLog)));
        } catch (Throwable ignored) {
        }
    }
}
