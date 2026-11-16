package org.telegram.messenger;

import android.app.Activity;
import android.content.Intent;

import org.telegram.ui.ActionBar.BaseFragment;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * IthaGram: puntos de enganche para plugins. No usa hooks nativos: Telegram llama a este
 * registro en sitios concretos y aqui se avisa a los plugins que se registraron.
 */
public class IthaHooks {

    public static class Param {
        public Object thisObject;
        public Object[] args;
        private Object result;
        private boolean returnEarly;

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
            this.returnEarly = true;
        }

        public boolean isReturnEarly() {
            return returnEarly;
        }
    }

    public interface Callback {
        void before(Param param);

        void after(Param param);
    }

    private static final class Entry {
        final Class<?> cls;
        final String method;
        final Callback callback;

        Entry(Class<?> cls, String method, Callback callback) {
            this.cls = cls;
            this.method = method;
            this.callback = callback;
        }
    }

    private static final CopyOnWriteArrayList<Entry> entries = new CopyOnWriteArrayList<>();

    public static boolean isSupported(String method) {
        return "onResume".equals(method) || "onActivityResult".equals(method);
    }

    /** Devuelve un manejador para desregistrar, o null si ese metodo no se puede enganchar. */
    public static Object register(Class<?> cls, String method, Callback callback) {
        if (cls == null || callback == null || !isSupported(method)) {
            return null;
        }
        Entry e = new Entry(cls, method, callback);
        entries.add(e);
        android.util.Log.i("IthaHooks", "registrado " + method + " en " + cls.getName());
        return e;
    }

    public static void unregister(Object handle) {
        if (handle instanceof Entry) {
            entries.remove(handle);
        }
    }

    private static boolean matches(Entry e, String method, Object thiz) {
        return e.method.equals(method) && e.cls.isInstance(thiz);
    }

    private static boolean runBefore(Object thiz, String method, Param p) {
        for (Entry e : entries) {
            if (!matches(e, method, thiz)) {
                continue;
            }
            android.util.Log.i("IthaHooks", "before " + method + " en " + thiz.getClass().getSimpleName());
            try {
                e.callback.before(p);
            } catch (Throwable t) {
                FileLog.e(t);
            }
            if (p.returnEarly) {
                return true;
            }
        }
        return false;
    }

    private static void runAfter(Object thiz, String method, Param p) {
        for (Entry e : entries) {
            if (!matches(e, method, thiz)) {
                continue;
            }
            android.util.Log.i("IthaHooks", "after " + method + " en " + thiz.getClass().getSimpleName());
            try {
                e.callback.after(p);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    /** Sustituye a fragment.onResume() en ActionBarLayout. */
    public static void resume(BaseFragment fragment) {
        if (entries.isEmpty()) {
            fragment.onResume();
            return;
        }
        Param p = new Param();
        p.thisObject = fragment;
        p.args = new Object[0];
        if (!runBefore(fragment, "onResume", p)) {
            fragment.onResume();
        }
        runAfter(fragment, "onResume", p);
    }

    /** Se llama al inicio de LaunchActivity.onActivityResult; true = el plugin lo consumio. */
    public static boolean beforeActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (entries.isEmpty()) {
            return false;
        }
        Param p = new Param();
        p.thisObject = activity;
        p.args = new Object[]{requestCode, resultCode, data};
        return runBefore(activity, "onActivityResult", p);
    }
}
