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
    /** Puente hacia el motor de Python (lo instala ithabus.py al iniciar). */
    public interface Bus {
        Object sendMessage(int account, Object params);

        boolean updates(int account, Object updates);

        void appEvent(String event);
    }

    public static volatile Bus bus;
    public static volatile boolean sendActive;
    public static volatile boolean updatesActive;
    /** Puente para menus de plugins (lo instala ithabus.py). */
    public interface MenuBus {
        void fill(String menuType, Object fragment, Object message, java.util.ArrayList<Integer> icons, java.util.ArrayList<CharSequence> items, java.util.ArrayList<Integer> options);

        boolean click(String menuType, Object fragment, int option, Object message);
    }

    public static volatile MenuBus menuBus;
    public static volatile boolean menuActive;

    public static void addMessageMenu(Object fragment, MessageObject message, java.util.ArrayList<Integer> icons, java.util.ArrayList<CharSequence> items, java.util.ArrayList<Integer> options) {
        MenuBus b = menuBus;
        if (b == null || !menuActive) {
            return;
        }
        try {
            b.fill("message_context_menu", fragment, message, icons, items, options);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    /** true = el item era de un plugin y ya se ejecuto. */
    public static boolean menuClick(Object fragment, int option, MessageObject message) {
        MenuBus b = menuBus;
        if (b == null || option < 100000) {
            return false;
        }
        try {
            return b.click("message_context_menu", fragment, option, message);
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    /** null = el plugin cancelo el envio. */
    public static SendMessagesHelper.SendMessageParams sendMessageHook(int account, SendMessagesHelper.SendMessageParams params) {
        Bus b = bus;
        if (b == null || !sendActive) {
            return params;
        }
        try {
            return (SendMessagesHelper.SendMessageParams) b.sendMessage(account, params);
        } catch (Throwable t) {
            FileLog.e(t);
            return params;
        }
    }

    /** false = un plugin descarto este paquete de updates. */
    public static boolean updatesHook(int account, org.telegram.tgnet.TLRPC.Updates updates) {
        Bus b = bus;
        if (b == null || !updatesActive) {
            return true;
        }
        try {
            return b.updates(account, updates);
        } catch (Throwable t) {
            FileLog.e(t);
            return true;
        }
    }

    public static void appEvent(String event) {
        Bus b = bus;
        if (b == null) {
            return;
        }
        try {
            b.appEvent(event);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static boolean isSupported(String method) {
        return "onResume".equals(method) || "onActivityResult".equals(method)
                || "sendMessage".equals(method) || "sendMessageInternal".equals(method);
    }

    /** Aviso simple (antes de ejecutar el metodo): lo llama Telegram en el sitio enganchado. */
    public static void call(Object thiz, String method) {
        if (entries.isEmpty() || thiz == null) {
            return;
        }
        Param p = new Param();
        p.thisObject = thiz;
        p.args = new Object[0];
        runBefore(thiz, method, p);
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
