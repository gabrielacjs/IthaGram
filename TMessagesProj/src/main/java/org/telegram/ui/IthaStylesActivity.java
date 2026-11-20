package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.IthaBubbles;
import org.telegram.messenger.IthaStyles;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.DownloadQueueManager.tr;

/**
 * IthaGram: pantalla "Estilos". Cada pieza de la app (burbujas, encabezado, fijado, barra de mensaje)
 * tiene su propio selector, y arriba hay un atajo para cambiar todas a la vez.
 */
public class IthaStylesActivity extends BaseFragment {

    private LinearLayout listLayout;
    private Context ctx;

    @Override
    public android.view.View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        IthaMd3.centerTitle(actionBar, context, tr("Estilos", "Styles"));
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

    private String[] names() {
        return new String[]{tr("Actual de Telegram", "Telegram default"), tr("Material 3", "Material 3")};
    }

    private void rebuild() {
        listLayout.removeAllViews();
        final Context context = ctx;
        final String[] names = names();

        final int h = IthaStyles.chatHeader();
        final int p = IthaStyles.pinned();
        final int i = IthaStyles.input();
        final int b = IthaBubbles.style() == 2 ? 1 : 0;

        section(context, tr("Todo a la vez", "Everything at once"));
        final String all = (h == p && p == i && i == b) ? names[h] : tr("Personalizado", "Custom");
        pick(context, tr("Estilo general", "Overall style"), all, tr("Estilo general", "Overall style"), names, which -> {
            IthaStyles.setChatHeader(which);
            IthaStyles.setPinned(which);
            IthaStyles.setInput(which);
            IthaBubbles.set(which == 1 ? 2 : 0);
        });

        section(context, tr("Cada pieza por separado", "Each part on its own"));
        pick(context, tr("Burbujas de mensajes", "Message bubbles"), names[b], tr("Burbujas de mensajes", "Message bubbles"), new String[]{names[0], tr("Material 3 (sin cola)", "Material 3 (no tail)")}, which -> IthaBubbles.set(which == 1 ? 2 : 0));
        pick(context, tr("Encabezado del chat", "Chat header"), names[h], tr("Encabezado del chat", "Chat header"), names, IthaStyles::setChatHeader);
        pick(context, tr("Mensaje fijado", "Pinned message"), names[p], tr("Mensaje fijado", "Pinned message"), names, IthaStyles::setPinned);
        pick(context, tr("Barra de mensaje", "Message bar"), names[i], tr("Barra de mensaje", "Message bar"), names, IthaStyles::setInput);

        TextView info = new TextView(context);
        info.setText(tr("Los cambios se ven al abrir un chat. Las opciones del encabezado (avatar, título centrado, llamada) están en Ajustes de chat y valen para cualquier estilo.",
                "Changes show when you open a chat. Header options (avatar, centered title, call) are in Chat settings and work with any style."));
        info.setTextSize(13);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setPadding(dp(20), dp(14), dp(20), dp(10));
        listLayout.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private interface IntCallback {
        void run(int which);
    }

    private void pick(Context context, String title, String current, String dialogTitle, String[] options, IntCallback callback) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        IthaMd3.style(row);
        row.setPadding(dp(20), dp(12), dp(20), dp(12));
        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(16);
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        row.addView(t, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        TextView v = new TextView(context);
        v.setText(current);
        v.setTextSize(13);
        v.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        row.addView(v, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        row.setOnClickListener(view -> {
            if (getParentActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(dialogTitle);
            builder.setItems(options, (d, which) -> {
                callback.run(which);
                rebuild();
            });
            builder.setNegativeButton(tr("Cancelar", "Cancel"), null);
            showDialog(builder.create());
        });
        IthaMd3.add(listLayout, row);
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
}
