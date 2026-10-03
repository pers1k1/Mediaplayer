package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.FrameClock;
import com.persiki84.mediaplayer.anim.Smooth;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Weight;
import net.minecraft.client.gui.GuiGraphics;

import static com.persiki84.mediaplayer.island.IslandContent.CARD_TIME_TOP;
import static com.persiki84.mediaplayer.island.IslandContent.PILL_TIME_TOP;
import static com.persiki84.mediaplayer.island.IslandMeasure.GAP;
import static com.persiki84.mediaplayer.island.IslandMeasure.PAD;
import static com.persiki84.mediaplayer.island.IslandMeasure.PILL_TIME_GAP;
import static com.persiki84.mediaplayer.island.IslandMeasure.TIME_SCALE;

// WHY: таймер и полоса летят вместе: волосок таблетки утолщается в стеклянную лунку карточки, а
// WHY: стиль меняется долей полёта, поэтому полоса ни в один кадр не пропадает
final class IslandTimeline {
    private static final float PILL_BAR_CENTER = 14.7f;
    private static final float CARD_BAR_TOP = 31.0f;
    private static final float HAIRLINE = 1.5f;
    private static final float BAR_HEIGHT = 2.8f;
    private static final float SEEK_HEAD_PILL = 1.6f;
    private static final float SEEK_HEAD_CARD = 1.15f;
    private static final float TRACK_SHADE = 0.16f;
    private static final float MIN_SPAN = 4.0f;
    private static final float BAR_SHIFT_SPEED = 12.0f;

    // WHY: полоса таблетки стоит за удержанной наибольшей шириной таймера и доезжает к ней плавно:
    // WHY: от ширины текущей строки она прыгала на каждой смене цифры вместе с общим временем
    private static final Smooth barShift = new Smooth(0.0f, BAR_SHIFT_SPEED);

    private IslandTimeline() {}

    static void draw(IslandFlight flight) {
        IslandMeasure measure = flight.measure();
        float fade = measure.media() * (1.0f - measure.blind());
        if (fade <= 0.02f || !IslandModel.track().present()) return;

        float timed = IslandSettings.on(IslandFlag.TIME) ? label(flight, fade) : 0.0f;
        if (!IslandSettings.on(IslandFlag.BAR) || flight.scene().titles().untimed()) return;

        float pillLeft = PAD + measure.face() + GAP + timed;
        float cardLeft = PAD + measure.art() + GAP;
        float left = flight.x(pillLeft, cardLeft);
        float span = flight.mix(measure.pillWidth() - PAD - pillLeft, measure.cardWidth() - PAD - cardLeft);
        if (span > MIN_SPAN) bar(flight, left, span, fade);
    }

    private static float label(IslandFlight flight, float fade) {
        IslandTitles titles = flight.scene().titles();
        GuiGraphics graphics = flight.scene().graphics();
        float x = IslandContent.textX(flight);
        float y = flight.y(PILL_TIME_TOP, CARD_TIME_TOP);
        float share = flight.share();
        float scale = Ink.fit(TIME_SCALE);
        IslandMeasure measure = flight.measure();
        float slot = measure.cardWidth();
        float pillSlot = Math.max(0.0f, measure.pillWidth() - PAD - (PAD + measure.face() + GAP));
        int pillInk = IslandContent.quiet(fade * (1.0f - share));
        titles.pillRow.draw(graphics, flight.scene().font(), x, y, flight.mix(pillSlot, slot), scale, pillInk,
                flight.blur());
        titles.timing.draw(graphics, flight.scene().font(), x, y, slot, scale, IslandContent.quiet(fade * share),
                flight.blur());
        return barShift.to(flight.measure().timingPeak() + PILL_TIME_GAP, FrameClock.delta());
    }

    private static void bar(IslandFlight flight, float left, float span, float fade) {
        GuiGraphics graphics = flight.scene().graphics();
        float share = flight.share();
        float thick = flight.mix(HAIRLINE, BAR_HEIGHT);
        float top = flight.y(PILL_BAR_CENTER - HAIRLINE / 2.0f, CARD_BAR_TOP);
        float hair = fade * (1.0f - share);
        IslandBar.hair(graphics, left, top, span, thick, Colors.withAlpha(Palette.WHITE, TRACK_SHADE * hair),
                Colors.alpha(Palette.ACCENT, hair), flight.blur());
        IslandBar.well(graphics, left, top, span, thick, fade * share, flight.blur());
        IslandBar.seekHead(graphics, left + span * IslandProgress.value(), top + thick / 2.0f,
                thick * flight.mix(SEEK_HEAD_PILL, SEEK_HEAD_CARD), fade);
    }
}
