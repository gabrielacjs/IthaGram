package org.telegram.messenger.regular;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import org.json.JSONArray;
import org.json.JSONObject;
import android.view.ViewGroup;

import org.telegram.messenger.IthaPlugins;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.ArrayList;
import java.util.List;

// IthaGram: implementa el puente de plugins usando el motor de Python.
public class IthaPyHost implements IthaPlugins.Host {

    private PyObject engine() {
        return Python.getInstance().getModule("ithaengine");
    }

    private static String result(PyObject o) {
        if (o == null) {
            return null;
        }
        String s = o.toString();
        return s == null || s.isEmpty() ? null : s;
    }

    @Override
    public List<IthaPlugins.Info> list() {
        List<IthaPlugins.Info> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(engine().callAttr("list_plugins_json").toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                IthaPlugins.Info p = new IthaPlugins.Info();
                p.id = o.optString("id");
                p.name = o.optString("name");
                p.version = o.optString("version");
                p.author = o.optString("author");
                p.description = o.optString("description");
                p.enabled = o.optBoolean("enabled", true);
                p.loaded = o.optBoolean("loaded", false);
                p.hasSettings = o.optBoolean("has_settings", false);
                out.add(p);
            }
        } catch (Throwable t) {
            android.util.Log.e("IthaPy", "list FALLO", t);
        }
        return out;
    }

    @Override
    public String setEnabled(String id, boolean enabled) {
        try {
            return result(engine().callAttr("set_enabled", id, enabled));
        } catch (Throwable t) {
            return String.valueOf(t.getMessage());
        }
    }

    @Override
    public String importPlugin(String path) {
        try {
            return result(engine().callAttr("import_plugin", path));
        } catch (Throwable t) {
            return String.valueOf(t.getMessage());
        }
    }

    @Override
    public String remove(String id) {
        try {
            return result(engine().callAttr("remove_plugin", id));
        } catch (Throwable t) {
            return String.valueOf(t.getMessage());
        }
    }

    @Override
    public boolean isMasterEnabled() {
        try {
            return engine().callAttr("is_master").toBoolean();
        } catch (Throwable t) {
            return true;
        }
    }

    @Override
    public String setMasterEnabled(boolean enabled) {
        try {
            return result(engine().callAttr("set_master", enabled));
        } catch (Throwable t) {
            return String.valueOf(t.getMessage());
        }
    }

    @Override
    public void fillSettings(String id, ViewGroup container, BaseFragment fragment) {
        try {
            engine().callAttr("fill_settings", id, container, fragment);
        } catch (Throwable t) {
            android.util.Log.e("IthaPy", "fillSettings FALLO", t);
        }
    }

    @Override
    public void closeSettings(String id) {
        try {
            engine().callAttr("close_settings", id);
        } catch (Throwable t) {
            android.util.Log.e("IthaPy", "closeSettings FALLO", t);
        }
    }
}
