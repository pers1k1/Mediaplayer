package com.persiki84.mediaplayer.render;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.color.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

// WHY: строка меняется по буквам, как числа и подписи в iOS: общее начало стоит на месте, общий
// WHY: хвост доезжает до нового положения, а отличающиеся буквы сменяются волной слева направо -
// WHY: старая уходит вверх и размывается, новая приходит снизу из размытия. У таймера так
// WHY: меняется одна цифра, у названия трека буквы перетекают волной, и строка не пустеет ни на кадр
public final class MorphText {
    private static final float GLYPH_SECONDS = 0.3f;
    private static final float STAGGER_SECONDS = 0.02f;
    private static final float STAGGER_LIMIT = 0.3f;
    private static final float TRAVEL_UNITS = 3.5f;

    private final Line current;
    private final Line previous;
    private float clock = Float.MAX_VALUE;
    private int prefix;
    private int suffix;

    public MorphText(Weight weight) {
        current = new Line(weight);
        previous = new Line(weight);
    }

    public void set(String next) {
        if (current.raw().equals(next)) return;

        previous.take(current);
        current.set(next);
        prefix = commonPrefix(previous.glyphs(), current.glyphs());
        suffix = commonSuffix(previous.glyphs(), current.glyphs(), prefix);
        clock = previous.isEmpty() ? Float.MAX_VALUE : 0.0f;
    }

    public void advance(float delta) {
        if (clock < Float.MAX_VALUE) clock += delta;
    }

    public boolean morphing() {
        return clock < duration();
    }

    public Line line() {
        return current;
    }

    private float duration() {
        int changed = Math.max(current.glyphs().length, previous.glyphs().length) - prefix - suffix;
        return GLYPH_SECONDS + Math.min(STAGGER_LIMIT, STAGGER_SECONDS * Math.max(0, changed - 1));
    }

    public void draw(GuiGraphics graphics, Font font, float x, float y, float slot, float scale, int color,
                     float blur) {
        if (!morphing()) {
            Ink.line(graphics, font, current, x, y, slot, scale, color, blur);
            return;
        }
        Ink.clipped(graphics, x, y, slot, scale, () -> {
            steady(graphics, font, x, y, scale, color, blur);
            changed(graphics, font, current, x, y, scale, color, blur, 1.0f);
            changed(graphics, font, previous, x, y, scale, color, blur, -1.0f);
        });
    }

    private void steady(GuiGraphics graphics, Font font, float x, float y, float scale, int color, float blur) {
        Component[] glyphs = current.glyphs();
        float[] now = current.offsets(font, scale);
        float[] was = previous.offsets(font, scale);
        float slide = Anim.smoothstep(0.0f, duration(), clock);
        int shift = previous.glyphs().length - glyphs.length;
        for (int index = 0; index < glyphs.length; index++) {
            boolean shared = index < prefix || index >= glyphs.length - suffix;
            if (!shared) continue;
            float from = index < prefix ? now[index] : was[index + shift];
            float left = x + Anim.lerp(from, now[index], slide) * scale;
            Ink.label(graphics, font, glyphs[index], current.weight(), left, y, scale, color, blur);
        }
    }

    // WHY: направление +1 рисует приходящие буквы (снизу вверх, из размытия), -1 уходящие
    private void changed(GuiGraphics graphics, Font font, Line line, float x, float y, float scale, int color,
                         float blur, float direction) {
        Component[] glyphs = line.glyphs();
        float[] offsets = line.offsets(font, scale);
        for (int index = prefix; index < glyphs.length - suffix; index++) {
            float share = Anim.smoothstep(0.0f, 1.0f,
                    (clock - Math.min(STAGGER_LIMIT, (index - prefix) * STAGGER_SECONDS)) / GLYPH_SECONDS);
            float shown = direction > 0.0f ? share : 1.0f - share;
            float lift = direction > 0.0f ? (1.0f - share) : -share;
            Ink.label(graphics, font, glyphs[index], line.weight(), x + offsets[index] * scale,
                    y + lift * TRAVEL_UNITS * scale, scale, Colors.alpha(color, shown), Math.max(blur, 1.0f - shown));
        }
    }

    private static int commonPrefix(Component[] was, Component[] now) {
        int length = 0;
        while (length < was.length && length < now.length && same(was[length], now[length])) length++;
        return length;
    }

    private static int commonSuffix(Component[] was, Component[] now, int prefix) {
        int length = 0;
        while (length < was.length - prefix && length < now.length - prefix
                && same(was[was.length - 1 - length], now[now.length - 1 - length])) {
            length++;
        }
        return length;
    }

    private static boolean same(Component left, Component right) {
        return left.getString().equals(right.getString());
    }
}
