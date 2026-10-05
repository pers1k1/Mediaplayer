package com.persiki84.mediaplayer.render;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.color.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public final class Ink {
    public static final float GLYPH_HEIGHT = 8.0f;

    private static final float BLUR_REACH = 1.6f;
    private static final float SPREAD = 0.6f;
    private static final int INNER_TAPS = 6;
    private static final int OUTER_TAPS = 10;
    private static final float INNER_SHARE = 0.55f;
    private static final float INNER_REACH = 0.45f;
    private static final int GLOW_TAPS = 8;
    private static final float GLOW_REACH = 0.7f;
    private static final float GLOW_SHARE = 0.03f;
    private static final float SHARP = 0.02f;
    private static final float LINE_UNITS = 9.0f;
    private static final float FADE_LINES = 1.8f;
    private static final float FADE_BOX_SHARE = 0.26f;
    private static final float MARGIN_LINES = 0.4f;
    private static final float MARGIN_BOX_SHARE = 0.08f;
    private static final float FADE_GROWTH = 3.0f;
    private static final float FOLLOW_SHARE = 0.45f;
    private static final float UNSUNG = 0.42f;
    private static final float LIFT_UNITS = 0.42f;
    private static final float MOTION_GROW = 0.035f;
    private static final float ACCENT_SHARE = 0.55f;
    private static final float BLOOM_SHARE = 0.1f;
    private static final float ACCENT_LIGHT = 0.6f;
    private static final int WHITE = 0xFFFFFFFF;
    private static final float BLOOM_REACH = 1.1f;
    private static final int BLOOM_TAPS = 8;
    private static final float HELD_LIFT_UNITS = 0.3f;
    private static final float HELD_GROW = 0.1f;
    private static final float HELD_BLOOM = 0.16f;
    private static final float HELD_WEIGHT_UNITS = 0.35f;

    private static boolean snapping = true;
    private static float basePixels;

    private Ink() {}

    public static float centerY(float y, float height, float scale) {
        return y + (height - GLYPH_HEIGHT * scale) / 2.0f;
    }

    public static float width(Font font, Component text, Weight weight, float scale) {
        return font.getSplitter().stringWidth(Typeface.styled(text, weight, scale * base())) * scale;
    }

    public static float base() {
        return basePixels > 0.0f ? basePixels : Minecraft.getInstance().getWindow().getGuiScale();
    }

    public record Point(float x, float y) {}

    public static void label(GuiGraphics graphics, Font font, Component text, Weight weight, float x, float y,
                             float scale, int color, float blur) {
        paint(graphics, font, text, weight, x, y, scale, color, blur, snapping);
    }

    public static void glyph(GuiGraphics graphics, Font font, Component text, Weight weight, float x, float y,
                             float scale, int color, float blur) {
        paint(graphics, font, text, weight, x, y, scale, color, blur, false);
    }

    private static void paint(GuiGraphics graphics, Font font, Component text, Weight weight, float x, float y,
                              float scale, int color, float blur, boolean onGrid) {
        if ((color >>> 24) < 3) return;
        if (blur <= SHARP) {
            glow(graphics, font, text, weight, x, y, scale, color);
            stamp(graphics, font, text, weight, x, y, scale, color, onGrid);
            return;
        }
        float reach = blur * BLUR_REACH * scale;
        float spread = SPREAD * blur;
        stamp(graphics, font, text, weight, x, y, scale, Colors.alpha(color, 1.0f - spread), false);
        ring(graphics, font, text, weight, x, y, scale, Colors.alpha(color, spread * INNER_SHARE / INNER_TAPS),
                reach * INNER_REACH, INNER_TAPS);
        ring(graphics, font, text, weight, x, y, scale,
                Colors.alpha(color, spread * (1.0f - INNER_SHARE) / OUTER_TAPS), reach, OUTER_TAPS);
    }

    // WHY: под буквой лежит лёгкое свечение её же цвета: кольцо прозрачных копий, сумма которых не
    // WHY: доходит до четверти яркости, поэтому это мягкий ореол, а не обводка
    private static void glow(GuiGraphics graphics, Font font, Component text, Weight weight, float x, float y,
                             float scale, int color) {
        if (!Typeface.modded()) return;

        ring(graphics, font, text, weight, x, y, scale, Colors.alpha(color, GLOW_SHARE), GLOW_REACH * scale,
                GLOW_TAPS);
    }

    // WHY: к целому физическому пикселю привязывается только начало строки, а буквы внутри стоят по
    // WHY: своим дробным местам: округление каждой буквы отдельно разводило промежутки между ними
    public static Point origin(GuiGraphics graphics, float x, float y) {
        if (!snapping) return new Point(x, y);

        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        float gui = Minecraft.getInstance().getWindow().getGuiScale();
        Vector2f device = pose.transformPosition(x, y, new Vector2f()).mul(gui);
        float shiftX = (Math.round(device.x) - device.x) / (gui * Math.max(1.0e-3f, pose.m00()));
        float shiftY = (Math.round(device.y) - device.y) / (gui * Math.max(1.0e-3f, pose.m11()));
        return new Point(x + shiftX, y + shiftY);
    }

    // WHY: размытие строки собирается из ядра и двух колец копий, веса которых в сумме дают ровно
    // WHY: исходную непрозрачность: свечения и осветления нет, а края букв расходятся мягкой дымкой
    private static void ring(GuiGraphics graphics, Font font, Component text, Weight weight, float x, float y,
                             float scale, int color, float reach, int taps) {
        if ((color >>> 24) < 2) return;

        for (int index = 0; index < taps; index++) {
            double angle = Math.PI * 2.0 * (index + 0.5) / taps;
            stamp(graphics, font, text, weight, x + (float) Math.cos(angle) * reach,
                    y + (float) Math.sin(angle) * reach, scale, color, false);
        }
    }

    private static void stamp(GuiGraphics graphics, Font font, Component text, Weight weight, float x, float y,
                              float scale, int color, boolean onGrid) {
        if ((color >>> 24) < 2) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        if (onGrid) snap(graphics);
        graphics.pose().scale(scale, scale);
        float pixels = QuadArea.pixels(new Matrix3x2f(graphics.pose()));
        graphics.drawString(font, Typeface.styled(text, weight, pixels), 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    // WHY: начало строки встаёт на целый физический пиксель, иначе растр глифа делится между двумя
    // WHY: пикселями и буквы мылятся; в движении сетку не держим, чтобы строка не шла ступенями
    private static void snap(GuiGraphics graphics) {
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        float gui = Minecraft.getInstance().getWindow().getGuiScale();
        float shiftX = (Math.round(pose.m20() * gui) - pose.m20() * gui) / gui;
        float shiftY = (Math.round(pose.m21() * gui) - pose.m21() * gui) / gui;
        graphics.pose().translate(shiftX / Math.max(1.0e-3f, pose.m00()), shiftY / Math.max(1.0e-3f, pose.m11()));
    }

    public static void snapping(boolean value) {
        snapping = value;
    }

    public static void basePixels(float value) {
        basePixels = value;
    }

    public static float fit(float scale) {
        return Typeface.fit(scale, base());
    }

    public static float line(GuiGraphics graphics, Font font, Line line, float x, float y, float slot, float scale,
                             int color, float blur) {
        return line(graphics, font, line, x, y, slot, scale, color, blur, Sweep.NONE, -1.0f);
    }

    // WHY: scroll не меньше нуля задаёт сдвиг строки снаружи: строку лирики двигает её сглаженное
    // WHY: следование за голосом, а не часы бегущей строки
    public static float line(GuiGraphics graphics, Font font, Line line, float x, float y, float slot, float scale,
                             int color, float blur, Sweep sweep, float scroll) {
        float span = line.width(font, scale);
        boolean overflow = span > slot;
        float shift = !overflow || blur > SHARP ? 0.0f
                : scroll >= 0.0f ? Math.min(scroll, span - slot) : Marquee.shift(line.raw(), span - slot);
        boolean gliding = scroll > 0.0f;
        clipped(graphics, x, y, slot, scale, () -> run(graphics, font, line, x, y, slot, scale, color, blur, shift,
                overflow, sweep, gliding));
        return shift;
    }

    // WHY: место голоса внутри строки берётся дробным индексом буквы, а не первой недогоревшей
    // WHY: буквой: та прыгала на соседнюю, как только догорала, и строка ехала рывками. Пропеваемое
    // WHY: место держится на FOLLOW_SHARE слота
    public static float followTarget(Font font, Line line, float scale, float slot, Sweep sweep) {
        float span = line.width(font, scale);
        float head = sweep.head();
        if (span <= slot || head < 0.0f) return 0.0f;

        float[] offsets = line.offsets(font, scale);
        int index = Math.min(offsets.length - 1, (int) head);
        float right = index + 1 < offsets.length ? offsets[index + 1] : span / scale;
        float place = (offsets[index] + (right - offsets[index]) * (head - index)) * scale;
        return Anim.clamp(place - slot * FOLLOW_SHARE, 0.0f, span - slot);
    }

    // WHY: непропетая буква приглушена, пропетая горит полным цветом, а та, что поётся сейчас, отдаёт
    // WHY: в цвет обложки и светится им же колоколом по своей доле. Подъём и лёгкий рост идут
    // WHY: движением её слога: слог поднимается целиком, держится, пока поётся, и оседает
    public static void sungGlyph(GuiGraphics graphics, Font font, Component glyph, Weight weight, float x, float y,
                                 float scale, int color, float blur, Sweep sweep, int index) {
        float lit = Anim.clamp01(sweep.lit(index));
        float active = 4.0f * lit * (1.0f - lit);
        float held = Anim.clamp01(sweep.held(index));
        if (held > 0.01f) {
            heldGlyph(graphics, font, glyph, weight, x, y, scale, color, blur, sweep, index, held);
            return;
        }
        int ink = Colors.alpha(color, UNSUNG + (1.0f - UNSUNG) * lit);
        float moving = Anim.clamp01(sweep.motion(index));
        float size = scale * (1.0f + MOTION_GROW * moving);
        float left = x - (size - scale) * width(font, glyph, weight, 1.0f) / 2.0f;
        float top = y - LIFT_UNITS * moving * scale - (size - scale) * GLYPH_HEIGHT / 2.0f;
        if (active > 0.01f) {
            int accent = sweep.accent(color);
            ink = Colors.mix(ink, (ink & 0xFF000000) | (lightened(accent) & 0x00FFFFFF), ACCENT_SHARE * active);
            if (blur <= SHARP) ring(graphics, font, glyph, weight, left, top, size,
                    Colors.alpha(accent, BLOOM_SHARE * active * ((color >>> 24) / 255.0f)), BLOOM_REACH * size, BLOOM_TAPS);
        }
        glyph(graphics, font, glyph, weight, left, top, size, ink, blur);
    }

    // WHY: цвет обложки осветляется к белому, прежде чем лечь на поющуюся букву: тёмная обложка
    // WHY: иначе делала её тусклее уже пропетых, а в Spicy Lyrics поющийся слог самый яркий
    private static int lightened(int accent) {
        return Colors.mix(0xFF000000 | accent, WHITE, ACCENT_LIGHT);
    }

    // WHY: буква затянутого слова держит акцент, пока слово тянется: подрастает вокруг своей середины,
    // WHY: стоит выше, светится цветом обложки и густеет второй копией со сдвигом в долю пикселя, как
    // WHY: жирное начертание. Всё идёт по held, поэтому акцент разгорается и гаснет плавно
    private static void heldGlyph(GuiGraphics graphics, Font font, Component glyph, Weight weight, float x, float y,
                                  float scale, int color, float blur, Sweep sweep, int index, float held) {
        float lit = Anim.clamp01(sweep.lit(index));
        float moving = Anim.clamp01(sweep.motion(index));
        float active = Math.max(4.0f * lit * (1.0f - lit), held);
        float size = scale * (1.0f + HELD_GROW * held + MOTION_GROW * moving);
        float left = x - (size - scale) * width(font, glyph, weight, 1.0f) / 2.0f;
        float top = y - (LIFT_UNITS * moving + HELD_LIFT_UNITS * held) * scale
                - (size - scale) * GLYPH_HEIGHT / 2.0f;
        int accent = sweep.accent(color);
        int base = Colors.alpha(color, UNSUNG + (1.0f - UNSUNG) * lit);
        int ink = Colors.mix(base, (base & 0xFF000000) | (lightened(accent) & 0x00FFFFFF), ACCENT_SHARE * active);
        if (blur <= SHARP) ring(graphics, font, glyph, weight, left, top, size,
                Colors.alpha(accent, (BLOOM_SHARE + HELD_BLOOM * held) * ((color >>> 24) / 255.0f)), BLOOM_REACH * size, BLOOM_TAPS);
        glyph(graphics, font, glyph, weight, left, top, size, ink, blur);
        glyph(graphics, font, glyph, weight, left + HELD_WEIGHT_UNITS * held * size, top, size, ink, blur);
    }

    // WHY: строка всегда ставится по буквам своей раскладкой, той же, что меряет ширину и морфит
    // WHY: буквы: раскладка игры расходилась с ней на пиксель-другой в пробелах, и на смене значения
    // WHY: строка вздрагивала. Бегущая строка гасит буквы у края по их месту, полоса растёт с уехавшей
    // WHY: частью. Едущая за голосом строка не привязывает начало к пикселю: шаг в пиксель на медленном
    // WHY: ходу и есть рывок
    private static void run(GuiGraphics graphics, Font font, Line line, float x, float y, float slot, float scale,
                            int color, float blur, float shift, boolean overflow, Sweep sweep, boolean gliding) {
        Point start = gliding ? new Point(x - shift, origin(graphics, x, y).y()) : origin(graphics, x - shift, y);
        float span = line.width(font, scale);
        Component[] glyphs = line.glyphs();
        float[] offsets = line.offsets(font, scale);
        for (int index = 0; index < glyphs.length; index++) {
            float left = start.x() + offsets[index] * scale;
            float shown = overflow ? edge(left, x, slot, scale, x - start.x(), span) : 1.0f;
            if (shown <= 0.01f) continue;
            sungGlyph(graphics, font, glyphs[index], line.weight(), left, start.y(), scale, Colors.alpha(color, shown),
                    blur, sweep, index);
        }
    }

    // WHY: кромка гасит букву по её месту, а полоса с каждой стороны растёт с уехавшей за неё частью
    // WHY: строки: у конца прокрутки правая полоса сходится к запасу за краем, и последняя буква горит
    // WHY: целиком. На смене строки каждая строка гаснет по своей прокрутке: общая полоса полной ширины
    // WHY: гасила конец уходящей и начало приходящей, и они горели только до и после смены
    public static float edge(float left, float x, float slot, float scale, float shift, float span) {
        float margin = margin(slot, scale);
        float fade = Math.max(margin, Math.min(LINE_UNITS * scale * FADE_LINES, slot * FADE_BOX_SHARE));
        float leftWidth = Math.min(fade, margin + Math.max(0.0f, shift) * FADE_GROWTH);
        float rightWidth = Math.min(fade, margin + Math.max(0.0f, span - shift - slot) * FADE_GROWTH);
        return Math.min(Anim.clamp01((left - (x - margin)) / leftWidth), Anim.clamp01((x + slot + margin - left) / rightWidth));
    }

    private static float margin(float slot, float scale) {
        return Math.min(LINE_UNITS * scale * MARGIN_LINES, slot * MARGIN_BOX_SHARE);
    }

    static void clipped(GuiGraphics graphics, float x, float y, float slot, float scale, Runnable body) {
        float margin = margin(slot, scale) + BLUR_REACH * scale;
        int top = (int) Math.floor(y - LINE_UNITS * scale);
        int bottom = (int) Math.ceil(y + LINE_UNITS * scale * 2.0f);
        graphics.enableScissor((int) Math.floor(x - margin), top, (int) Math.ceil(x + slot + margin), bottom);
        try {
            body.run();
        } finally {
            graphics.disableScissor();
        }
    }
}
