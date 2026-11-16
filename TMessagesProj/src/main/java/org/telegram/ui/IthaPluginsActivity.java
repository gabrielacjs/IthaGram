package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.IthaPlugins;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.DownloadQueueManager.tr;

/**
 * IthaGram: lista de plugins (.plugin de Python). Activar/desactivar, importar y borrar.
 */
public class IthaPluginsActivity extends BaseFragment {

    private static final int REQUEST_IMPORT = 8731;

    private LinearLayout listLayout;
    private Context ctx;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(tr("Plugins", "Plugins"));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        ScrollView scroll = new ScrollView(context);
        listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listLayout, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        ctx = context;
        rebuild();

        return fragmentView;
    }

    private void rebuild() {
        listLayout.removeAllViews();
        final Context context = ctx;
        final IthaPlugins.Host host = IthaPlugins.getHost();

        section(context, tr("Plugins instalados", "Installed plugins"));

        if (host == null) {
            info(context, tr("Los plugins todavía no están listos. Cerrá y abrí la app.", "Plugins are not ready yet. Close and reopen the app."));
        } else {
            List<IthaPlugins.Info> plugins = host.list();
            if (plugins.isEmpty()) {
                info(context, tr("No hay plugins instalados. Usá \"Importar plugin\".", "No plugins installed. Use \"Import plugin\"."));
            }
            for (final IthaPlugins.Info p : plugins) {
                final TextCheckCell cell = new TextCheckCell(context);
                cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                String title = p.name + (p.version == null || p.version.isEmpty() ? "" : "  v" + p.version);
                String sub = p.description != null && !p.description.isEmpty() ? p.description : p.author;
                cell.setTextAndValueAndCheck(title, sub, p.enabled, true, false);
                cell.setOnClickListener(v -> {
                    boolean value = !cell.isChecked();
                    String err = host.setEnabled(p.id, value);
                    if (err != null) {
                        showError(err);
                        return;
                    }
                    cell.setChecked(value);
                });
                cell.setOnLongClickListener(v -> {
                    confirmRemove(p);
                    return true;
                });
                listLayout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                if (p.enabled && p.loaded && p.hasSettings) {
                    row(context, tr("Ajustes de ", "Settings of ") + p.name, null, () -> presentFragment(new IthaPluginSettingsActivity(p.id, p.name)));
                }
            }
            info(context, tr("Tocá un plugin para activarlo o desactivarlo. Mantenelo presionado para borrarlo.", "Tap a plugin to turn it on or off. Hold it to delete it."));

            section(context, tr("Agregar", "Add"));
            row(context, tr("Importar plugin", "Import plugin"), tr("Elegí un archivo .plugin de tu teléfono", "Pick a .plugin file from your phone"), this::pickFile);
        }
    }

    private void pickFile() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Plugin"), REQUEST_IMPORT);
        } catch (Throwable t) {
            showError(String.valueOf(t.getMessage()));
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_IMPORT || resultCode != android.app.Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        IthaPlugins.Host host = IthaPlugins.getHost();
        if (host == null) {
            return;
        }
        try {
            Uri uri = data.getData();
            String name = null;
            Cursor c = ctx.getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                try {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0 && c.moveToFirst()) {
                        name = c.getString(idx);
                    }
                } finally {
                    c.close();
                }
            }
            if (name == null || !name.endsWith(".plugin")) {
                showError(tr("El archivo tiene que terminar en .plugin", "The file must end in .plugin"));
                return;
            }
            name = name.replace("/", "_");
            File out = new File(IthaPlugins.pluginsDir(), name);
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) {
                showError(tr("No se pudo leer el archivo", "Could not read the file"));
                return;
            }
            try (InputStream input = in; OutputStream os = new FileOutputStream(out)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = input.read(buf)) > 0) {
                    os.write(buf, 0, n);
                }
            }
            String err = host.importPlugin(out.getAbsolutePath());
            if (err != null) {
                out.delete();
                showError(err);
                return;
            }
            rebuild();
        } catch (Throwable t) {
            showError(String.valueOf(t.getMessage()));
        }
    }

    private void confirmRemove(final IthaPlugins.Info p) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(tr("Borrar plugin", "Delete plugin"));
        builder.setMessage(p.name);
        builder.setPositiveButton(tr("Borrar", "Delete"), (dialog, which) -> {
            IthaPlugins.Host host = IthaPlugins.getHost();
            if (host != null) {
                String err = host.remove(p.id);
                if (err != null) {
                    showError(err);
                }
                rebuild();
            }
        });
        builder.setNegativeButton(tr("Cancelar", "Cancel"), null);
        showDialog(builder.create());
    }

    private void showError(String message) {
        try {
            // dialogo con el error completo y un boton para copiarlo (para mandarlo a quien lo arregla)
            final String text = String.valueOf(message);
            AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
            b.setTitle(tr("Error del plugin", "Plugin error"));
            b.setMessage(text);
            b.setPositiveButton(tr("Copiar log", "Copy log"), (d, w) -> {
                AndroidUtilities.addToClipboard(text);
                android.widget.Toast.makeText(getParentActivity(), tr("Log copiado", "Log copied"), android.widget.Toast.LENGTH_SHORT).show();
            });
            b.setNegativeButton(tr("Cerrar", "Close"), null);
            showDialog(b.create());
        } catch (Throwable ignored) {
        }
    }

    private void info(Context context, String text) {
        TextView info = new TextView(context);
        info.setText(text);
        info.setTextSize(13);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setPadding(dp(20), dp(10), dp(20), dp(10));
        listLayout.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void section(Context context, String title) {
        TextView header = new TextView(context);
        header.setText(title);
        header.setTextSize(14);
        header.setTypeface(AndroidUtilities.bold());
        header.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        header.setPadding(dp(20), dp(18), dp(20), dp(8));
        listLayout.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void row(Context context, String title, String subtitle, Runnable action) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        row.setPadding(dp(20), dp(12), dp(20), dp(12));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        row.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (subtitle != null) {
            TextView subtitleView = new TextView(context);
            subtitleView.setText(subtitle);
            subtitleView.setTextSize(13);
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            row.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        }

        row.setOnClickListener(v -> action.run());
        listLayout.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }
}
