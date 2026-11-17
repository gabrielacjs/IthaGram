package com.exteragram.messenger.plugins.ui.components;

import org.telegram.ui.ActionBar.BaseFragment;

import java.io.File;

/** IthaGram: stub. */
public class PluginFileViewer {
    private static final PluginFileViewer INSTANCE = new PluginFileViewer();

    public static PluginFileViewer getInstance() {
        return INSTANCE;
    }

    public void open(BaseFragment fragment, File file, String name) {
    }
}
