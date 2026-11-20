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
        IthaMd3.centerTitle(actionBar, context, tr("Ajustes de chat", "Chat settings"));
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
        headerStyleRow(context);
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
                    preview.invalidate();
                });
        if (org.telegram.messenger.IthaStyles.chatHeader() == 0) {
            check(context, tr("Píldora compacta", "Compact pill"), tr("Ajusta el fondo del título más al texto, sin espacio de sobra.", "Fits the title background closer to the text."),
                    IthaConfig.isChatPillCompact(), (value) -> {
                        IthaConfig.setChatPillCompact(value);
                        preview.invalidate();
                    });
        }
        check(context, tr("Centrar título", "Center title"), tr("Centra el avatar y el nombre entre el botón de volver y el menú.", "Centers the avatar and name between the back and menu buttons."),
                IthaConfig.isChatTitleCentered(), (value) -> {
                    IthaConfig.setChatTitleCentered(value);
                    preview.invalidate();
                });
        if (org.telegram.messenger.IthaStyles.chatHeader() == 0) {
            check(context, tr("Ocultar fondo de píldora", "Hide pill background"), tr("Quita el fondo de vidrio detrás del título del chat.", "Removes the glass background behind the chat title."),
                    IthaConfig.isChatPillHidden(), (value) -> {
                        IthaConfig.setChatPillHidden(value);
                        preview.invalidate();
                    });
        }

        section(context, tr("Burbujas de mensajes", "Message bubbles"));
        bubblePreview = new BubblePreview(context);
        IthaMd3.style(bubblePreview);
        IthaMd3.add(listLayout, bubblePreview);
        bubbleRow(context);
        radiusRow(context);
        gapRow(context);
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
        final TextView resetR = resetButton(context);
        resetR.setVisibility(SharedConfig.bubbleRadius != 17 ? View.VISIBLE : View.GONE);
        head.addView(resetR, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 12, 0));
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
                    resetR.setVisibility(v != 17 ? View.VISIBLE : View.GONE);
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
        resetR.setOnClickListener(v -> {
            SharedConfig.bubbleRadius = 17;
            org.telegram.messenger.MessagesController.getGlobalMainSettings().edit().putInt("bubbleRadius", 17).apply();
            value.setText("17 dp");
            bar.setProgress(17 / (float) RADIUS_MAX);
            resetR.setVisibility(View.GONE);
            if (bubblePreview != null) {
                bubblePreview.invalidate();
            }
        });
        row.addView(bar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, 0, 4, 0, 0));
        IthaMd3.add(listLayout, row);
    }

    /** Boton chico "Restablecer" que aparece solo cuando el valor no es el de Telegram. */
    private TextView resetButton(Context context) {
        TextView t = new TextView(context);
        t.setText(tr("Restablecer", "Reset"));
        t.setTextSize(13);
        t.setTypeface(AndroidUtilities.bold());
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        t.setPadding(dp(8), dp(2), dp(8), dp(2));
        return t;
    }

    /** Deslizador para el espacio extra entre mensajes (0 a 12 dp). */
    private void gapRow(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        IthaMd3.style(row);
        row.setPadding(dp(20), dp(12), dp(20), dp(8));
        LinearLayout head = new LinearLayout(context);
        head.setOrientation(LinearLayout.HORIZONTAL);
        TextView title = new TextView(context);
        title.setText(tr("Espacio entre mensajes", "Space between messages"));
        title.setTextSize(16);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        head.addView(title, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        final TextView resetG = resetButton(context);
        resetG.setVisibility(org.telegram.messenger.IthaBubbles.gap() != 0 ? View.VISIBLE : View.GONE);
        head.addView(resetG, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 12, 0));
        final TextView value = new TextView(context);
        value.setText("+" + org.telegram.messenger.IthaBubbles.gap() + " dp");
        value.setTextSize(14);
        value.setTypeface(AndroidUtilities.bold());
        value.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        head.addView(value, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        row.addView(head, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final SeekBarView bar = new SeekBarView(context);
        bar.setReportChanges(true);
        bar.setSeparatorsCount(13);
        bar.setOuterColor(Theme.getColor(Theme.key_player_progress));
        bar.setProgress(org.telegram.messenger.IthaBubbles.gap() / 12f);
        bar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {
                int v = Math.round(12 * progress);
                if (v != org.telegram.messenger.IthaBubbles.gap()) {
                    org.telegram.messenger.IthaBubbles.setGap(v);
                    value.setText("+" + v + " dp");
                    resetG.setVisibility(v != 0 ? View.VISIBLE : View.GONE);
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
                return String.valueOf(org.telegram.messenger.IthaBubbles.gap());
            }

            @Override
            public int getStepsCount() {
                return 12;
            }
        });
        resetG.setOnClickListener(v -> {
            org.telegram.messenger.IthaBubbles.setGap(0);
            value.setText("+0 dp");
            bar.setProgress(0f);
            resetG.setVisibility(View.GONE);
            if (bubblePreview != null) {
                bubblePreview.invalidate();
            }
        });
        row.addView(bar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, 0, 4, 0, 0));
        IthaMd3.add(listLayout, row);
    }

    /** Elegir el estilo del encabezado del chat: Telegram o Material 3. */
    private void headerStyleRow(Context context) {
        final String[] names = new String[]{
                tr("Actual de Telegram", "Telegram default"),
                tr("Material 3", "Material 3")};
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        IthaMd3.style(row);
        row.setPadding(dp(20), dp(12), dp(20), dp(12));
        TextView title = new TextView(context);
        title.setText(tr("Estilo del encabezado", "Header style"));
        title.setTextSize(16);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        row.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        TextView value = new TextView(context);
        value.setText(names[org.telegram.messenger.IthaStyles.chatHeader()]);
        value.setTextSize(13);
        value.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        row.addView(value, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        row.setOnClickListener(v -> {
            if (getParentActivity() == null) {
                return;
            }
            org.telegram.ui.ActionBar.AlertDialog.Builder b = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
            b.setTitle(tr("Estilo del encabezado", "Header style"));
            b.setItems(names, (d, which) -> {
                org.telegram.messenger.IthaStyles.setChatHeader(which);
                rebuild();
            });
            b.setNegativeButton(tr("Cancelar", "Cancel"), null);
            showDialog(b.create());
        });
        IthaMd3.add(listLayout, row);
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
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(138));
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
            bubble(canvas, 0, dp(63) + dp(org.telegram.messenger.IthaBubbles.gap()), true, tr("¡Sí, está genial!", "Yes, it's great!"), "16:31");
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

        private void icon(Canvas canvas, int res, float cx, float cy, int color, int size) {
            android.graphics.drawable.Drawable d = getContext().getResources().getDrawable(res).mutate();
            d.setColorFilter(new android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN));
            final int hs = dp(size) / 2;
            d.setBounds((int) cx - hs, (int) cy - hs, (int) cx + hs, (int) cy + hs);
            d.draw(canvas);
        }

        private void avatarCircle(Canvas canvas, float cx, float cy, float r, String initial) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Theme.getColor(Theme.key_avatar_backgroundBlue));
            canvas.drawCircle(cx, cy, r, paint);
            paint.setColor(0xffffffff);
            paint.setTypeface(AndroidUtilities.bold());
            paint.setTextSize(r * 0.9f);
            canvas.drawText(initial, cx - paint.measureText(initial) / 2f, cy + r * 0.32f, paint);
        }

        /** Encabezado en Material 3: barra solida, avatar a la izquierda, iconos a la derecha. */
        private void drawMd3(Canvas canvas) {
            final int w = getMeasuredWidth();
            final int h = getMeasuredHeight();
            final float cy = h / 2f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(androidx.core.graphics.ColorUtils.blendARGB(Theme.getColor(Theme.key_windowBackgroundGray),
                    Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 0.1f));
            canvas.drawRect(0, 0, w, h, paint);

            final int bar = Theme.getColor(Theme.key_windowBackgroundWhite);
            final int text = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
            final int hint = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);
            final float top = cy - dp(32);
            final float bottom = cy + dp(32);
            paint.setColor(bar);
            canvas.drawRect(0, top, w, bottom, paint);
            paint.setColor(androidx.core.graphics.ColorUtils.setAlphaComponent(text, 31));
            canvas.drawRect(0, bottom - dp(1), w, bottom, paint);

            // flecha de volver
            paint.setColor(text);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(dp(31), cy - dp(7), dp(24), cy, paint);
            canvas.drawLine(dp(24), cy, dp(31), cy + dp(7), paint);
            paint.setStyle(Paint.Style.FILL);

            TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
            String name = user != null ? ContactsController.formatName(user.first_name, user.last_name) : null;
            if (name == null || name.trim().isEmpty()) {
                name = "IthaGram";
            }
            final String initial = initialOf(name);
            final String sub = tr("en línea", "online");
            final boolean avatar = !IthaConfig.isChatAvatarHidden();
            final boolean menuAv = avatar && IthaConfig.isChatAvatarMenu();
            final boolean avLeft = avatar && !menuAv && !IthaConfig.isChatAvatarRight();
            final boolean avRight = avatar && !menuAv && IthaConfig.isChatAvatarRight();
            final boolean callVisible = !IthaConfig.isChatCallInMenu();

            final float leftEdge = dp(56);
            final float rightEdge = w - (callVisible ? dp(100) : dp(52));
            final float region = rightEdge - leftEdge;
            final float avW = dp(48);
            paint.setTypeface(AndroidUtilities.bold());
            paint.setTextSize(dp(16));
            final float nameW = paint.measureText(name);
            paint.setTypeface(null);
            paint.setTextSize(dp(13));
            final float subW = paint.measureText(sub);
            final float textMax = Math.max(dp(40), region - (avLeft ? avW : 0) - (avRight ? avW : 0) - dp(8));
            final float textW = Math.min(Math.max(nameW, subW), textMax);
            final float groupW = (avLeft ? avW : 0) + textW + (avRight ? avW : 0);
            float x = IthaConfig.isChatTitleCentered() ? leftEdge + Math.max(0, (region - groupW) / 2f) : leftEdge + dp(4);

            if (avLeft) {
                avatarCircle(canvas, x + dp(20), cy, dp(20), initial);
                x += avW;
            }
            paint.setColor(text);
            paint.setTypeface(AndroidUtilities.bold());
            paint.setTextSize(dp(16));
            String shown = name;
            while (shown.length() > 1 && paint.measureText(shown) > textW) {
                shown = shown.substring(0, shown.length() - 1);
            }
            canvas.drawText(shown, x, cy - dp(2), paint);
            paint.setColor(hint);
            paint.setTypeface(null);
            paint.setTextSize(dp(13));
            canvas.drawText(sub, x, cy + dp(15), paint);
            x += textW;
            if (avRight) {
                avatarCircle(canvas, x + dp(8) + dp(20), cy, dp(20), initial);
            }

            if (callVisible) {
                icon(canvas, R.drawable.msg_calls, w - dp(76), cy, text, 24);
            }
            if (menuAv) {
                avatarCircle(canvas, w - dp(28), cy, dp(17), initial);
            } else {
                paint.setColor(text);
                for (int i = -1; i <= 1; i++) {
                    canvas.drawCircle(w - dp(28), cy + i * dp(6), dp(1.6f), paint);
                }
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (org.telegram.messenger.IthaStyles.chatHeader() == 1) {
                drawMd3(canvas);
                return;
            }
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
            final boolean callVisible = !IthaConfig.isChatCallInMenu();
            paint.setColor(bubble);
            if (callVisible) {
                rect.set(w - dp(88), cy - dp(20), w - dp(14), cy + dp(20));
                canvas.drawRoundRect(rect, dp(20), dp(20), paint);
                icon(canvas, R.drawable.msg_calls, w - dp(64), cy, text, 22);
            } else {
                canvas.drawCircle(w - dp(34), cy, dp(20), paint);
            }
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
            final float full = w - dp(60) - (callVisible ? dp(100) : dp(60));
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
