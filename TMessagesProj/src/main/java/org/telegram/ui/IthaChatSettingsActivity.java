package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.IthaConfig;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.DownloadQueueManager.tr;

/**
 * IthaGram: "Ajustes de chat". Opciones del encabezado que se ve dentro de los chats, con una vista previa.
 */
public class IthaChatSettingsActivity extends BaseFragment {

    private LinearLayout listLayout;
    private Context ctx;
    private PreviewView preview;
    private BubblePreview bubblePreview;
    private static final int RADIUS_MAX = 28;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(tr("Ajustes de chat", "Chat settings"));
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
        listLayout.addView(preview, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 116, 0, 0, 0, 0));

        TextView info = new TextView(context);
        info.setText(tr("La vista previa de arriba se actualiza al instante. Los cambios se ven al abrir un chat.", "The preview above updates instantly. Changes show when you open a chat."));
        info.setTextSize(13);
        info.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setPadding(dp(20), dp(10), dp(20), dp(10));
        listLayout.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        section(context, tr("Encabezado de chat", "Chat header"));
        check(context, tr("Ocultar avatar", "Hide avatar"), tr("Quita el avatar del encabezado. Tocá el título para ver el perfil.", "Removes the avatar from the header. Tap the title to see the profile."),
                IthaConfig.isChatAvatarHidden(), (value) -> {
                    IthaConfig.setChatAvatarHidden(value);
                    preview.invalidate();
                });
        check(context, tr("Avatar a la derecha", "Avatar on the right"), tr("Pone el avatar a la derecha del nombre.", "Puts the avatar to the right of the name."),
                IthaConfig.isChatAvatarRight(), (value) -> {
                    IthaConfig.setChatAvatarRight(value);
                    preview.invalidate();
                });
        check(context, tr("Avatar en el botón de menú", "Avatar in the menu button"), tr("El avatar ocupa el lugar de los tres puntos. Tocalo para abrir el menú; mantenelo presionado para ver el perfil.", "The avatar takes the place of the three dots. Tap it to open the menu; hold it to see the profile."),
                IthaConfig.isChatAvatarMenu(), (value) -> {
                    IthaConfig.setChatAvatarMenu(value);
                    preview.invalidate();
                });
        check(context, tr("Llamada dentro del menú", "Call inside the menu"), tr("Quita el botón de llamada de la barra; queda en el menú de los tres puntos. Se aplica al abrir un chat.", "Removes the call button from the bar; it stays in the three-dots menu. Applies when you open a chat."),
                IthaConfig.isChatCallInMenu(), (value) -> {
                    IthaConfig.setChatCallInMenu(value);
                });
        check(context, tr("Píldora compacta", "Compact pill"), tr("Ajusta el fondo del título más al texto, sin espacio de sobra.", "Fits the title background closer to the text."),
                IthaConfig.isChatPillCompact(), (value) -> {
                    IthaConfig.setChatPillCompact(value);
                    preview.invalidate();
                });
        check(context, tr("Centrar título", "Center title"), tr("Centra el avatar y el nombre entre el botón de volver y el menú.", "Centers the avatar and name between the back and menu buttons."),
                IthaConfig.isChatTitleCentered(), (value) -> {
                    IthaConfig.setChatTitleCentered(value);
                    preview.invalidate();
                });
        check(context, tr("Ocultar fondo de píldora", "Hide pill background"), tr("Quita el fondo de vidrio detrás del título del chat.", "Removes the glass background behind the chat title."),
                IthaConfig.isChatPillHidden(), (value) -> {
                    IthaConfig.setChatPillHidden(value);
                    preview.invalidate();
                });

        section(context, tr("Burbujas de mensajes", "Message bubbles"));
        bubblePreview = new BubblePreview(context);
        IthaMd3.style(bubblePreview);
        IthaMd3.add(listLayout, bubblePreview);
        bubbleRow(context);
        radiusRow(context);
        resetRow(context);
    }

    /** Deslizador para el redondeo de las esquinas de las burbujas (0 a 28). */
    private void radiusRow(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        IthaMd3.style(row);
        row.setPadding(dp(20), dp(12), dp(20), dp(8));
        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(context);
        title.setText(tr("Redondeo de esquinas", "Corner roundness"));
        title.setTextSize(16);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        head.addView(title, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        final TextView value = new TextView(context);
        value.setText(SharedConfig.bubbleRadius + " dp");
        value.setTextSize(14);
        value.setTypeface(AndroidUtilities.bold());
        value.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        head.addView(value, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        row.addView(head, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final SeekBarView bar = new SeekBarView(context);
        bar.setReportChanges(true);
        bar.setSeparatorsCount(RADIUS_MAX + 1);
        bar.setOuterColor(Theme.getColor(Theme.key_player_progress));
        bar.setProgress(Math.min(1f, SharedConfig.bubbleRadius / (float) RADIUS_MAX));
        bar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {
                int v = Math.round(RADIUS_MAX * progress);
                if (v != SharedConfig.bubbleRadius) {
                    SharedConfig.bubbleRadius = v;
                    org.telegram.messenger.MessagesController.getGlobalMainSettings().edit().putInt("bubbleRadius", v).apply();
                    value.setText(v + " dp");
                    if (bubblePreview != null) {
                        bubblePreview.invalidate();
                    }
                }
            }

            @Override
            public void onSeekBarPressed(boolean pressed) {
            }

            @Override
            public CharSequence getContentDescription() {
                return String.valueOf(SharedConfig.bubbleRadius);
            }

            @Override
            public int getStepsCount() {
                return RADIUS_MAX;
            }
        });
        row.addView(bar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, 0, 4, 0, 0));
        IthaMd3.add(listLayout, row);
    }

    /** Vuelve a lo normal de Telegram: burbujas con cola y esquinas de 17. */
    private void resetRow(Context context) {
        TextView reset = new TextView(context);
        reset.setText(tr("Volver a lo de Telegram", "Back to Telegram default"));
        reset.setTextSize(15);
        reset.setTypeface(AndroidUtilities.bold());
        reset.setGravity(Gravity.CENTER);
        reset.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        IthaMd3.style(reset);
        reset.setOnClickListener(v -> {
            SharedConfig.bubbleRadius = 17;
            org.telegram.messenger.MessagesController.getGlobalMainSettings().edit().putInt("bubbleRadius", 17).apply();
            org.telegram.messenger.IthaBubbles.set(0);
            rebuild();
        });
        listLayout.addView(reset, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, 12, 10, 12, 10));
    }

    /** Elegir el estilo de las burbujas: Telegram, iOS o Material 3. */
    private void bubbleRow(Context context) {
        final String[] names = new String[]{
                tr("Actual de Telegram", "Telegram default"),
                tr("Material 3 (sin cola)", "Material 3 (no tail)")};
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        row.setPadding(dp(20), dp(12), dp(20), dp(12));
        TextView title = new TextView(context);
        title.setText(tr("Estilo de burbujas", "Bubble style"));
        title.setTextSize(16);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        row.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        final TextView value = new TextView(context);
        value.setText(names[org.telegram.messenger.IthaBubbles.style() == 2 ? 1 : 0]);
        value.setTextSize(13);
        value.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        row.addView(value, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        row.setOnClickListener(v -> {
            if (getParentActivity() == null) {
                return;
            }
            org.telegram.ui.ActionBar.AlertDialog.Builder b = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
            b.setTitle(tr("Estilo de burbujas", "Bubble style"));
            b.setItems(names, (d, which) -> {
                org.telegram.messenger.IthaBubbles.set(which == 1 ? 2 : 0);
                value.setText(names[which]);
                if (bubblePreview != null) {
                    bubblePreview.invalidate();
                }
            });
            b.setNegativeButton(tr("Cancelar", "Cancel"), null);
            showDialog(b.create());
        });
        listLayout.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView note = new TextView(context);
        note.setText(tr("Se aplica al abrir un chat.", "Applies when you open a chat."));
        note.setTextSize(13);
        note.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        note.setPadding(dp(20), dp(10), dp(20), dp(10));
        listLayout.addView(note, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private interface BoolCallback {
        void run(boolean value);
    }

    private void check(Context context, String title, String subtitle, boolean checked, BoolCallback callback) {
        final TextCheckCell cell = new TextCheckCell(context);
        IthaMd3.style(cell);
        cell.setTextAndValueAndCheck(title, subtitle, checked, true, false);
        cell.setOnClickListener(v -> {
            boolean value = !cell.isChecked();
            cell.setChecked(value);
            callback.run(value);
        });
        IthaMd3.add(listLayout, cell);
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

    /** Vista previa de dos burbujas con el estilo y el redondeo elegidos. */
    private class BubblePreview extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF r = new RectF();

        BubblePreview(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(132));
        }

        private void bubble(Canvas canvas, float left, float top, boolean out, String text, String time) {
            final boolean md3 = org.telegram.messenger.IthaBubbles.style() == 2;
            p.setTextSize(dp(15));
            p.setTypeface(null);
            final float tw = p.measureText(text);
            p.setTextSize(dp(11));
            final float timeW = p.measureText(time);
            final float width = tw + dp(8) + timeW + dp(24);
            final float height = dp(42);
            final float l = out ? getMeasuredWidth() - dp(20) - width : left;
            final float t = top;
            r.set(l, t, l + width, t + height);
            final float rad = Math.min(dp(SharedConfig.bubbleRadius), height / 2f);
            final float near = md3 ? Math.min(dp(6), rad) : dp(2);
            final float tl = rad, tr = rad;
            final float br = out ? near : rad;
            final float bl = out ? rad : near;
            path.reset();
            path.addRoundRect(r, new float[]{tl, tl, tr, tr, br, br, bl, bl}, Path.Direction.CW);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Theme.getColor(out ? Theme.key_chat_outBubble : Theme.key_chat_inBubble));
            canvas.drawPath(path, p);
            if (!md3) {
                // cola de Telegram
                path.reset();
                if (out) {
                    path.moveTo(r.right - dp(8), r.bottom - dp(10));
                    path.lineTo(r.right + dp(5), r.bottom);
                    path.lineTo(r.right - dp(12), r.bottom);
                } else {
                    path.moveTo(r.left + dp(8), r.bottom - dp(10));
                    path.lineTo(r.left - dp(5), r.bottom);
                    path.lineTo(r.left + dp(12), r.bottom);
                }
                path.close();
                canvas.drawPath(path, p);
            }
            p.setTextSize(dp(15));
            p.setColor(Theme.getColor(out ? Theme.key_chat_messageTextOut : Theme.key_chat_messageTextIn));
            canvas.drawText(text, l + dp(12), t + dp(26), p);
            p.setTextSize(dp(11));
            p.setColor(Theme.getColor(out ? Theme.key_chat_outTimeText : Theme.key_chat_inTimeText));
            canvas.drawText(time, r.right - dp(12) - timeW, t + dp(36), p);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            bubble(canvas, dp(20), dp(18), false, tr("¿Ya probaste IthaGram?", "Tried IthaGram yet?"), "16:30");
            bubble(canvas, 0, dp(72), true, tr("¡Sí, está genial!", "Yes, it's great!"), "16:31");
        }
    }

    /** Dibujo de ejemplo del encabezado de un chat: boton de volver, pildora con avatar y titulo, menu. */
    private class PreviewView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        PreviewView(Context context) {
            super(context);
        }

        /** Primera letra del nombre sin partir caracteres especiales (emojis y letras decorativas ocupan dos chars). */
        private String initialOf(String value) {
            return new String(Character.toChars(value.codePointAt(0))).toUpperCase();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final int w = getMeasuredWidth();
            final int h = getMeasuredHeight();
            final float cy = h / 2f;

            paint.setStyle(Paint.Style.FILL);
            // fondo neutro segun el tema (el color de fondo del chat puede ser cualquiera, hasta rojo)
            paint.setColor(androidx.core.graphics.ColorUtils.blendARGB(Theme.getColor(Theme.key_windowBackgroundGray),
                    Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 0.1f));
            canvas.drawRect(0, 0, w, h, paint);

            final int bubble = Theme.getColor(Theme.key_windowBackgroundWhite);
            final int text = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
            final int hint = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);

            // boton de volver
            paint.setColor(bubble);
            canvas.drawCircle(dp(34), cy, dp(20), paint);
            paint.setColor(text);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(dp(37), cy - dp(7), dp(30), cy, paint);
            canvas.drawLine(dp(30), cy, dp(37), cy + dp(7), paint);
            paint.setStyle(Paint.Style.FILL);

            // boton de menu (tres puntos)
            paint.setColor(bubble);
            canvas.drawCircle(w - dp(34), cy, dp(20), paint);
            paint.setColor(text);
            for (int i = -1; i <= 1; i++) {
                canvas.drawCircle(w - dp(34), cy + i * dp(6), dp(1.6f), paint);
            }

            // nombre del usuario actual
            TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
            String name = user != null ? ContactsController.formatName(user.first_name, user.last_name) : null;
            if (name == null || name.trim().isEmpty()) {
                name = "IthaGram";
            }
            final String sub = tr("en línea", "online");
            final boolean avatar = !IthaConfig.isChatAvatarHidden();

            // la pildora se ajusta al texto, igual que en el chat
            paint.setTypeface(AndroidUtilities.bold());
            paint.setTextSize(dp(16));
            final float nameW = paint.measureText(name);
            paint.setTypeface(null);
            paint.setTextSize(dp(13));
            final float subW = paint.measureText(sub);
            // en el chat el contenido va pegado a la izquierda; compacta = la pildora se ajusta al texto
            final float pl0 = dp(60);
            final float full = w - dp(60) * 2;
            final float textW = Math.max(nameW, subW);
            final boolean menuAv = avatar && IthaConfig.isChatAvatarMenu();
            final boolean inPill = avatar && !menuAv;
            final boolean right = inPill && IthaConfig.isChatAvatarRight();
            final float contentW = right ? dp(16) + textW + dp(8) + dp(38) + dp(10) : (inPill ? dp(52) : dp(16)) + textW + dp(10);
            final float pillW = IthaConfig.isChatPillCompact() ? Math.min(full, contentW) : full;
            final boolean centered = IthaConfig.isChatTitleCentered();
            final float pl = pl0 + (centered ? (full - pillW) / 2f : 0f);
            final float pr = pl + pillW;
            // inicio del contenido (avatar y texto); centrado dentro de la pildora si corresponde
            final float cl = pl + (centered ? (pillW - Math.min(contentW, pillW)) / 2f : 0f);

            if (!IthaConfig.isChatPillHidden()) {
                rect.set(pl, cy - dp(24), pr, cy + dp(24));
                paint.setColor(bubble);
                canvas.drawRoundRect(rect, dp(24), dp(24), paint);
            }

            float textLeft = cl + dp(16);
            if (inPill) {
                final float acx = right ? cl + dp(16) + textW + dp(8) + dp(19) : cl + dp(24);
                paint.setColor(Theme.getColor(Theme.key_avatar_backgroundBlue));
                canvas.drawCircle(acx, cy, dp(19), paint);
                paint.setColor(0xffffffff);
                paint.setTypeface(AndroidUtilities.bold());
                paint.setTextSize(dp(16));
                final String initial = initialOf(name);
                canvas.drawText(initial, acx - paint.measureText(initial) / 2f, cy + dp(5.5f), paint);
                if (!right) {
                    textLeft = cl + dp(52);
                }
            }

            paint.setColor(text);
            paint.setTypeface(AndroidUtilities.bold());
            paint.setTextSize(dp(16));
            String shown = name;
            final float maxW = pr - textLeft - dp(8);
            while (shown.length() > 1 && paint.measureText(shown) > maxW) {
                shown = shown.substring(0, shown.length() - 1);
            }
            canvas.drawText(shown, textLeft, cy - dp(2), paint);

            paint.setColor(hint);
            paint.setTypeface(null);
            paint.setTextSize(dp(13));
            canvas.drawText(sub, textLeft, cy + dp(15), paint);

            if (menuAv) {
                // el avatar tapa los tres puntos del boton de menu
                final float mx = w - dp(34);
                paint.setColor(bubble);
                canvas.drawCircle(mx, cy, dp(20), paint);
                paint.setColor(Theme.getColor(Theme.key_avatar_backgroundBlue));
                canvas.drawCircle(mx, cy, dp(17), paint);
                paint.setColor(0xffffffff);
                paint.setTypeface(AndroidUtilities.bold());
                paint.setTextSize(dp(15));
                final String initial = initialOf(name);
                canvas.drawText(initial, mx - paint.measureText(initial) / 2f, cy + dp(5), paint);
            }
        }
    }
}
