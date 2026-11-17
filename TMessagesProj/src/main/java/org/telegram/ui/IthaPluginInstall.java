package org.telegram.ui;

import android.app.Activity;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.IthaPlugins;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Al tocar un archivo .plugin en un chat: ofrece Instalar / Actualizar. */
public class IthaPluginInstall {

    private static String field(String src, String name) {
        Matcher m = Pattern.compile("__" + name + "__\\s*=\\s*[\"']([^\"']*)[\"']").matcher(src);
        return m.find() ? m.group(1) : null;
    }

    /** Devuelve true si se mostro el dialogo (el toque ya fue atendido). */
    public static boolean handle(final ChatActivity fragment, MessageObject message) {
        try {
            final Activity activity = fragment.getParentActivity();
            IthaPlugins.Host host = IthaPlugins.getHost();
            if (activity == null || host == null) {
                return false;
            }
            File file = null;
            if (message.messageOwner.attachPath != null && message.messageOwner.attachPath.length() != 0) {
                File f = new File(message.messageOwner.attachPath);
                if (f.exists()) {
                    file = f;
                }
            }
            if (file == null) {
                File f = fragment.getFileLoader().getPathToMessage(message.messageOwner);
                if (f != null && f.exists()) {
                    file = f;
                }
            }
            if (file == null) {
                return false; // todavia no esta descargado: que Telegram lo descargue
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0 && bos.size() < 4 * 1024 * 1024) {
                    bos.write(buf, 0, n);
                }
            }
            String src = bos.toString("UTF-8");
            String id = field(src, "id");
            String name = field(src, "name");
            String version = field(src, "version");
            if (id == null) {
                return false;
            }
            if (name == null) {
                name = id;
            }
            String oldVersion = null;
            boolean installed = false;
            List<IthaPlugins.Info> list = host.list();
            if (list != null) {
                for (IthaPlugins.Info info : list) {
                    if (id.equals(info.id)) {
                        installed = true;
                        oldVersion = info.version;
                    }
                }
            }
            final File fileFinal = file;
            final boolean update = installed;
            final String shownName = name;
            AlertDialog.Builder b = new AlertDialog.Builder(activity);
            b.setTitle(update ? "Actualizar plugin" : "Instalar plugin");
            StringBuilder msg = new StringBuilder(shownName);
            if (version != null) {
                msg.append(" ").append(version);
            }
            if (update && oldVersion != null) {
                msg.append("\n\nVersion instalada: ").append(oldVersion);
            }
            if (!update) {
                msg.append("\n\nInstala solo plugins de gente en la que confies: cada quien es responsable de los plugins que usa.");
            }
            b.setMessage(msg.toString());
            b.setPositiveButton(update ? "Actualizar" : "Instalar", (d, w) -> {
                Utilities.globalQueue.postRunnable(() -> {
                    String err;
                    try {
                        err = host.importPlugin(fileFinal.getAbsolutePath());
                    } catch (Throwable t) {
                        err = String.valueOf(t.getMessage());
                    }
                    final String error = err;
                    AndroidUtilities.runOnUIThread(() -> {
                        if (error == null || error.length() == 0) {
                            Toast.makeText(activity, shownName + (update ? " actualizado" : " instalado"), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, error, Toast.LENGTH_LONG).show();
                        }
                    });
                });
            });
            b.setNegativeButton("Cancelar", null);
            fragment.showDialog(b.create());
            return true;
        } catch (Throwable t) {
            org.telegram.messenger.FileLog.e(t);
            return false;
        }
    }
}
