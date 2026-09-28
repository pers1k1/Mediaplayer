package com.persiki84.mediaplayer.render;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.color.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class Ink {
    public static final float GLYPH_HEIGHT = 8.0f;

    private static final int BLUR_TAPS = 6;
    private static final float BLUR_REACH = 2.2f;
    private static final float CORE_LOSS = 0.8f;
    private static final float TAP_SHARE = 0.22f;
    private static final float SHARP = 0.02f;
    private static final float LINE_UNITS = 9.0f;
    private static final float FADE_LINES = 1.8f;
    private static final float FADE_BOX_SHARE = 0.26f;
    private static final float MARGIN_LINES = 0.4f;
    private static final float MARGIN_BOX_SHARE = 0.08f;
    private static final float FADE_GROWTH = 3.0f;

    private Ink() {}

    public static float centerY(float y, float height, float scale) {
        return y + (height - GLYPH_HEIGHT * scale) / 2.0f;
    }

    public static float width(Font font, Component text, float scale) {
        return font.width(text) * scale;
    }

    public static void label(GuiGraphics graphics, Font font, Component text, float x, float y, float scale,
                             int color, float blur) {
        if ((color >>> 24) < 3) return;
        if (blur <= SHARP) {
            stamp(graphics, font, text, x, y, scale, color);
            return;
        }
        float reach = blur * BLUR_REACH * scale;
        stamp(graphics, font, text, x, y, scale, Colors.alpha(color, 1.0f - CORE_LOSS * blur));
        int tap = Colors.alpha(color, TAP_SHARE * blur);
        for (int index = 0; index < BLUR_TAPS; index++) {
            double angle = Math.PI * 2.0 * index / BLUR_TAPS;
            stamp(graphics, font, text, x + (float) Math.cos(angle) * reach, y + (float) Math.sin(angle) * reach,
                    scale, tap);
        }
    }

    private static void stamp(GuiGraphics graphics, Font font, Component text, float x, float y, float scale,
                              int color) {
        if ((color >>> 24) < 3) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    public static void line(GuiGraphics graphics, Font font, Line line, float x, float y, float slot, float scale,
                            int color, float blur) {
        float span = line.width(font) * scale;
        if (span <= slot || blur > SHARP) {
            clipped(graphics, x, y, slot, scale, () -> label(graphics, font, line.value(), x, y, scale, color, blur));
            return;
        }
        float shift = Marquee.shift(line.raw(), span - slot);
        clipped(graphics, x, y, slot, scale, () -> scroll(graphics, font, line, x, y, slot, scale, color, shift));
    }

    // WHY: ванильное перо не знает маски, и ножницы режут букву пополам: вместо этого каждая буква
    // WHY: бегущей строки гаснет по своему месту у края, а полоса затухания растёт с уехавшей частью
    private static void scroll(GuiGraphics graphics, Font font, Line line, float x, float y, float slot,
                               float scale, int color, float shift) {
        float margin = margin(slot, scale);
        float fade = Math.max(margin, Math.min(LINE_UNITS * scale * FADE_LINES, slot * FADE_BOX_SHARE));
        float start = x - shift;
        float leftWidth = Math.min(fade, margin + shift * FADE_GROWTH);
        float hiddenRight = Math.max(0.0f, start + line.width(font) * scale - x - slot);
        float rightWidth = Math.min(fade, margin + hiddenRight * FADE_GROWTH);
        Component[] glyphs = line.glyphs(font);
        float[] offsets = line.offsets(font);
        float[] advances = line.advances(font);
        for (int index = 0; index < glyphs.length; index++) {
            float left = start + offsets[index] * scale;
            float center = left + advances[index] * scale / 2.0f;
            float shown = Math.min(Anim.clamp01((center - (x - margin)) / leftWidth),
                    Anim.clamp01((x + slot + margin - center) / rightWidth));
            if (shown <= 0.01f) continue;
            stamp(graphics, font, glyphs[index], left, y, scale, Colors.alpha(color, shown));
        }
    }

    private static float margin(float slot, float scale) {
        return Math.min(LINE_UNITS * scale * MARGIN_LINES, slot * MARGIN_BOX_SHARE);
    }

    private static void clipped(GuiGraphics graphics, float x, float y, float slot, float scale, Runnable body) {
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
