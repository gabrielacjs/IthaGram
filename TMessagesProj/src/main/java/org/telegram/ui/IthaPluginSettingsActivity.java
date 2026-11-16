package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.IthaPlugins;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * IthaGram: ajustes de un plugin. Las filas las arma el motor de Python (ithasettings.py).
 */
public class IthaPluginSettingsActivity extends BaseFragment {

    private final String pluginId;
    private final String pluginName;

    public IthaPluginSettingsActivity(String pluginId, String pluginName) {
        this.pluginId = pluginId;
        this.pluginName = pluginName;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(pluginName);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        ScrollView scroll = new ScrollView(context);
        LinearLayout listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listLayout, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        IthaPlugins.Host host = IthaPlugins.getHost();
        if (host != null) {
            host.fillSettings(pluginId, listLayout, this);
        }
        return fragmentView;
    }

    @Override
    public void onFragmentDestroy() {
        IthaPlugins.Host host = IthaPlugins.getHost();
        if (host != null) {
            host.closeSettings(pluginId);
        }
        super.onFragmentDestroy();
    }
}
