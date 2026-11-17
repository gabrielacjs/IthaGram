package org.telegram.messenger.regular;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.exteragram.messenger.ExteraConfig;
import com.exteragram.messenger.plugins.Plugin;
import com.exteragram.messenger.plugins.PluginEvents;
import com.exteragram.messenger.plugins.PluginsController;
import com.exteragram.messenger.plugins.PythonPluginsEngine;
import com.exteragram.messenger.plugins.models.CustomSetting;
import com.exteragram.messenger.plugins.models.DividerSetting;
import com.exteragram.messenger.plugins.models.EditTextSetting;
import com.exteragram.messenger.plugins.models.HeaderSetting;
import com.exteragram.messenger.plugins.models.InputSetting;
import com.exteragram.messenger.plugins.models.SelectorSetting;
import com.exteragram.messenger.plugins.models.SettingItem;
import com.exteragram.messenger.plugins.models.SwitchSetting;
import com.exteragram.messenger.plugins.models.TextSetting;
import com.exteragram.messenger.plugins.ui.components.InstallPluginBottomSheet;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.IthaPlugins;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

// IthaGram: implementa el puente de plugins de la pantalla de Itha usando el motor de Nagram (PluginsController).
public class IthaPyHost implements IthaPlugins.Host {

    private static final String TAG = "IthaDiag";

    // estado "optimista": el motor trabaja en otro hilo, la pantalla pide la lista enseguida
    private final ConcurrentHashMap<String, Boolean> pendingEnabled = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> pendingRemoved = new ConcurrentHashMap<>();

    private static void toast(final String text) {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Toast.makeText(ApplicationLoader.applicationContext, text, Toast.LENGTH_LONG).show();
                } catch (Throwable ignored) {
                }
            }
        });
    }

    @Override
    public List<IthaPlugins.Info> list() {
        List<IthaPlugins.Info> out = new ArrayList<>();
        try {
            PluginsController pc = PluginsController.getInstance();
            for (Plugin pl : pc.getPlugins().values()) {
                if (pl == null || pendingRemoved.containsKey(pl.getId())) {
                    continue;
                }
                IthaPlugins.Info p = new IthaPlugins.Info();
                p.id = pl.getId();
                p.name = pl.getName();
                p.version = pl.getVersion();
                p.author = pl.getAuthor();
                p.description = pl.getDescription();
                Boolean pending = pendingEnabled.get(p.id);
                p.enabled = pending != null ? pending : pl.isEnabled();
                p.loaded = !pl.hasError();
                p.hasSettings = pc.hasPluginSettings(p.id);
                out.add(p);
            }
            Collections.sort(out, (x, y) -> String.valueOf(x.name).compareToIgnoreCase(String.valueOf(y.name)));
        } catch (Throwable t) {
            android.util.Log.e(TAG, "list FALLO", t);
        }
        return out;
    }

    @Override
    public String setEnabled(final String id, final boolean enabled) {
        try {
            pendingEnabled.put(id, enabled);
            PluginsController.getInstance().setPluginEnabled(id, enabled, new Utilities.Callback<String>() {
                @Override
                public void run(String error) {
                    pendingEnabled.remove(id);
                    if (error != null) {
                        android.util.Log.e(TAG, "setEnabled " + id + " FALLO: " + error);
                        toast(error);
                    }
                }
            });
            return null;
        } catch (Throwable t) {
            pendingEnabled.remove(id);
            return String.valueOf(t.getMessage());
        }
    }

    @Override
    public String importPlugin(String path) {
        try {
            File src = new File(path);
            // el motor guarda su propia copia: se instala desde una carpeta aparte para no pisar el mismo archivo
            File dir = new File(ApplicationLoader.applicationContext.getCacheDir(), "itha_import");
            dir.mkdirs();
            File tmp = new File(dir, src.getName());
            if (tmp.exists()) {
                tmp.delete();
            }
            if (!src.renameTo(tmp)) {
                java.io.FileInputStream in = new java.io.FileInputStream(src);
                java.io.FileOutputStream os = new java.io.FileOutputStream(tmp);
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    os.write(buf, 0, n);
                }
                in.close();
                os.close();
                src.delete();
            }
            InstallPluginBottomSheet.installAndEnable(tmp.getAbsolutePath(), new Utilities.Callback<String>() {
                @Override
                public void run(String error) {
                    if (error != null) {
                        toast(error);
                    }
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getGlobalInstance().postNotificationName(PluginEvents.pluginsUpdated);
                        }
                    });
                }
            });
            return null;
        } catch (Throwable t) {
            return String.valueOf(t.getMessage());
        }
    }

    @Override
    public String remove(final String id) {
        try {
            pendingRemoved.put(id, true);
            PluginsController.getInstance().deletePlugin(id, new Utilities.Callback<String>() {
                @Override
                public void run(String error) {
                    pendingRemoved.remove(id);
                    if (error != null) {
                        android.util.Log.e(TAG, "remove " + id + " FALLO: " + error);
                        toast(error);
                    }
                }
            });
            return null;
        } catch (Throwable t) {
            pendingRemoved.remove(id);
            return String.valueOf(t.getMessage());
        }
    }

    @Override
    public boolean isMasterEnabled() {
        return ExteraConfig.pluginsEngine;
    }

    @Override
    public String setMasterEnabled(boolean enabled) {
        try {
            ExteraConfig.pluginsEngine = enabled;
            if (ExteraConfig.editor != null) {
                ExteraConfig.editor.putBoolean("pluginsEngine", enabled).apply();
            }
            return null;
        } catch (Throwable t) {
            return String.valueOf(t.getMessage());
        }
    }

    private final ConcurrentHashMap<String, Screen> screens = new ConcurrentHashMap<>();

    @Override
    public void fillSettings(String id, ViewGroup container, BaseFragment fragment) {
        try {
            Screen old = screens.remove(id);
            if (old != null) {
                old.unregister();
            }
            Screen s = new Screen(id, container, fragment);
            screens.put(id, s);
            s.register();
            s.rebuild();
        } catch (Throwable t) {
            android.util.Log.e(TAG, "fillSettings FALLO", t);
        }
    }

    @Override
    public void closeSettings(String id) {
        Screen s = screens.remove(id);
        if (s != null) {
            s.unregister();
        }
    }

    // ---- pantalla de ajustes de un plugin: arma las filas desde los SettingItem del motor ----
    private static final class Screen implements NotificationCenter.NotificationCenterDelegate {
        final String pluginId;
        final ViewGroup container;
        final BaseFragment fragment;
        // null = nivel principal; si no, los items del submenu abierto
        List<SettingItem> sub;
        PyObject subCallback;
        String lastSignature;

        Screen(String pluginId, ViewGroup container, BaseFragment fragment) {
            this.pluginId = pluginId;
            this.container = container;
            this.fragment = fragment;
        }

        void register() {
            NotificationCenter.getGlobalInstance().addObserver(this, PluginEvents.pluginSettingsRegistered);
        }

        void unregister() {
            NotificationCenter.getGlobalInstance().removeObserver(this, PluginEvents.pluginSettingsRegistered);
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == PluginEvents.pluginSettingsRegistered && args != null && args.length > 0 && pluginId.equals(args[0])) {
                if (sub == null) {
                    String sig = signature(PluginsController.getInstance().getPluginSettingsList(pluginId));
                    if (!sig.equals(lastSignature)) {
                        rebuild();
                    }
                }
            }
        }

        Context ctx() {
            return container.getContext();
        }

        void add(View v) {
            container.addView(v, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        // describe la estructura visible de la lista (no los valores): si no cambia, no hace falta redibujar
        String signature(List<SettingItem> items) {
            StringBuilder sb = new StringBuilder();
            if (items != null) {
                for (SettingItem it : items) {
                    sb.append(it.getClass().getSimpleName()).append('|');
                    if (it instanceof HeaderSetting) sb.append(((HeaderSetting) it).text);
                    else if (it instanceof DividerSetting) sb.append(((DividerSetting) it).text);
                    else if (it instanceof SwitchSetting) sb.append(((SwitchSetting) it).key).append(((SwitchSetting) it).text).append(((SwitchSetting) it).subtext);
                    else if (it instanceof SelectorSetting) sb.append(((SelectorSetting) it).key).append(((SelectorSetting) it).text);
                    else if (it instanceof InputSetting) sb.append(((InputSetting) it).key).append(((InputSetting) it).text);
                    else if (it instanceof EditTextSetting) sb.append(((EditTextSetting) it).key).append(((EditTextSetting) it).hint);
                    else if (it instanceof TextSetting) sb.append(((TextSetting) it).text).append(((TextSetting) it).subtext);
                    else if (it instanceof CustomSetting) sb.append(System.identityHashCode(((CustomSetting) it).factory));
                    sb.append('\n');
                }
            }
            return sb.toString();
        }

        void rebuild() {
            container.removeAllViews();
            Context c = ctx();
            List<SettingItem> items = sub != null ? sub : PluginsController.getInstance().getPluginSettingsList(pluginId);
            lastSignature = signature(items);
            if (sub != null) {
                TextSettingsCell back = new TextSettingsCell(c);
                back.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                back.setText("←", true);
                back.setOnClickListener(v -> {
                    sub = null;
                    subCallback = null;
                    rebuild();
                });
                add(back);
            }
            if (items == null || items.isEmpty()) {
                TextInfoPrivacyCell info = new TextInfoPrivacyCell(c);
                info.setText("Este plugin no tiene ajustes (o todavia se estan cargando).");
                add(info);
                return;
            }
            for (int i = 0; i < items.size(); i++) {
                try {
                    View v = createRow(c, items.get(i), i + 1 < items.size());
                    if (v != null) {
                        add(v);
                    }
                } catch (Throwable t) {
                    android.util.Log.e(TAG, "fila de ajustes FALLO", t);
                }
            }
        }

        View createRow(Context c, SettingItem item, boolean divider) {
            if (item instanceof HeaderSetting) {
                HeaderCell h = new HeaderCell(c);
                h.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                h.setText(((HeaderSetting) item).text);
                return h;
            }
            if (item instanceof DividerSetting) {
                String text = ((DividerSetting) item).text;
                if (text == null || text.isEmpty()) {
                    return new ShadowSectionCell(c);
                }
                TextInfoPrivacyCell info = new TextInfoPrivacyCell(c);
                info.setText(text);
                return info;
            }
            if (item instanceof SwitchSetting) {
                return switchRow(c, (SwitchSetting) item, divider);
            }
            if (item instanceof SelectorSetting) {
                return selectorRow(c, (SelectorSetting) item, divider);
            }
            if (item instanceof InputSetting) {
                InputSetting s = (InputSetting) item;
                return inputRow(c, s.key, s.text, s.subtext, s.defaultValue, s.onChangeCallback, false, divider);
            }
            if (item instanceof EditTextSetting) {
                EditTextSetting s = (EditTextSetting) item;
                return inputRow(c, s.key, s.hint, null, s.defaultValue, s.onChangeCallback, s.multiline, divider);
            }
            if (item instanceof TextSetting) {
                return textRow(c, (TextSetting) item, divider);
            }
            if (item instanceof CustomSetting) {
                return customRow(c, (CustomSetting) item);
            }
            return null;
        }

        View switchRow(Context c, final SwitchSetting s, boolean divider) {
            if (s.key == null || s.text == null) {
                return null;
            }
            final TextCheckCell cell = new TextCheckCell(c);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            boolean checked = PluginsController.getInstance().getPluginSettingBoolean(pluginId, s.key, s.defaultValue);
            if (s.subtext != null && !s.subtext.isEmpty()) {
                cell.setTextAndValueAndCheck(s.text, s.subtext, checked, true, divider);
            } else {
                cell.setTextAndCheck(s.text, checked, divider);
            }
            final PyObject onChange = s.onChangeCallback;
            cell.setOnClickListener(v -> {
                final boolean value = !cell.isChecked();
                cell.setChecked(value);
                save(s.key, value, onChange);
            });
            return cell;
        }

        View selectorRow(Context c, final SelectorSetting s, boolean divider) {
            if (s.key == null || s.text == null || s.items == null || s.items.length == 0) {
                return null;
            }
            final TextSettingsCell cell = new TextSettingsCell(c);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            int value = PluginsController.getInstance().getPluginSettingInt(pluginId, s.key, s.defaultValue);
            if (value < 0 || value >= s.items.length) {
                value = Math.max(0, Math.min(s.defaultValue, s.items.length - 1));
            }
            cell.setTextAndValue(s.text, s.items[value], divider);
            final PyObject onChange = s.onChangeCallback;
            cell.setOnClickListener(v -> {
                if (fragment.getParentActivity() == null) {
                    return;
                }
                AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
                b.setTitle(s.text);
                b.setItems(s.items, (dialog, which) -> {
                    cell.setTextAndValue(s.text, s.items[which], divider);
                    save(s.key, which, onChange);
                });
                fragment.showDialog(b.create());
            });
            return cell;
        }

        View inputRow(Context c, final String key, final String title, String subtext, String def, final PyObject onChange, final boolean multiline, boolean divider) {
            if (key == null || title == null) {
                return null;
            }
            final TextSettingsCell cell = new TextSettingsCell(c);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            String value = PluginsController.getInstance().getPluginSettingString(pluginId, key, def);
            cell.setTextAndValue(title, value == null ? "" : value, divider);
            final boolean dv = divider;
            cell.setOnClickListener(v -> {
                if (fragment.getParentActivity() == null) {
                    return;
                }
                final EditText edit = new EditText(fragment.getParentActivity());
                edit.setText(PluginsController.getInstance().getPluginSettingString(pluginId, key, def));
                edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                edit.setInputType(multiline ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE) : InputType.TYPE_CLASS_TEXT);
                AlertDialog.Builder b = new AlertDialog.Builder(fragment.getParentActivity());
                b.setTitle(title);
                b.setView(edit);
                b.setPositiveButton("OK", (dialog, which) -> {
                    String nv = edit.getText().toString();
                    cell.setTextAndValue(title, nv, dv);
                    save(key, nv, onChange);
                });
                b.setNegativeButton("Cancelar", null);
                fragment.showDialog(b.create());
            });
            return cell;
        }

        View textRow(Context c, final TextSetting s, boolean divider) {
            if (s.text == null) {
                return null;
            }
            final TextSettingsCell cell = new TextSettingsCell(c);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            if (s.subtext != null && !s.subtext.isEmpty()) {
                cell.setTextAndValue(s.text, s.subtext, divider);
            } else {
                cell.setText(s.text, divider);
            }
            if (s.red) {
                cell.setTextColor(Theme.getColor(Theme.key_text_RedBold));
            } else if (s.accent) {
                cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4));
            }
            cell.setOnClickListener(v -> {
                if (s.createSubFragmentCallback != null) {
                    openSub(s.createSubFragmentCallback);
                } else if (s.onClickCallback != null) {
                    try {
                        s.onClickCallback.call(v);
                    } catch (Throwable t) {
                        android.util.Log.e(TAG, "on_click FALLO", t);
                    }
                }
            });
            return cell;
        }

        View customRow(Context c, final CustomSetting s) {
            // 1) fabrica de Python (vista previa, deslizantes...): se crea y se enlaza la vista por reflexion
            if (s.factory != null) {
                View v = factoryView(c, s);
                if (v != null) {
                    return v;
                }
            }
            // 2) una vista ya armada
            if (s.item != null) {
                try {
                    java.lang.reflect.Field f = s.item.getClass().getField("view");
                    Object v = f.get(s.item);
                    if (v instanceof View) {
                        View view = (View) v;
                        if (view.getParent() instanceof ViewGroup) {
                            ((ViewGroup) view.getParent()).removeView(view);
                        }
                        return view;
                    }
                } catch (Throwable ignored) {
                }
            }
            // 3) sin vista pero con accion: se muestra como boton
            if (s.onClickCallback == null && s.createSubFragmentCallback == null) {
                return null;
            }
            final TextSettingsCell cell = new TextSettingsCell(c);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cell.setText("(personalizado)", true);
            cell.setOnClickListener(v -> {
                if (s.createSubFragmentCallback != null) {
                    openSub(s.createSubFragmentCallback);
                } else {
                    try {
                        s.onClickCallback.call(v);
                    } catch (Throwable t) {
                        android.util.Log.e(TAG, "on_click custom FALLO", t);
                    }
                }
            });
            return cell;
        }

        View factoryView(Context c, CustomSetting s) {
            try {
                Object f = s.factory;
                java.lang.reflect.Method create = null;
                java.lang.reflect.Method bind = null;
                for (java.lang.reflect.Method m : f.getClass().getMethods()) {
                    if (m.isBridge() || m.getParameterTypes().length != 5) {
                        continue;
                    }
                    if (m.getName().equals("createView") && create == null) {
                        create = m;
                    } else if (m.getName().equals("bindView") && bind == null) {
                        bind = m;
                    }
                }
                if (create == null) {
                    android.util.Log.e(TAG, "factoria sin createView");
                    return null;
                }
                int account = fragment.getCurrentAccount();
                Object view = create.invoke(f, c, null, account, 0, null);
                if (!(view instanceof View)) {
                    return null;
                }
                Object uitem = null;
                try {
                    Plugin plugin = PluginsController.getInstance().getPlugins().get(pluginId);
                    uitem = s.factory.create(plugin, s, s.factoryArgs);
                } catch (Throwable ignored) {
                }
                if (bind != null) {
                    bind.invoke(f, view, uitem, false, null, null);
                }
                return (View) view;
            } catch (Throwable t) {
                Throwable cause = t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null ? t.getCause() : t;
                android.util.Log.e(TAG, "fila personalizada FALLO", cause);
                return null;
            }
        }

        void openSub(final PyObject callback) {
            PluginsController.runOnPluginsQueue(new Runnable() {
                @Override
                public void run() {
                    try {
                        PyObject result = callback.call(new Object[0]);
                        if (result == null) {
                            return;
                        }
                        final List<SettingItem> items = PythonPluginsEngine.INSTANCE.parsePySettingDefinitions(result.asList());
                        if (items == null || items.isEmpty()) {
                            return;
                        }
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                sub = items;
                                subCallback = callback;
                                rebuild();
                            }
                        });
                    } catch (Throwable t) {
                        android.util.Log.e(TAG, "create_sub_fragment FALLO", t);
                    }
                }
            });
        }

        void save(final String key, final Object value, final PyObject onChange) {
            PluginsController.runOnPluginsQueue(new Runnable() {
                @Override
                public void run() {
                    try {
                        PluginsController.getInstance().setPluginSetting(pluginId, key, value);
                        if (onChange != null) {
                            onChange.call(value);
                        }
                    } catch (Throwable t) {
                        android.util.Log.e(TAG, "guardar ajuste " + key + " FALLO", t);
                    }
                }
            });
        }
    }
}
