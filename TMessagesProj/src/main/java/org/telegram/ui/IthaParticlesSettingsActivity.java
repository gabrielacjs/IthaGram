package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.IthaConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.DownloadQueueManager.tr;

/**
 * IthaGram: ajustes de las particulas del fondo del chat (nieve, sakura, lluvia, hojas, matriz).
 */
public class IthaParticlesSettingsActivity extends BaseFragment {

    private interface OnPick {
        void pick(int index);
    }

    private LinearLayout listLayout;
    private Context ctx;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(tr("Partículas", "Particles"));
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
        listLayout = new LinearLayout(context);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listLayout, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        ctx = context;
        rebuild();
        return fragmentView;
    }

    private String[] typeNames() {
        return new String[]{
                tr("Apagado", "Off"),
                tr("Nieve", "Snow"),
                tr("Pétalos de sakura", "Sakura petals"),
                tr("Lluvia", "Rain"),
                tr("Hojas de otoño", "Autumn leaves"),
                tr("Matriz", "Matrix")
        };
    }

    private static final int[] COUNTS = {30, 80, 150, 250};
    private static final int[] SPEEDS = {60, 100, 160};
    private static final int[] OPACITIES = {30, 50, 80, 100};

    private String valueLabel(int value, int[] values, String[] labels) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return labels[i];
            }
        }
        return String.valueOf(value);
    }

    private void rebuild() {
        listLayout.removeAllViews();
        final Context context = ctx;

        final String[] types = typeNames();
        final String[] countNames = {tr("Pocas", "Few"), tr("Normal", "Normal"), tr("Muchas", "Many"), tr("Muchísimas", "Lots")};
        final String[] speedNames = {tr("Lenta", "Slow"), tr("Normal", "Normal"), tr("Rápida", "Fast")};
        final String[] opacityNames = {"30%", "50%", "80%", "100%"};

        section(context, tr("Fondo del chat", "Chat background"));
        row(context, tr("Efecto", "Effect"), types[IthaConfig.getParticleType()],
                () -> choose(tr("Efecto", "Effect"), types, i -> IthaConfig.setParticleType(i)));
        row(context, tr("Cantidad", "Amount"), valueLabel(IthaConfig.getParticleCount(), COUNTS, countNames),
                () -> choose(tr("Cantidad", "Amount"), countNames, i -> IthaConfig.setParticleCount(COUNTS[i])));
        row(context, tr("Velocidad", "Speed"), valueLabel(IthaConfig.getParticleSpeed(), SPEEDS, speedNames),
                () -> choose(tr("Velocidad", "Speed"), speedNames, i -> IthaConfig.setParticleSpeed(SPEEDS[i])));
        row(context, tr("Opacidad", "Opacity"), valueLabel(IthaConfig.getParticleOpacity(), OPACITIES, opacityNames),
                () -> choose(tr("Opacidad", "Opacity"), opacityNames, i -> IthaConfig.setParticleOpacity(OPACITIES[i])));

        note(context, tr("Las partículas se dibujan sobre el fondo de los chats, sin importar la fecha. Se apagan si desactivas las animaciones del fondo del chat en el ahorro de energía de Telegram.",
                "Particles are drawn over the chat background, regardless of the date. They stop if chat background animations are disabled in Telegram's power saving settings."));
    }

    private void choose(String title, String[] names, OnPick callback) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);
        builder.setItems(names, (dialog, which) -> {
            callback.pick(which);
            rebuild();
        });
        showDialog(builder.create());
    }

    private void note(Context context, String text) {
        TextView info = new TextView(context);
        info.setText(text);
        info.setTextSize(14);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        info.setPadding(dp(20), dp(12), dp(20), dp(12));
        listLayout.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void section(Context context, String title) {
        TextView header = new TextView(context);
        header.setText(title);
        header.setTextSize(14);
        header.setTypeface(AndroidUtilities.bold());
        header.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        header.setPadding(dp(20), dp(18), dp(20), dp(8));
        listLayout.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void row(Context context, String title, String subtitle, Runnable action) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        row.setPadding(dp(20), dp(12), dp(20), dp(12));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        row.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (subtitle != null) {
            TextView subtitleView = new TextView(context);
            subtitleView.setText(subtitle);
            subtitleView.setTextSize(13);
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            row.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        }

        row.setOnClickListener(v -> action.run());
        listLayout.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }
}
