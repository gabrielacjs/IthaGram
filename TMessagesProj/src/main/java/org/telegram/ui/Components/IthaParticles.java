package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

import org.telegram.messenger.IthaConfig;
import org.telegram.messenger.LiteMode;

import java.util.ArrayList;
import java.util.Random;

/**
 * IthaGram: particulas propias para el fondo del chat (nieve, sakura, lluvia, hojas, matriz).
 * Se dibujan siempre que esten activadas en Itha Ajustes, sin depender de la fecha.
 */
public class IthaParticles {

    public static final int OFF = 0, SNOW = 1, SAKURA = 2, RAIN = 3, LEAVES = 4, MATRIX = 5;

    private static final int[] SAKURA_COLORS = {0xFFFFC1D6, 0xFFFFB0C8, 0xFFFFD6E3, 0xFFFF9FBD};
    private static final int[] LEAF_COLORS = {0xFFD9731A, 0xFFE8A020, 0xFFB5381F, 0xFFC99A1E, 0xFF8F4A1B};
    private static final char[] GLYPHS = new char[10 + 45];

    static {
        for (int i = 0; i < 10; i++) {
            GLYPHS[i] = (char) ('0' + i);
        }
        for (int i = 0; i < 45; i++) {
            GLYPHS[10 + i] = (char) (0xFF71 + i);
        }
    }

    private static class P {
        float x, y, vx, vy, size, rot, vrot, phase, sway, a;
        int color;
    }

    private final ArrayList<P> list = new ArrayList<>();
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final char[] one = new char[1];
    private long last;
    private int lastType = -1, lastW, lastH;
    private float[] colY = new float[0];
    private float[] colV = new float[0];

    public static boolean enabled() {
        return IthaConfig.getParticleType() != OFF && LiteMode.isEnabled(LiteMode.FLAG_CHAT_BACKGROUND);
    }

    public void onDraw(View parent, Canvas canvas) {
        final int w = parent.getMeasuredWidth();
        final int h = parent.getMeasuredHeight();
        final int type = IthaConfig.getParticleType();
        if (w <= 0 || h <= 0 || type == OFF) {
            return;
        }
        final long now = SystemClock.uptimeMillis();
        final float dt = last == 0 ? 16f : Math.min(40f, now - last);
        last = now;
        final float speed = IthaConfig.getParticleSpeed() / 100f;
        final float alpha = IthaConfig.getParticleOpacity() / 100f;
        if (type != lastType || w != lastW || h != lastH) {
            lastType = type;
            lastW = w;
            lastH = h;
            list.clear();
            if (type == MATRIX) {
                initMatrix(w, h);
            }
        }
        if (type == MATRIX) {
            drawMatrix(canvas, h, dt * speed, alpha, now);
        } else {
            drawFalling(canvas, type, w, h, dt / 1000f * speed, alpha);
        }
        parent.invalidate();
    }

    private void spawn(P p, int type, int w, int h, boolean anywhere) {
        p.x = random.nextFloat() * w;
        p.y = anywhere ? random.nextFloat() * h : -dp(12) - random.nextFloat() * dp(40);
        p.phase = random.nextFloat() * 6.28f;
        p.a = 0.6f + random.nextFloat() * 0.4f;
        p.rot = random.nextFloat() * 360f;
        p.vrot = 0;
        p.sway = 0;
        p.vx = 0;
        switch (type) {
            case SNOW:
                p.size = dp(1) + random.nextFloat() * dp(2.2f);
                p.vy = dp(18) + random.nextFloat() * dp(40);
                p.sway = dp(10) + random.nextFloat() * dp(14);
                p.color = 0xFFFFFFFF;
                break;
            case SAKURA:
                p.size = dp(3) + random.nextFloat() * dp(3.5f);
                p.vy = dp(22) + random.nextFloat() * dp(30);
                p.vx = dp(8) + random.nextFloat() * dp(14);
                p.sway = dp(14) + random.nextFloat() * dp(16);
                p.vrot = -90f + random.nextFloat() * 180f;
                p.color = SAKURA_COLORS[random.nextInt(SAKURA_COLORS.length)];
                break;
            case RAIN:
                p.x = random.nextFloat() * (w + dp(80));
                p.size = dp(9) + random.nextFloat() * dp(9);
                p.vy = dp(520) + random.nextFloat() * dp(300);
                p.vx = -dp(70);
                p.a = 0.4f + random.nextFloat() * 0.4f;
                p.color = 0xFFBFD9FF;
                break;
            default:
                p.size = dp(4) + random.nextFloat() * dp(4);
                p.vy = dp(30) + random.nextFloat() * dp(40);
                p.vx = -dp(10) + random.nextFloat() * dp(30);
                p.sway = dp(20) + random.nextFloat() * dp(20);
                p.vrot = -120f + random.nextFloat() * 240f;
                p.color = LEAF_COLORS[random.nextInt(LEAF_COLORS.length)];
                break;
        }
    }

    private void drawFalling(Canvas canvas, int type, int w, int h, float sec, float alpha) {
        final int target = IthaConfig.getParticleCount();
        while (list.size() < target) {
            P p = new P();
            spawn(p, type, w, h, true);
            list.add(p);
        }
        while (list.size() > target) {
            list.remove(list.size() - 1);
        }
        for (int i = 0, n = list.size(); i < n; i++) {
            final P p = list.get(i);
            p.phase += sec * 2f;
            p.x += (p.vx + (float) Math.sin(p.phase) * p.sway) * sec;
            p.y += p.vy * sec;
            p.rot += p.vrot * sec;
            if (p.y > h + p.size * 2 || p.x < -dp(40) || p.x > w + dp(40) + (type == RAIN ? dp(80) : 0)) {
                spawn(p, type, w, h, false);
            }
            final int a = (int) (255 * alpha * p.a);
            paint.setColor((p.color & 0x00FFFFFF) | (a << 24));
            final float s = p.size;
            switch (type) {
                case SNOW:
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(p.x, p.y, s, paint);
                    break;
                case SAKURA:
                    paint.setStyle(Paint.Style.FILL);
                    canvas.save();
                    canvas.translate(p.x, p.y);
                    canvas.rotate(p.rot);
                    rect.set(-s, -s * 0.55f, s, s * 0.55f);
                    canvas.drawOval(rect, paint);
                    canvas.restore();
                    break;
                case RAIN: {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(1));
                    paint.setStrokeCap(Paint.Cap.ROUND);
                    final float m = (float) Math.hypot(p.vx, p.vy);
                    canvas.drawLine(p.x, p.y, p.x + p.vx / m * s, p.y + p.vy / m * s, paint);
                    break;
                }
                default:
                    paint.setStyle(Paint.Style.FILL);
                    path.reset();
                    path.moveTo(0, -s);
                    path.quadTo(s * 0.9f, -s * 0.2f, 0, s);
                    path.quadTo(-s * 0.9f, -s * 0.2f, 0, -s);
                    path.close();
                    canvas.save();
                    canvas.translate(p.x, p.y);
                    canvas.rotate(p.rot);
                    canvas.drawPath(path, paint);
                    canvas.restore();
                    break;
            }
        }
    }

    private void initMatrix(int w, int h) {
        final int cols = w / dp(14) + 1;
        colY = new float[cols];
        colV = new float[cols];
        for (int i = 0; i < cols; i++) {
            colY[i] = -random.nextFloat() * h;
            colV[i] = dp(90) + random.nextFloat() * dp(160);
        }
    }

    private void drawMatrix(Canvas canvas, int h, float dtSpeed, float alpha, long now) {
        final float colW = dp(14);
        final float charH = dp(13);
        final int tail = 14;
        final float density = Math.min(1f, IthaConfig.getParticleCount() / 150f);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(dp(12));
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < colY.length; i++) {
            if (((i * 7919) % 100) / 100f >= density) {
                continue;
            }
            colY[i] += colV[i] * dtSpeed / 1000f;
            if (colY[i] - tail * charH > h) {
                colY[i] = -random.nextFloat() * h * 0.5f - charH;
                colV[i] = dp(90) + random.nextFloat() * dp(160);
            }
            final float x = i * colW + colW / 2f;
            final int headRow = (int) Math.floor(colY[i] / charH);
            for (int k = 0; k < tail; k++) {
                final float y = colY[i] - k * charH;
                if (y < 0 || y > h) {
                    continue;
                }
                final int hash = (i * 73856093) ^ ((headRow - k) * 19349663) ^ (int) ((now >> 8) + i) * 83492791;
                one[0] = GLYPHS[(hash & 0x7fffffff) % GLYPHS.length];
                final float f = 1f - k / (float) tail;
                final int a = (int) (255 * alpha * f * f);
                paint.setColor((k == 0 ? 0x00D8FFE0 : 0x0000FF46) | (a << 24));
                canvas.drawText(one, 0, 1, x, y, paint);
            }
        }
    }
}
