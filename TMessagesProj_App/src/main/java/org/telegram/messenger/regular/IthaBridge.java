package org.telegram.messenger.regular;

import android.util.Log;

import com.exteragram.messenger.plugins.PluginsConstants;
import com.exteragram.messenger.plugins.PluginsController;
import com.exteragram.messenger.plugins.hooks.MenuItemRecord;
import com.exteragram.messenger.plugins.utils.MenuContextBuilder;

import org.telegram.messenger.IthaHooks;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// IthaGram: conecta los puntos de enganche de Telegram (IthaHooks) con el motor de plugins de Nagram.
public class IthaBridge {
    private static final String TAG = "IthaDiag";
    private static final int FIRST_ID = 100000;

    private static final ConcurrentHashMap<String, Integer> idByKey = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, MenuItemRecord> recordById = new ConcurrentHashMap<>();
    private static int nextId = FIRST_ID;

    public static void install() {
        org.telegram.messenger.IthaPlugins.setHost(new IthaPyHost());
        IthaHooks.menuBus = new IthaHooks.MenuBus() {
            @Override
            public void fill(String menuType, Object fragment, Object message, ArrayList<Integer> icons, ArrayList<CharSequence> items, ArrayList<Integer> options) {
                try {
                    Map<String, Object> ctx = buildContext(menuType, fragment, message);
                    List<MenuItemRecord> list = PluginsController.getInstance().getMenuItemsForLocation(menuType, ctx);
                    Log.i(TAG, "menu " + menuType + ": items=" + list.size() + " ctx=" + ctx.keySet() + " plugins=" + describePlugins());
                    for (MenuItemRecord r : list) {
                        icons.add(r.iconResId != 0 ? r.iconResId : R.drawable.msg_info);
                        items.add(r.text);
                        options.add(idFor(r));
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "menuBus.fill FALLO " + menuType, t);
                }
            }

            @Override
            public boolean click(String menuType, Object fragment, int option, Object message) {
                MenuItemRecord r = recordById.get(option);
                Log.i(TAG, "menu click " + menuType + " option=" + option + " encontrado=" + (r != null));
                if (r == null) {
                    return false;
                }
                try {
                    r.executeClick(buildContext(menuType, fragment, message));
                } catch (Throwable t) {
                    Log.e(TAG, "menuBus.click FALLO " + menuType, t);
                }
                return true;
            }
        };
        IthaHooks.menuActive = true;

        IthaHooks.bus = new IthaHooks.Bus() {
            @Override
            public Object sendMessage(int account, Object params) {
                return PluginsController.getInstance().executeSendMessageHook(account, (SendMessagesHelper.SendMessageParams) params);
            }

            @Override
            public boolean updates(int account, Object updates) {
                TLRPC.Updates u = (TLRPC.Updates) updates;
                return PluginsController.getInstance().executeUpdatesHook(u.getClass().getSimpleName(), account, u) != null;
            }

            @Override
            public void appEvent(String event) {
                PluginsController.getInstance().executeOnAppEvent(event);
            }
        };
        IthaHooks.sendActive = true;
        IthaHooks.updatesActive = true;
        Log.i(TAG, "motor Nagram: enganches conectados " + describePlugins());
    }

    private static String describePlugins() {
        StringBuilder sb = new StringBuilder();
        try {
            for (com.exteragram.messenger.plugins.Plugin p : PluginsController.getInstance().getPlugins().values()) {
                sb.append('[').append(p.getId()).append(" enabled=").append(p.isEnabled()).append(" error=").append(p.hasError() ? p.getError() : "no").append(']');
            }
        } catch (Throwable t) {
            sb.append("?").append(t);
        }
        return sb.toString();
    }

    private static synchronized int idFor(MenuItemRecord r) {
        String key = r.pluginId + "/" + r.itemId;
        Integer id = idByKey.get(key);
        if (id == null) {
            id = nextId++;
            idByKey.put(key, id);
        }
        recordById.put(id, r);
        return id;
    }

    private static Map<String, Object> buildContext(String menuType, Object fragment, Object message) {
        MenuContextBuilder b = fragment instanceof BaseFragment ? MenuContextBuilder.from((BaseFragment) fragment) : MenuContextBuilder.create();
        long dialogId = 0;
        MessageObject msg = message instanceof MessageObject ? (MessageObject) message : null;
        if (msg != null) {
            b.withMessage(msg);
            dialogId = msg.getDialogId();
        } else if (fragment instanceof org.telegram.ui.ChatActivity) {
            dialogId = ((org.telegram.ui.ChatActivity) fragment).getDialogId();
        } else if (fragment instanceof BaseFragment) {
            android.os.Bundle args = ((BaseFragment) fragment).getArguments();
            if (args != null) {
                long uid = args.getLong("user_id", 0);
                long cid = args.getLong("chat_id", 0);
                long did = args.getLong("dialog_id", 0);
                dialogId = uid != 0 ? uid : (cid != 0 ? -cid : did);
            }
        }
        if (dialogId != 0) {
            b.withDialogId(dialogId);
            try {
                int account = fragment instanceof BaseFragment ? ((BaseFragment) fragment).getCurrentAccount() : org.telegram.messenger.UserConfig.selectedAccount;
                MessagesController mc = MessagesController.getInstance(account);
                if (dialogId > 0) {
                    b.withUser(mc.getUser(dialogId));
                } else {
                    b.withChat(mc.getChat(-dialogId));
                }
            } catch (Throwable ignored) {
            }
        }
        Map<String, Object> map = new HashMap<>(b.build());
        map.put("menu_type", menuType);
        return map;
    }
}
