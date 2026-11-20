package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.DownloadQueueManager.tr;

/**
 * IthaGram: pantalla "Itha Ajustes". Aqui se van sumando las funciones propias de IthaGram.
 * Para agregar una opcion nueva: usar section(...) para un titulo y row(...) para una fila.
 */
public class IthaSettingsActivity extends BaseFragment {

    private LinearLayout listLayout;
    private Context ctx;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("");
        // titulo centrado en la barra de arriba
        TextView centerTitle = new TextView(context);
        centerTitle.setText("Itha Ajustes");
        centerTitle.setTextSize(20);
        centerTitle.setTypeface(AndroidUtilities.bold());
        centerTitle.setSingleLine(true);
        centerTitle.setGravity(Gravity.CENTER);
        centerTitle.setTextColor(Theme.getColor(Theme.key_actionBarDefaultTitle));
        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dp(56), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        titleLp.topMargin = actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;
        actionBar.addView(centerTitle, titleLp);
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

    /** Itha Ajustes y sus pantallas usan siempre el estilo Material 3. */
    public static boolean isMd3() {
        return true;
    }

    private void rebuild() {
        listLayout.removeAllViews();
        listLayout.setPadding(0, 0, 0, dp(16));
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        buildMd3(ctx);
    }

    private void buildMd3(Context context) {
        final int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader);
        int cardBase = Theme.getColor(Theme.key_windowBackgroundWhite);
        final int card = cardBase == Theme.getColor(Theme.key_windowBackgroundGray)
                ? androidx.core.graphics.ColorUtils.blendARGB(cardBase, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 0.07f)
                : cardBase;
        final int tint = (accent & 0x00FFFFFF) | 0x33000000;

        TextView sub = new TextView(context);
        sub.setText(tr("Todo lo que hace único a IthaGram", "Everything that makes IthaGram unique"));
        sub.setTextSize(14);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        sub.setPadding(dp(20), dp(12), dp(20), dp(16));
        listLayout.addView(sub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout group = md3Card(context, card);
        md3Row(context, group, R.drawable.msg_discussion, tint, accent,
                tr("Ajustes lista de chat", "Chat list settings"),
                tr("Título, alineación y barra de búsqueda", "Title, alignment and search bar"),
                () -> presentFragment(new IthaChatListSettingsActivity()));
        md3Row(context, group, R.drawable.msg_contacts, tint, accent,
                tr("Ajustes de perfil", "Profile settings"),
                tr("Número de teléfono", "Phone number"),
                () -> presentFragment(new IthaProfileSettingsActivity()));
        md3Row(context, group, R.drawable.msg_customize, tint, accent,
                tr("Ajustes de chat", "Chat settings"),
                tr("Encabezado dentro de los chats", "Header inside chats"),
                () -> presentFragment(new IthaChatSettingsActivity()));
        md3Row(context, group, R.drawable.msg_bots, tint, accent,
                tr("Plugins", "Plugins"),
                tr("Extensiones .plugin (Python)", "Python .plugin extensions"),
                () -> presentFragment(new IthaPluginsActivity()));

        TextView info = new TextView(context);
        info.setText(tr("IthaGram es una app no oficial que usa la API de Telegram. No está afiliada a Telegram.",
                "IthaGram is an unofficial app that uses the Telegram API. It is not affiliated with Telegram."));
        info.setTextSize(13);
        info.setGravity(Gravity.CENTER);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setPadding(dp(32), dp(24), dp(32), dp(8));
        listLayout.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private LinearLayout md3Card(Context context, int color) {
        LinearLayout group = new LinearLayout(context);
        group.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(28));
        group.setBackground(bg);
        group.setPadding(0, dp(4), 0, dp(4));
        listLayout.addView(group, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 8, 0, 8, 0));
        return group;
    }

    private void md3Row(Context context, LinearLayout group, int iconRes, int tint, int accent, String title, String subtitle, Runnable action) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));

        FrameLayout circle = new FrameLayout(context);
        GradientDrawable cbg = new GradientDrawable();
        cbg.setShape(GradientDrawable.OVAL);
        cbg.setColor(tint);
        circle.setBackground(cbg);
        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        circle.addView(icon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));
        row.addView(circle, LayoutHelper.createLinear(44, 44, 0, 0, 16, 0));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(16);
        t.setTypeface(AndroidUtilities.bold());
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        texts.addView(t, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        TextView st = new TextView(context);
        st.setText(subtitle);
        st.setTextSize(14);
        st.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        texts.addView(st, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        row.addView(texts, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        row.setOnClickListener(v -> action.run());
        group.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
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
