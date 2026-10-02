package com.persiki84.mediaplayer.render;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.color.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

// WHY: строка меняется по буквам, как числа и подписи в iOS: буквы, что стоят на своих местах в обеих
// WHY: строках, не трогаются, общий хвост доезжает до нового положения, а остальные сменяются волной
// WHY: слева направо - старая уходит вверх и размывается, новая приходит снизу из размытия. Ход
// WHY: короткий, чтобы буквы не выходили за свою строку, и строка не пустеет ни на кадр
public final class MorphText {
    private static final float GLYPH_SECONDS = 0.3f;
    private static final float STAGGER_SECONDS = 0.02f;
    private static final float STAGGER_LIMIT = 0.3f;
    private static final float TRAVEL_UNITS = 2.0f;

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

    private int changedSpan() {
        return Math.max(1, current.glyphs().length - prefix - suffix);
    }

    private float duration() {
        return GLYPH_SECONDS + Math.min(STAGGER_LIMIT, STAGGER_SECONDS * (changedSpan() - 1));
    }

    public void draw(GuiGraphics graphics, Font font, float x, float y, float slot, float scale, int color,
                     float blur) {
        if (!morphing()) {
            Ink.line(graphics, font, current, x, y, slot, scale, color, blur);
            return;
        }
        Ink.Point start = Ink.origin(graphics, x, y);
        Ink.clipped(graphics, x, y, slot, scale, () -> {
            Lane now = new Lane(current, font, scale);
            Lane was = new Lane(previous, font, scale);
            steady(graphics, now, was, start.x(), start.y(), scale, color, blur);
            changed(graphics, now, was, start.x(), start.y(), scale, color, blur, 1.0f);
            changed(graphics, was, now, start.x(), start.y(), scale, color, blur, -1.0f);
        });
    }

    private record Lane(Line line, Font font, Component[] glyphs, float[] offsets) {
        Lane(Line line, Font font, float scale) {
            this(line, font, line.glyphs(), line.offsets(font, scale));
        }

        float left(int index) {
            return offsets[index];
        }

        boolean holds(int index, Lane other) {
            return index < glyphs.length && index < other.glyphs.length
                    && glyphs[index].getString().equals(other.glyphs[index].getString())
                    && Math.abs(left(index) - other.left(index)) < 0.01f;
        }
    }

    private void steady(GuiGraphics graphics, Lane now, Lane was, float x, float y, float scale, int color,
                        float blur) {
        float slide = Anim.smoothstep(0.0f, duration(), clock);
        int shift = was.glyphs().length - now.glyphs().length;
        for (int index = 0; index < now.glyphs().length; index++) {
            boolean tail = index >= now.glyphs().length - suffix;
            if (index >= prefix && !tail && !now.holds(index, was)) continue;
            float from = tail ? was.left(index + shift) : now.left(index);
            Ink.glyph(graphics, now.font(), now.glyphs()[index], current.weight(),
                    x + Anim.lerp(from, now.left(index), slide) * scale, y, scale, color, blur);
        }
    }

    // WHY: направление +1 рисует приходящие буквы (снизу, из размытия), -1 уходящие (вверх); лишние
    // WHY: буквы более длинной старой строки уходят вместе с последней новой, а не висят хвостом
    private void changed(GuiGraphics graphics, Lane lane, Lane other, float x, float y, float scale, int color,
                         float blur, float direction) {
        int last = lane.glyphs().length - suffix;
        for (int index = prefix; index < last; index++) {
            if (lane.holds(index, other)) continue;
            float wave = Math.min(STAGGER_LIMIT, Math.min(index - prefix, changedSpan() - 1) * STAGGER_SECONDS);
            float share = Anim.smoothstep(0.0f, 1.0f, (clock - wave) / GLYPH_SECONDS);
            float shown = direction > 0.0f ? share : 1.0f - share;
            float lift = direction > 0.0f ? 1.0f - share : -share;
            Ink.glyph(graphics, lane.font(), lane.glyphs()[index], lane.line().weight(), x + lane.left(index) * scale,
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
