package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.DownloadQueueManager.tr;

/**
 * IthaGram: "Ajustes lista de chat". Opciones que cambian la lista de chats (titulo, busqueda).
 */
public class IthaChatListSettingsActivity extends BaseFragment {

    private LinearLayout listLayout;
    private Context ctx;
    private PreviewView preview;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        IthaMd3.centerTitle(actionBar, context, tr("Ajustes lista de chat", "Chat list settings"));
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

    private void rebuild() {
        listLayout.removeAllViews();
        final Context context = ctx;

        preview = new PreviewView(context);
        listLayout.addView(preview, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 170, 16, 16, 16, 4));

        section(context, tr("Título", "Title"));
        row(context, tr("Texto del título", "Title text"),
                IthaConfig.getTitleText() + (IthaConfig.getTitleTextRaw().trim().isEmpty() ? " (" + tr("por defecto", "default") + ")" : ""),
                this::askTitle);
        check(context, tr("Título centrado", "Centered title"), IthaConfig.isTitleCentered(), (value) -> {
            IthaConfig.setTitleCentered(value);
            applyLive();
        });

        section(context, tr("Búsqueda", "Search"));
        check(context, tr("Mostrar barra de búsqueda", "Show search bar"), !IthaConfig.isSearchBarHidden(), (value) -> {
            IthaConfig.setSearchBarHidden(!value);
            applyLive();
            askRestart();
        });

        TextView note = new TextView(context);
        note.setText(tr("El título y el centrado cambian al instante. La barra de búsqueda necesita reiniciar la app.", "Title and centering change instantly. The search bar needs an app restart."));
        note.setTextSize(13);
        note.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        note.setPadding(dp(20), dp(10), dp(20), dp(10));
        listLayout.addView(note, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void askTitle() {
        final android.widget.EditText edit = new android.widget.EditText(getParentActivity());
        edit.setText(IthaConfig.getTitleTextRaw());
        edit.setHint(IthaConfig.DEFAULT_TITLE);
        edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        edit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.getColor(Theme.key_dialogTextGray2)));
        edit.setSingleLine(true);
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(tr("Texto del título", "Title text"));
        b.setView(edit);
        b.setPositiveButton(tr("Guardar", "Save"), (d, w) -> {
            IthaConfig.setTitleText(edit.getText().toString());
            applyLive();
            rebuild();
        });
        b.setNegativeButton(tr("Cancelar", "Cancel"), null);
        showDialog(b.create());
    }

    /** Aplica los cambios a la lista de chats que ya esta abierta (sin reiniciar). */
    private void applyLive() {
        if (preview != null) {
            preview.invalidate();
        }
        try {
            DialogsActivity dialogs = IthaConfig.dialogsRef != null ? IthaConfig.dialogsRef.get() : null;
            if (dialogs != null) {
                dialogs.ithaRefreshTitle();
                return;
            }
            if (getParentLayout() == null) {
                return;
            }
            for (BaseFragment f : getParentLayout().getFragmentStack()) {
                if (f instanceof DialogsActivity) {
                    ((DialogsActivity) f).ithaRefreshTitle();
                }
            }
        } catch (Throwable ignore) {
        }
    }

    /** La barra de busqueda cambia el espacio de toda la lista: se aplica bien al reiniciar la app. */
    private void askRestart() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(tr("Reiniciar la app", "Restart the app"));
        b.setMessage(tr("Para acomodar la barra de búsqueda hay que reiniciar la app. ¿Reiniciar ahora?", "The search bar needs an app restart. Restart now?"));
        b.setPositiveButton(tr("Reiniciar", "Restart"), (d, w) -> IthaRestartActivity.restart(getParentActivity()));
        b.setNegativeButton(tr("Más tarde", "Later"), null);
        showDialog(b.create());
    }

    private void restartApp() {
        try {
            final android.app.Activity activity = getParentActivity();
            final Context c = org.telegram.messenger.ApplicationLoader.applicationContext;
            android.content.Intent intent = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
            if (activity == null || intent == null) {
                return;
            }
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // Plan B por si el telefono bloquea el paso siguiente: una alarma que abre la app
            android.app.PendingIntent pending = android.app.PendingIntent.getActivity(c, 7731, intent, android.app.PendingIntent.FLAG_CANCEL_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
            android.app.AlarmManager alarm = (android.app.AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            alarm.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 600, pending);
            // Plan A: pedirle a Android que abra la app ahora (permitido porque la app esta en pantalla) y cerrar el proceso viejo
            activity.startActivity(intent);
            AndroidUtilities.runOnUIThread(() -> android.os.Process.killProcess(android.os.Process.myPid()), 300);
        } catch (Throwable ignore) {
        }
    }

    private interface BoolCallback {
        void run(boolean value);
    }

    private void check(Context context, String title, boolean checked, BoolCallback callback) {
        final TextCheckCell cell = new TextCheckCell(context);
        IthaMd3.style(cell);
        cell.setTextAndCheck(title, checked, false);
        cell.setOnClickListener(v -> {
            boolean value = !cell.isChecked();
            cell.setChecked(value);
            callback.run(value);
        });
        IthaMd3.add(listLayout, cell);
    }

    /** Dibujo de ejemplo de la cabecera de la lista de chats. */
    private static class PreviewView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        PreviewView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final int w = getMeasuredWidth();
            final int h = getMeasuredHeight();
            final int gray = Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 0.14f);
            final int text = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
            final int hint = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);

            // tarjeta
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, dp(18), dp(18), paint);
            rect.set(1, 1, w - 1, h - 1);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1, dp(1)));
            paint.setColor(gray);
            canvas.drawRoundRect(rect, dp(18), dp(18), paint);
            paint.setStyle(Paint.Style.FILL);

            // botones de los costados (menu y lupa)
            final float barCy = dp(28);
            paint.setColor(gray);
            canvas.drawCircle(dp(28), barCy, dp(14), paint);
            canvas.drawCircle(w - dp(28), barCy, dp(14), paint);

            // titulo
            paint.setColor(text);
            paint.setTypeface(AndroidUtilities.bold());
            paint.setTextSize(dp(17));
            final String title = IthaConfig.getTitleText();
            final float maxW = w - dp(120);
            String shown = title;
            while (shown.length() > 1 && paint.measureText(shown) > maxW) {
                shown = shown.substring(0, shown.length() - 1);
            }
            final float tw = paint.measureText(shown);
            final float x = IthaConfig.isTitleCentered() ? (w - tw) / 2f : dp(56);
            canvas.drawText(shown, x, barCy + dp(6), paint);

            float y = dp(56);
            // barra de busqueda
            if (!IthaConfig.isSearchBarHidden()) {
                paint.setColor(gray);
                rect.set(dp(14), y, w - dp(14), y + dp(34));
                canvas.drawRoundRect(rect, dp(17), dp(17), paint);
                paint.setColor(hint);
                paint.setTypeface(null);
                paint.setTextSize(dp(14));
                canvas.drawText(tr("Buscar chats", "Search chats"), dp(32), y + dp(22), paint);
                y += dp(44);
            } else {
                y += dp(6);
            }

            // filas de chat de ejemplo
            for (int i = 0; i < 2 && y + dp(40) < h; i++) {
                paint.setColor(gray);
                canvas.drawCircle(dp(30), y + dp(20), dp(17), paint);
                rect.set(dp(58), y + dp(9), dp(58) + (w - dp(110)) * 0.45f, y + dp(17));
                canvas.drawRoundRect(rect, dp(4), dp(4), paint);
                rect.set(dp(58), y + dp(24), dp(58) + (w - dp(110)) * 0.75f, y + dp(31));
                canvas.drawRoundRect(rect, dp(4), dp(4), paint);
                y += dp(46);
            }
        }
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
        IthaMd3.style(row);
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
        IthaMd3.add(listLayout, row);
    }
}
