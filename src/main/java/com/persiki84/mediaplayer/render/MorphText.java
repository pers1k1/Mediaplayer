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
    private float tempo = 1.0f;
    private float shown;
    private float leaving;
    private int prefix;
    private int suffix;

    public MorphText(Weight weight) {
        current = new Line(weight);
        previous = new Line(weight);
    }

    public void set(String next) {
        set(next, 0.0f);
    }

    // WHY: строки лирики идут в темпе песни: в быстром речитативе следующая строка приходит через
    // WHY: секунду, и полный морф съедал бы её половину. Смена укладывается в seconds, если оно
    // WHY: задано, иначе идёт своим обычным ходом
    public void set(String next, float seconds) {
        if (current.raw().equals(next)) return;

        previous.take(current);
        current.set(next);
        prefix = commonPrefix(previous.glyphs(), current.glyphs());
        suffix = commonSuffix(previous.glyphs(), current.glyphs(), prefix);
        clock = previous.isEmpty() ? Float.MAX_VALUE : 0.0f;
        leaving = shown;
        shown = 0.0f;
        tempo = seconds > 0.0f ? Math.max(0.05f, seconds / duration()) : 1.0f;
    }

    public void advance(float delta) {
        if (clock < Float.MAX_VALUE) clock += delta / tempo;
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
        draw(graphics, font, x, y, slot, scale, color, blur, Sweep.NONE);
    }

    // WHY: подсветка лирики относится только к приходящей строке: уходящая уже пропета и уходит
    // WHY: полным цветом. Буквы за краем слота не рисуются вовсе, ножницы отсекают их уже после отправки
    public void draw(GuiGraphics graphics, Font font, float x, float y, float slot, float scale, int color,
                     float blur, Sweep sweep) {
        if (!morphing()) {
            shown = Ink.line(graphics, font, current, x, y, slot, scale, color, blur, sweep);
            return;
        }
        Ink.Point start = Ink.origin(graphics, x, y);
        Ink.clipped(graphics, x, y, slot, scale, () -> {
            Lane now = new Lane(current, font, scale, x - slot, x + slot * 2.0f, 0.0f);
            Lane was = new Lane(previous, font, scale, x - slot, x + slot * 2.0f, leaving / scale);
            steady(graphics, now, was, start.x(), start.y(), scale, color, blur, sweep);
            changed(graphics, now, was, start.x(), start.y(), scale, color, blur, 1.0f, sweep);
            changed(graphics, was, now, start.x(), start.y(), scale, color, blur, -1.0f, Sweep.NONE);
        });
    }

    // WHY: уходящая строка уезжает с того места, где стояла: прокрученная бегущей строкой длинная
    // WHY: строка иначе в первый кадр смены отскакивала к своему началу
    private record Lane(Line line, Font font, Component[] glyphs, float[] offsets, float minX, float maxX,
                        float scroll) {
        Lane(Line line, Font font, float scale, float minX, float maxX, float scroll) {
            this(line, font, line.glyphs(), line.offsets(font, scale), minX, maxX, scroll);
        }

        boolean outside(float left) {
            return left < minX || left > maxX;
        }

        float left(int index) {
            return offsets[index] - scroll;
        }

        boolean holds(int index, Lane other) {
            return index < glyphs.length && index < other.glyphs.length
                    && glyphs[index].getString().equals(other.glyphs[index].getString())
                    && Math.abs(left(index) - other.left(index)) < 0.01f;
        }
    }

    private void steady(GuiGraphics graphics, Lane now, Lane was, float x, float y, float scale, int color,
                        float blur, Sweep sweep) {
        float slide = Anim.smoothstep(0.0f, duration(), clock);
        int shift = was.glyphs().length - now.glyphs().length;
        for (int index = 0; index < now.glyphs().length; index++) {
            boolean tail = index >= now.glyphs().length - suffix;
            if (index >= prefix && !tail && !now.holds(index, was)) continue;
            float from = tail ? was.left(index + shift) : now.left(index);
            float left = x + Anim.lerp(from, now.left(index), slide) * scale;
            if (now.outside(left)) continue;
            float lit = sweep.lit(index);
            Ink.glyph(graphics, now.font(), now.glyphs()[index], current.weight(), left, y + Ink.lift(lit) * scale,
                    scale, Ink.sung(color, lit), blur);
        }
    }

    // WHY: направление +1 рисует приходящие буквы (снизу, из размытия), -1 уходящие (вверх); лишние
    // WHY: буквы более длинной старой строки уходят вместе с последней новой, а не висят хвостом
    private void changed(GuiGraphics graphics, Lane lane, Lane other, float x, float y, float scale, int color,
                         float blur, float direction, Sweep sweep) {
        int last = lane.glyphs().length - suffix;
        for (int index = prefix; index < last; index++) {
            float left = x + lane.left(index) * scale;
            if (lane.holds(index, other) || lane.outside(left)) continue;
            float wave = Math.min(STAGGER_LIMIT, Math.min(index - prefix, changedSpan() - 1) * STAGGER_SECONDS);
            float share = Anim.smoothstep(0.0f, 1.0f, (clock - wave) / GLYPH_SECONDS);
            float shown = direction > 0.0f ? share : 1.0f - share;
            float lift = direction > 0.0f ? 1.0f - share : -share;
            float lit = sweep.lit(index);
            Ink.glyph(graphics, lane.font(), lane.glyphs()[index], lane.line().weight(), left,
                    y + (lift * TRAVEL_UNITS + Ink.lift(lit)) * scale, scale, Ink.sung(Colors.alpha(color, shown), lit),
                    Math.max(blur, 1.0f - shown));
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
