package com.exteragram.messenger.plugins;

import org.telegram.messenger.DispatchQueue;

/** IthaGram: ids de eventos y cola propios del motor (no tocan NotificationCenter ni Utilities de Telegram). */
public class PluginEvents {
    public static final int pluginsUpdated = 91000001;
    public static final int pluginMenuItemsUpdated = 91000002;
    public static final int pluginSettingsRegistered = 91000003;
    public static final int pluginSettingsUnregistered = 91000004;
    public static final int pluginIsNotResponding = 91000005;
    public static final int pluginsPySdkInfoChanged = 91000006;

    public static volatile DispatchQueue pluginsQueue;
}
