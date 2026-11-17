package com.exteragram.messenger.plugins.ui;

import com.chaquo.python.PyObject;
import com.exteragram.messenger.plugins.Plugin;
import com.exteragram.messenger.plugins.models.SettingItem;

import org.telegram.ui.ActionBar.BaseFragment;

import java.util.List;

/** IthaGram: stub, la pantalla real llega en una etapa posterior. */
public class PluginSettingsActivity extends BaseFragment {
    public PluginSettingsActivity(Plugin plugin) {
    }

    public PluginSettingsActivity(Plugin plugin, String targetSetting) {
    }

    public PluginSettingsActivity(Plugin plugin, String title, List<SettingItem> items, PyObject createSubFragmentCallback) {
    }

    public PluginSettingsActivity(Plugin plugin, String title, List<SettingItem> items, PyObject createSubFragmentCallback, String targetSetting) {
    }

    public PluginSettingsActivity setSettingsLinkPrefix(String prefix) {
        return this;
    }

    public void checkTargetSetting() {
    }
}
