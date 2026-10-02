package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Weight;
import net.minecraft.client.gui.GuiGraphics;

import static com.persiki84.mediaplayer.island.IslandMeasure.ARTIST_SCALE;
import static com.persiki84.mediaplayer.island.IslandMeasure.CARD_HEIGHT;
import static com.persiki84.mediaplayer.island.IslandMeasure.GAP;
import static com.persiki84.mediaplayer.island.IslandMeasure.NICK_SCALE;
import static com.persiki84.mediaplayer.island.IslandMeasure.PAD;
import static com.persiki84.mediaplayer.island.IslandMeasure.PILL_HEIGHT;
import static com.persiki84.mediaplayer.island.IslandMeasure.STAT_INSET;
import static com.persiki84.mediaplayer.island.IslandMeasure.TITLE_CARD_SCALE;
import static com.persiki84.mediaplayer.island.IslandMeasure.TITLE_PILL_SCALE;

final class IslandContent {
    static final float PILL_TITLE_TOP = 2.4f;
    static final float PILL_ROW_CENTER = 6.6f;
    static final float PILL_TIME_TOP = 11.6f;
    static final float CARD_TITLE_TOP = 7.5f;
    static final float CARD_ARTIST_TOP = 19.5f;
    static final float CARD_TIME_TOP = 35.5f;
    private static final float CARD_WAVE_CENTER = 17.0f;
    private static final float MIN_SLOT = 4.0f;

    private IslandContent() {}

    static void draw(IslandFlight flight) {
        face(flight);
        mainLine(flight);
        artist(flight);
        IslandTimeline.draw(flight);
        bars(flight);
    }

    private static void face(IslandFlight flight) {
        IslandMeasure measure = flight.measure();
        float size = flight.mix(measure.face(), measure.art());
        if (size <= 0.0f) return;

        IslandFace.draw(flight.scene().graphics(), flight.x(PAD + measure.face() / 2.0f, PAD + measure.art() / 2.0f),
                flight.y(PILL_HEIGHT / 2.0f, CARD_HEIGHT / 2.0f), size, 1.0f, flight.blur(),
                IslandHud.showsArt(measure));
    }

    static float textX(IslandFlight flight) {
        IslandMeasure measure = flight.measure();
        return flight.x(PAD + measure.face() + GAP, PAD + measure.art() + GAP);
    }

    private static float slot(IslandFlight flight) {
        IslandMeasure measure = flight.measure();
        float idleReserve = measure.stats() > 0.0f ? measure.stats() + STAT_INSET : 0.0f;
        float reserve = Anim.lerp(idleReserve, measure.waveSlot(), measure.media());
        float pill = measure.pillWidth() - PAD - reserve - (PAD + measure.face() + GAP);
        float card = measure.cardWidth() - PAD - measure.cardWaveSlot() - (PAD + measure.art() + GAP);
        return flight.mix(pill, card);
    }

    // WHY: ник и название стоят каждый в своём покое своим кеглем, и переход между ними идёт позой:
    // WHY: уходящая строка едет и растёт к месту приходящей, приходящая выходит из места уходящей
    private static void mainLine(IslandFlight flight) {
        IslandMeasure measure = flight.measure();
        float slot = slot(flight);
        if (slot <= MIN_SLOT) return;

        float media = measure.media();
        float nickTop = Ink.centerY(0.0f, PILL_HEIGHT, Ink.fit(NICK_SCALE));
        float titleScale = Ink.fit(TITLE_PILL_SCALE);
        float pillTitle = Anim.lerp(PILL_TITLE_TOP, Ink.centerY(0.0f, PILL_HEIGHT, titleScale), measure.blind());
        float pillTop = Anim.lerp(nickTop, pillTitle, media);
        if (IslandSettings.on(IslandFlag.NICK) && media < 0.98f) nick(flight, slot, pillTop, media);
        if (!IslandSettings.on(IslandFlag.TITLE) || media <= 0.02f) return;

        float pillScale = titleScale * Anim.lerp(Ink.fit(NICK_SCALE) / titleScale, 1.0f, media);
        float scale = flight.mix(pillScale, Ink.fit(TITLE_CARD_SCALE));
        float top = flight.y(pillTop, CARD_TITLE_TOP);
        float x = textX(flight);
        flight.scene().titles().title.draw(flight.scene().graphics(), flight.scene().font(), x, top, slot, scale,
                ink(media), flight.blur());
    }

    private static void nick(IslandFlight flight, float slot, float top, float media) {
        float scale = Ink.fit(NICK_SCALE) * Anim.lerp(1.0f, Ink.fit(TITLE_PILL_SCALE) / Ink.fit(NICK_SCALE), media);
        GuiGraphics graphics = flight.scene().graphics();
        Ink.label(graphics, flight.scene().font(), IslandModel.nick(), Weight.SEMIBOLD, textX(flight),
                flight.pillY() + top, scale, ink((1.0f - media) * (1.0f - flight.share())), flight.blur());
    }

    // WHY: исполнителя нет в таблетке, и он выезжает из-под названия вниз на своё место: из строки
    // WHY: таймера он выезжал у самого края ещё маленькой таблетки и срезался её кромкой.
    // WHY: Проявляется к концу хода, когда карточке уже есть куда его поставить
    private static void artist(IslandFlight flight) {
        if (!IslandSettings.on(IslandFlag.ARTIST) || flight.share() <= 0.02f) return;

        float x = textX(flight);
        float top = flight.y(PILL_TITLE_TOP, CARD_ARTIST_TOP);
        float slot = slot(flight);
        flight.scene().titles().artist.draw(flight.scene().graphics(), flight.scene().font(), x, top, slot,
                Ink.fit(ARTIST_SCALE), quiet(flight.share() * flight.share()), flight.blur());
    }

    private static void bars(IslandFlight flight) {
        IslandMeasure measure = flight.measure();
        if (measure.waveSlot() <= 0.0f) return;

        float pillCenter = Anim.lerp(PILL_ROW_CENTER, PILL_HEIGHT / 2.0f, measure.blind());
        IslandGlyph.draw(flight.scene().graphics(),
                flight.x(measure.pillWidth() - PAD - IslandGlyph.PILL_WIDTH / 2.0f,
                        measure.cardWidth() - PAD - IslandGlyph.CARD_WIDTH / 2.0f),
                flight.y(pillCenter, CARD_WAVE_CENTER), flight.mix(IslandGlyph.PILL_WIDTH, IslandGlyph.CARD_WIDTH),
                flight.mix(IslandGlyph.PILL_HEIGHT, IslandGlyph.CARD_HEIGHT), measure.media(), flight.blur());
    }

    static int ink(float alpha) {
        return Colors.alpha(Palette.INK, alpha);
    }

    // WHY: вторичные строки (исполнитель, таймер) полужирные, но приглушённые, как подписи в iOS:
    // WHY: обычное начертание Inter на восьми-десяти пикселях со сглаживанием выходило истощённым
    static int quiet(float alpha) {
        return Colors.alpha(Palette.INK_DIM, alpha);
    }
}
