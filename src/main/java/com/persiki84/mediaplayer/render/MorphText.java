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
    private static final float FOLLOW_RATE = 7.0f;
    private static final float LEAVE_SHARE = 0.45f;
    private static final float HANDOFF_SHARE = 0.2f;

    private final Line current;
    private final Line previous;
    private float clock = Float.MAX_VALUE;
    private float tempo = 1.0f;
    private float shown;
    private float leaving;
    private float lastDelta;
    private boolean followFresh = true;
    private boolean whole;
    private float[] litShown = new float[0];
    private int accentShown = -1;
    private float[] leavingLit = new float[0];
    private int leavingAccent = -1;
    private final Sweep leavingSweep = new Sweep() {
        @Override
        public float lit(int index) {
            return index >= 0 && index < leavingLit.length ? leavingLit[index] : 1.0f;
        }

        @Override
        public int accent(int base) {
            return leavingAccent;
        }
    };
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
    // WHY: строка лирики меняется целиком, волной: общий хвост двух разных строк случаен (точка, одна
    // WHY: буква), и он уезжал на новое место без анимации. Уходящая строка уносит с собой ровно тот
    // WHY: вид, каким горела в последнем кадре
    public void set(String next, float seconds) {
        if (current.raw().equals(next)) return;

        previous.take(current);
        current.set(next);
        whole = seconds > 0.0f;
        prefix = whole ? 0 : commonPrefix(previous.glyphs(), current.glyphs());
        suffix = whole ? 0 : commonSuffix(previous.glyphs(), current.glyphs(), prefix);
        leavingLit = litShown;
        leavingAccent = accentShown;
        litShown = new float[0];
        clock = previous.isEmpty() ? Float.MAX_VALUE : 0.0f;
        leaving = shown;
        shown = 0.0f;
        followFresh = true;
        tempo = seconds > 0.0f ? Math.max(0.05f, seconds / duration()) : 1.0f;
    }

    public void advance(float delta) {
        lastDelta = delta;
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
            shown = Ink.line(graphics, font, current, x, y, slot, scale, color, blur, sweep, follow(font, slot, scale, sweep));
            remember(sweep);
            return;
        }
        Ink.Point start = Ink.origin(graphics, x, y);
        Ink.clipped(graphics, x, y, slot, scale, () -> {
            Lane now = new Lane(current, font, scale, x, slot, 0.0f);
            Lane was = new Lane(previous, font, scale, x, slot, leaving / scale);
            steady(graphics, now, was, start.x(), start.y(), scale, color, blur, sweep);
            changed(graphics, now, was, start.x(), start.y(), scale, color, blur, 1.0f, sweep);
            changed(graphics, was, now, start.x(), start.y(), scale, color, blur, -1.0f, leavingSweep);
        });
    }

    // WHY: строка лирики длиннее слота догоняет голос экспоненциально, а не встаёт на место скачком:
    // WHY: метки слов и смена строки дают ступени, и бегущая строка дёргалась бы на каждой. Новая строка
    // WHY: начинает с цели сразу, без догона из прошлого положения
    private float follow(Font font, float slot, float scale, Sweep sweep) {
        if (sweep.head() < 0.0f) return -1.0f;

        float target = Ink.followTarget(font, current, scale, slot, sweep);
        if (followFresh) {
            followFresh = false;
            shown = target;
            return target;
        }
        return shown + (target - shown) * (1.0f - (float) Math.exp(-FOLLOW_RATE * lastDelta));
    }

    // WHY: уходящая строка уезжает с того места, где стояла: прокрученная бегущей строкой длинная
    // WHY: строка иначе в первый кадр смены отскакивала к своему началу
    private record Lane(Line line, Font font, Component[] glyphs, float[] offsets, float x, float slot, float scale,
                        float scroll, float span, boolean overflow) {
        Lane(Line line, Font font, float scale, float x, float slot, float scroll) {
            this(line, font, line.glyphs(), line.offsets(font, scale), x, slot, scale, scroll, line.width(font, scale),
                    scroll > 0.0f || line.width(font, scale) > slot + 0.5f);
        }

        boolean outside(float left) {
            return left < x - slot || left > x + slot * 2.0f;
        }

        float edge(float left) {
            return overflow ? Ink.edge(left, x, slot, scale, scroll * scale, span) : 1.0f;
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
            if (index >= prefix && !tail && !holds(now, index, was)) continue;
            float from = tail ? was.left(index + shift) : now.left(index);
            float left = x + Anim.lerp(from, now.left(index), slide) * scale;
            float edge = now.edge(left);
            if (now.outside(left) || edge <= 0.01f) continue;
            Ink.sungGlyph(graphics, now.font(), now.glyphs()[index], current.weight(), left, y, scale,
                    Colors.alpha(color, edge), blur, sweep, index);
        }
    }

    // WHY: направление +1 рисует приходящие буквы (снизу, из размытия), -1 уходящие (вверх); лишние
    // WHY: буквы более длинной старой строки уходят вместе с последней новой, а не висят хвостом
    private void changed(GuiGraphics graphics, Lane lane, Lane other, float x, float y, float scale, int color,
                         float blur, float direction, Sweep sweep) {
        int last = lane.glyphs().length - suffix;
        for (int index = prefix; index < last; index++) {
            float left = x + lane.left(index) * scale;
            float edge = lane.edge(left);
            if (holds(lane, index, other) || lane.outside(left) || edge <= 0.01f) continue;
            float share = share(index, direction);
            float shown = direction > 0.0f ? share : 1.0f - share;
            float lift = direction > 0.0f ? 1.0f - share : -share;
            Ink.sungGlyph(graphics, lane.font(), lane.glyphs()[index], lane.line().weight(), left,
                    y + lift * TRAVEL_UNITS * scale, scale, Colors.alpha(color, shown * edge), Math.max(blur, 1.0f - shown),
                    sweep, index);
        }
    }

    // WHY: строка лирики сменяется передачей, а не наложением: уходящая целиком поднимается и гаснет за
    // WHY: первые LEAVE_SHARE смены, приходящая волна начинается с HANDOFF_SHARE и укладывается в тот же
    // WHY: срок. Иначе две разные строки на смене стояли друг на друге в одних и тех же местах
    private float share(int index, float direction) {
        if (whole && direction < 0.0f) return Anim.smoothstep(0.0f, duration() * LEAVE_SHARE, clock);

        float local = whole ? (clock - duration() * HANDOFF_SHARE) / (1.0f - HANDOFF_SHARE) : clock;
        float wave = Math.min(STAGGER_LIMIT, Math.min(index - prefix, changedSpan() - 1) * STAGGER_SECONDS);
        return Anim.smoothstep(0.0f, 1.0f, (local - wave) / GLYPH_SECONDS);
    }

    private boolean holds(Lane lane, int index, Lane other) {
        return !whole && lane.holds(index, other);
    }

    private void remember(Sweep sweep) {
        int count = current.glyphs().length;
        if (litShown.length != count) litShown = new float[count];
        for (int index = 0; index < count; index++) litShown[index] = sweep.lit(index);
        accentShown = sweep.accent(-1);
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
