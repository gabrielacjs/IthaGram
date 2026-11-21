package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.LaunchActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.telegram.messenger.DownloadQueueManager.tr;

/**
 * IthaGram: aviso de actualizacion. Mira la ultima Release publicada en GitHub y, si es mas nueva que la
 * version instalada, muestra un dialogo con las novedades y un boton para descargar el APK.
 *
 * IMPORTANTE: antes de compilar cada Release nueva, subir REVISION (v13.2.2-itha = 1, -itha2 = 2, -itha3 = 3...).
 */
public class IthaUpdate {

    /** Numero de revision de Itha de ESTA compilacion (el numero que va al final del tag: -itha3 = 3). */
    public static final int REVISION = 3;

    private static final String API = "https://api.github.com/repos/gabrielacjs/IthaGram/releases/latest";
    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final Pattern TAG = Pattern.compile("v?(\\d+(?:\\.\\d+)*)(?:-itha(\\d*))?");

    private static volatile boolean busy;
    private static volatile boolean showing;

    private static class Release {
        String tag = "";
        String body = "";
        String apkUrl;
        String pageUrl;
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("itha_config", Context.MODE_PRIVATE);
    }

    /** Buscar solas al abrir la app (una vez al dia). */
    public static boolean isAutoCheck() {
        return prefs().getBoolean("update_auto", true);
    }

    public static void setAutoCheck(boolean value) {
        prefs().edit().putBoolean("update_auto", value).apply();
    }

    /** Version instalada, con el mismo formato que los tags de las Releases (sin la "v"). */
    public static String currentVersion() {
        return BuildVars.BUILD_VERSION_STRING + "-itha" + (REVISION > 1 ? String.valueOf(REVISION) : "");
    }

    /** Se llama al volver a la app: busca como mucho una vez cada 24 horas. */
    public static void autoCheck() {
        if (!isAutoCheck() || busy || showing) {
            return;
        }
        if (System.currentTimeMillis() - prefs().getLong("update_last_check", 0L) < DAY) {
            return;
        }
        check(null, false);
    }

    /** manual = viene del boton de Itha Ajustes: avisa tambien si no hay nada nuevo o si falla. */
    public static void check(final Activity activity, final boolean manual) {
        if (busy) {
            return;
        }
        busy = true;
        Utilities.globalQueue.postRunnable(() -> {
            Release found = null;
            try {
                found = fetch();
                prefs().edit().putLong("update_last_check", System.currentTimeMillis()).apply();
            } catch (Throwable t) {
                FileLog.e(t);
            }
            final Release release = found;
            AndroidUtilities.runOnUIThread(() -> {
                busy = false;
                deliver(activity, manual, release);
            });
        });
    }

    private static void deliver(Activity activity, boolean manual, Release release) {
        final Activity act = activity != null ? activity : LaunchActivity.instance;
        if (act == null || act.isFinishing()) {
            return;
        }
        if (release == null) {
            if (manual) {
                Toast.makeText(act, tr("No pude revisar las actualizaciones. Probá más tarde.", "Couldn't check for updates. Try again later."), Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (!isNewer(release.tag)) {
            if (manual) {
                Toast.makeText(act, tr("Ya tenés la última versión.", "You already have the latest version."), Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (showing) {
            return;
        }
        showing = true;
        String name = release.tag.startsWith("v") ? release.tag.substring(1) : release.tag;
        String notes = release.body == null ? "" : release.body.trim();
        if (notes.length() > 700) {
            notes = notes.substring(0, 700).trim() + "…";
        }
        final String message = "IthaGram " + name + (notes.isEmpty() ? "" : "\n\n" + notes);
        final String url = release.apkUrl != null ? release.apkUrl : release.pageUrl;
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(act);
            builder.setTitle(tr("Hay una versión nueva", "A new version is available"));
            builder.setMessage(message);
            builder.setPositiveButton(tr("Descargar", "Download"), (dialog, which) -> {
                try {
                    act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Throwable t) {
                    FileLog.e(t);
                }
            });
            builder.setNegativeButton(tr("Más tarde", "Later"), null);
            AlertDialog dialog = builder.create();
            dialog.setOnDismissListener(d -> showing = false);
            dialog.show();
        } catch (Throwable t) {
            showing = false;
            FileLog.e(t);
        }
    }

    private static Release fetch() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(API).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("User-Agent", "IthaGram");
        try {
            if (c.getResponseCode() != 200) {
                throw new java.io.IOException("HTTP " + c.getResponseCode());
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            JSONObject o = new JSONObject(sb.toString());
            Release rel = new Release();
            rel.tag = o.optString("tag_name", "");
            rel.body = o.optString("body", "");
            rel.pageUrl = o.optString("html_url", "https://github.com/gabrielacjs/IthaGram/releases");
            JSONArray assets = o.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.getJSONObject(i);
                    if (a.optString("name", "").toLowerCase().endsWith(".apk")) {
                        rel.apkUrl = a.optString("browser_download_url", null);
                        break;
                    }
                }
            }
            if (rel.tag.isEmpty()) {
                throw new java.io.IOException("sin tag");
            }
            return rel;
        } finally {
            c.disconnect();
        }
    }

    /** {numeros de la version..., revision}; null si el texto no tiene el formato esperado. */
    private static int[] parse(String tag) {
        Matcher m = TAG.matcher(tag == null ? "" : tag);
        if (!m.find()) {
            return null;
        }
        String[] parts = m.group(1).split("\\.");
        int[] out = new int[parts.length + 1];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Utilities.parseInt(parts[i]);
        }
        String rev = m.group(2);
        out[parts.length] = rev == null || rev.isEmpty() ? 1 : Utilities.parseInt(rev);
        return out;
    }

    private static boolean isNewer(String remoteTag) {
        int[] remote = parse(remoteTag);
        int[] local = parse(currentVersion());
        if (remote == null || local == null) {
            return false;
        }
        // se comparan los numeros de la version y, al final, la revision de Itha
        int n = Math.max(remote.length, local.length);
        int[] r = new int[n];
        int[] l = new int[n];
        System.arraycopy(remote, 0, r, 0, remote.length - 1);
        System.arraycopy(local, 0, l, 0, local.length - 1);
        r[n - 1] = remote[remote.length - 1];
        l[n - 1] = local[local.length - 1];
        for (int i = 0; i < n; i++) {
            if (r[i] != l[i]) {
                return r[i] > l[i];
            }
        }
        return false;
    }
}
