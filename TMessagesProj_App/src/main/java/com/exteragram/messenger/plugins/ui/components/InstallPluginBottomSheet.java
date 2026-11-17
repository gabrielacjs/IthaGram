package com.exteragram.messenger.plugins.ui.components;

import com.exteragram.messenger.plugins.PluginsController;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.BaseFragment;

import tw.nekomimi.nekogram.helpers.MessageHelper;

/** IthaGram: stub (sin hoja de instalacion todavia). */
public class InstallPluginBottomSheet {
    public static class PluginInstallParams {
        public final String filePath;
        public final boolean trusted;

        public PluginInstallParams(String filePath, boolean trusted) {
            this.filePath = filePath;
            this.trusted = trusted;
        }

        public static PluginInstallParams of(MessageObject messageObject) {
            return new PluginInstallParams(MessageHelper.getPathToMessage(messageObject), false);
        }
    }

    public InstallPluginBottomSheet(BaseFragment fragment, PluginsController.PluginValidationResult result, PluginInstallParams params) {
    }

    public void show() {
    }
}
