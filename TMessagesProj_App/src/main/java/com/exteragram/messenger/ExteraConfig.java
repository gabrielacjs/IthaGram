package com.exteragram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** IthaGram: version minima de la config de exteraGram (solo lo que usa el motor de plugins). */
public class ExteraConfig {
    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;
    public static boolean pluginsEngine;
    public static boolean pluginsSafeMode;
    public static boolean pluginsDevMode;
    public static boolean pluginsDisableArtOpts;
    public static boolean pluginsPySdkAutoUpdate;
    public static boolean pluginsPySdkBetaVersions;
    public static long sdkUpdateScheduleTimestamp;
    public static Set<String> pinnedPlugins = Collections.emptySet();

    private static boolean loaded;

    static {
        ensureLoaded();
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("itha_plugins_engine", Context.MODE_PRIVATE);
        editor = preferences.edit();
        pluginsEngine = preferences.getBoolean("pluginsEngine", false);
        pluginsSafeMode = preferences.getBoolean("pluginsSafeMode", false);
        pluginsDevMode = preferences.getBoolean("pluginsDevMode", false);
        pluginsDisableArtOpts = preferences.getBoolean("pluginsDisableArtOpts", false);
        pluginsPySdkAutoUpdate = false;
        pluginsPySdkBetaVersions = preferences.getBoolean("pluginsPySdkBetaVersions", false);
        sdkUpdateScheduleTimestamp = preferences.getLong("sdkUpdateScheduleTimestamp", 0L);
        pinnedPlugins = new HashSet<>(preferences.getStringSet("pinnedPlugins", Collections.emptySet()));
    }
}
