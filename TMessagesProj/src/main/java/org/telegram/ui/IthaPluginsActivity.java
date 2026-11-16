package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.IthaPlugins;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Switch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.DownloadQueueManager.tr;

/**
 * IthaGram: lista de plugins (.plugin de Python). Activar/desactivar, importar y borrar.
 */
public class IthaPluginsActivity extends BaseFragment {

    private static final int REQUEST_IMPORT = 8731;

    private LinearLayout listLayout;
    private Context ctx;
    private String searchQuery = "";

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

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem searchItem = menu.addItem(1, R.drawable.msg_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onTextChanged(android.widget.EditText editText) {
                searchQuery = editText.getText().toString().trim().toLowerCase();
                rebuild();
            }

            @Override
            public void onSearchCollapse() {
                searchQuery = "";
                rebuild();
            }
        });
        searchItem.setSearchFieldHint(tr("Buscar plugins", "Search plugins"));
        menu.addItem(2, R.drawable.msg_info).setOnClickListener(v -> showHelp());

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
            final Set<String> pinned = pinnedSet();
            List<IthaPlugins.Info> shown = new ArrayList<>();
            for (IthaPlugins.Info p : plugins) {
                if (searchQuery.isEmpty() || (p.name != null && p.name.toLowerCase().contains(searchQuery)) || (p.author != null && p.author.toLowerCase().contains(searchQuery))) {
                    shown.add(p);
                }
            }
            java.util.Collections.sort(shown, (x, y) -> Boolean.compare(pinned.contains(y.id), pinned.contains(x.id)));
            for (final IthaPlugins.Info p : shown) {
                addPluginCard(context, host, p, pinned.contains(p.id));
            }
            info(context, tr("Usá el interruptor para activar o desactivar cada plugin.", "Use the switch to turn each plugin on or off."));

            section(context, tr("Agregar", "Add"));
            row(context, tr("Importar plugin", "Import plugin"), tr("Elegí un archivo .plugin de tu teléfono", "Pick a .plugin file from your phone"), this::pickFile);
        }
    }

    private Set<String> pinnedSet() {
        try {
            Set<String> saved = ctx.getSharedPreferences("itha_plugins_ui", Context.MODE_PRIVATE).getStringSet("pinned", null);
            return saved == null ? new HashSet<String>() : new HashSet<String>(saved);
        } catch (Throwable t) {
            return new HashSet<String>();
        }
    }

    private void togglePin(String id) {
        Set<String> set = pinnedSet();
        if (!set.remove(id)) {
            set.add(id);
        }
        ctx.getSharedPreferences("itha_plugins_ui", Context.MODE_PRIVATE).edit().putStringSet("pinned", set).apply();
        rebuild();
    }

    private File pluginFile(IthaPlugins.Info p) {
        try {
            File dir = IthaPlugins.pluginsDir();
            File f = new File(dir, p.id + ".plugin");
            if (f.exists()) {
                return f;
            }
            File[] all = dir.listFiles();
            if (all != null) {
                for (File c : all) {
                    if (c.getName().endsWith(".plugin") && c.getName().startsWith(p.id)) {
                        return c;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void sharePlugin(IthaPlugins.Info p) {
        try {
            File src = pluginFile(p);
            if (src == null) {
                showError(tr("No encontré el archivo de este plugin", "Could not find this plugin's file"));
                return;
            }
            File out = new File(AndroidUtilities.getCacheDir(), p.id + ".plugin");
            try (InputStream in = new java.io.FileInputStream(src); OutputStream os = new FileOutputStream(out)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    os.write(buf, 0, n);
                }
            }
            Uri uri = androidx.core.content.FileProvider.getUriForFile(ctx, org.telegram.messenger.ApplicationLoader.getApplicationId() + ".provider", out);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, p.name), 8732);
        } catch (Throwable t) {
            showError(String.valueOf(t));
        }
    }

    private void showPluginInfo(IthaPlugins.Info p) {
        if (getParentActivity() == null) {
            return;
        }
        File f = pluginFile(p);
        String text = "ID: " + p.id + "\n" + tr("Versión: ", "Version: ") + p.version + "\n" + tr("Autor: ", "Author: ") + p.author
                + "\n" + tr("Estado: ", "Status: ") + (p.enabled ? (p.loaded ? tr("activo", "active") : tr("activado, sin cargar", "enabled, not loaded")) : tr("desactivado", "disabled"))
                + "\n" + tr("Archivo: ", "File: ") + (f != null ? f.getAbsolutePath() : "?");
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(p.name);
        b.setMessage(text);
        b.setPositiveButton(tr("Copiar", "Copy"), (d, w) -> AndroidUtilities.addToClipboard(text));
        b.setNegativeButton(tr("Cerrar", "Close"), null);
        showDialog(b.create());
    }

    private void showHelp() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(tr("Plugins", "Plugins"));
        b.setMessage(tr("Compartir: manda el archivo .plugin.\nAbrir: datos del plugin.\nFijar: lo sube al principio de la lista.\nAjustes: opciones del plugin.\nBorrar: lo elimina.",
                "Share: sends the .plugin file.\nOpen: plugin details.\nPin: moves it to the top.\nSettings: plugin options.\nDelete: removes it."));
        b.setPositiveButton("OK", null);
        showDialog(b.create());
    }

    private ImageView actionIcon(Context context, int res, int color, Runnable action) {
        ImageView iv = new ImageView(context);
        iv.setImageResource(res);
        iv.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 1, dp(20)));
        iv.setOnClickListener(v -> action.run());
        return iv;
    }

    private void addPluginCard(final Context context, final IthaPlugins.Host host, final IthaPlugins.Info p, boolean pinned) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Theme.createRoundRectDrawable(dp(16), Theme.getColor(Theme.key_windowBackgroundWhite)));
        card.setPadding(dp(16), dp(16), dp(16), dp(8));

        LinearLayout top = new LinearLayout(context);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        String name = p.name == null || p.name.isEmpty() ? p.id : p.name;
        int hue = Math.abs(name.hashCode()) % 360;
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.HSVToColor(new float[]{hue, 0.65f, 0.95f}), Color.HSVToColor(new float[]{(hue + 40) % 360, 0.75f, 0.75f})});
        g.setCornerRadius(dp(14));
        TextView icon = new TextView(context);
        icon.setText(name.substring(0, 1).toUpperCase());
        icon.setTextSize(24);
        icon.setTypeface(AndroidUtilities.bold());
        icon.setTextColor(Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(g);
        top.addView(icon, LayoutHelper.createLinear(52, 52));

        View spacer = new View(context);
        top.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        final Switch sw = new Switch(context);
        sw.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        sw.setChecked(p.enabled, false);
        sw.setOnClickListener(v -> {
            boolean value = !p.enabled;
            String err = host.setEnabled(p.id, value);
            if (err != null) {
                sw.setChecked(p.enabled, false);
                showError(err);
                return;
            }
            rebuild();
        });
        top.addView(sw, LayoutHelper.createLinear(37, 20));
        card.addView(top, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText((pinned ? "\uD83D\uDCCC " : "") + name);
        title.setTextSize(20);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 0));

        String meta = (p.version == null || p.version.isEmpty() ? "" : tr("Versión ", "Version ") + p.version) + (p.author == null || p.author.isEmpty() ? "" : "  \u00B7  " + p.author);
        if (!meta.isEmpty()) {
            TextView metaView = new TextView(context);
            metaView.setText(meta);
            metaView.setTextSize(14);
            metaView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            card.addView(metaView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        }
        if (p.description != null && !p.description.isEmpty()) {
            TextView desc = new TextView(context);
            desc.setText(p.description);
            desc.setTextSize(15);
            desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            card.addView(desc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));
        }

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        card.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 12, 0, 4));

        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        int gray = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
        actions.addView(actionIcon(context, R.drawable.msg_share, gray, () -> sharePlugin(p)), LayoutHelper.createLinear(48, 44));
        actions.addView(actionIcon(context, R.drawable.msg_openin, gray, () -> showPluginInfo(p)), LayoutHelper.createLinear(48, 44));
        actions.addView(actionIcon(context, R.drawable.msg_pin, gray, () -> togglePin(p.id)), LayoutHelper.createLinear(48, 44));
        actions.addView(actionIcon(context, R.drawable.msg_settings, gray, () -> {
            if (p.enabled && p.loaded && p.hasSettings) {
                presentFragment(new IthaPluginSettingsActivity(p.id, p.name));
            } else {
                android.widget.Toast.makeText(context, tr("Este plugin no tiene ajustes", "This plugin has no settings"), android.widget.Toast.LENGTH_SHORT).show();
            }
        }), LayoutHelper.createLinear(48, 44));
        View grow = new View(context);
        actions.addView(grow, new LinearLayout.LayoutParams(0, 1, 1f));
        actions.addView(actionIcon(context, R.drawable.msg_delete, Theme.getColor(Theme.key_text_RedBold), () -> confirmRemove(p)), LayoutHelper.createLinear(48, 44));
        card.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        listLayout.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, 6, 12, 6));
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
