package com.persiki84.mediaplayer.render;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.color.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;

public final class Ink {
    public static final float GLYPH_HEIGHT = 8.0f;

    private static final float BLUR_REACH = 1.6f;
    private static final float SPREAD = 0.6f;
    private static final int INNER_TAPS = 6;
    private static final int OUTER_TAPS = 10;
    private static final float INNER_SHARE = 0.55f;
    private static final float INNER_REACH = 0.45f;
    private static final float SHARP = 0.02f;
    private static final float LINE_UNITS = 9.0f;
    private static final float FADE_LINES = 1.8f;
    private static final float FADE_BOX_SHARE = 0.26f;
    private static final float MARGIN_LINES = 0.4f;
    private static final float MARGIN_BOX_SHARE = 0.08f;
    private static final float FADE_GROWTH = 3.0f;

    private static boolean snapping = true;
    private static float basePixels;

    private Ink() {}

    public static float centerY(float y, float height, float scale) {
        return y + (height - GLYPH_HEIGHT * scale) / 2.0f;
    }

    public static float width(Font font, Component text, Weight weight, float scale) {
        return font.width(Typeface.styled(text, weight, scale * base())) * scale;
    }

    public static float base() {
        return basePixels > 0.0f ? basePixels : Minecraft.getInstance().getWindow().getGuiScale();
    }

    public static void label(GuiGraphics graphics, Font font, Component text, Weight weight, float x, float y,
                             float scale, int color, float blur) {
        if ((color >>> 24) < 3) return;
        if (blur <= SHARP) {
            stamp(graphics, font, text, weight, x, y, scale, color, snapping);
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

    public static void line(GuiGraphics graphics, Font font, Line line, float x, float y, float slot, float scale,
                            int color, float blur) {
        float span = line.width(font, scale);
        boolean overflow = span > slot;
        float shift = overflow && blur <= SHARP ? Marquee.shift(line.raw(), span - slot) : 0.0f;
        clipped(graphics, x, y, slot, scale, () -> run(graphics, font, line, x, y, slot, scale, color, blur, shift,
                overflow));
    }

    // WHY: строка всегда ставится по буквам своей раскладкой, той же, что меряет ширину и морфит
    // WHY: буквы: раскладка игры расходилась с ней на пиксель-другой в пробелах, и на смене значения
    // WHY: строка вздрагивала. Бегущая строка гасит буквы у края по их месту, полоса растёт с уехавшей частью
    private static void run(GuiGraphics graphics, Font font, Line line, float x, float y, float slot, float scale,
                            int color, float blur, float shift, boolean overflow) {
        float margin = margin(slot, scale);
        float fade = Math.max(margin, Math.min(LINE_UNITS * scale * FADE_LINES, slot * FADE_BOX_SHARE));
        float start = x - shift;
        float leftWidth = Math.min(fade, margin + shift * FADE_GROWTH);
        float rightWidth = Math.min(fade, margin + Math.max(0.0f, start + line.width(font, scale) - x - slot)
                * FADE_GROWTH);
        Component[] glyphs = line.glyphs();
        float[] offsets = line.offsets(font, scale);
        float[] insets = line.insets(font, scale);
        for (int index = 0; index < glyphs.length; index++) {
            float left = start + (offsets[index] + insets[index]) * scale;
            float shown = overflow ? Math.min(Anim.clamp01((left - (x - margin)) / leftWidth),
                    Anim.clamp01((x + slot + margin - left) / rightWidth)) : 1.0f;
            if (shown <= 0.01f) continue;
            label(graphics, font, glyphs[index], line.weight(), left, y, scale, Colors.alpha(color, shown), blur);
        }
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
