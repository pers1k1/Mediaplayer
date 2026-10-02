package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Line;
import com.persiki84.mediaplayer.render.Weight;

import static com.persiki84.mediaplayer.island.IslandMeasure.ART;
import static com.persiki84.mediaplayer.island.IslandMeasure.ARTIST_SCALE;
import static com.persiki84.mediaplayer.island.IslandMeasure.CARD_HEIGHT;
import static com.persiki84.mediaplayer.island.IslandMeasure.GAP;
import static com.persiki84.mediaplayer.island.IslandMeasure.PAD;
import static com.persiki84.mediaplayer.island.IslandMeasure.TIME_SCALE;
import static com.persiki84.mediaplayer.island.IslandMeasure.TITLE_CARD_SCALE;

final class IslandCard {
    private static final float TITLE_TOP = 7.0f;
    private static final float ARTIST_TOP = 18.0f;
    private static final float BAR_TOP = 27.0f;
    private static final float TIME_TOP = 31.0f;
    private static final float BAR_HEIGHT = 2.5f;
    private static final float WAVE_CENTER = 15.5f;
    private static final float SEEK_HEAD = 1.15f;
    private static final float MIN_SLOT = 4.0f;
    private static final float MIN_BAR = 8.0f;

    private IslandCard() {}

    static void draw(IslandScene scene, float x, float y, float alpha, float blur) {
        if (alpha <= 0.02f) return;

        IslandMeasure measure = scene.measure();
        if (measure.art() > 0.0f) {
            IslandFace.draw(scene.graphics(), x + PAD + ART / 2.0f, y + CARD_HEIGHT / 2.0f, ART, alpha, blur,
                    IslandHud.showsArt(measure));
        }
        text(scene, x, y, alpha, blur);
        progress(scene, x, y, alpha, blur);
        if (measure.cardWaveSlot() > 0.0f) {
            IslandGlyph.draw(scene.graphics(), x + measure.cardWidth() - PAD - IslandGlyph.CARD_WIDTH / 2.0f,
                    y + WAVE_CENTER, IslandGlyph.CARD_WIDTH, IslandGlyph.CARD_HEIGHT, alpha, blur);
        }
    }

    private static void text(IslandScene scene, float x, float y, float alpha, float blur) {
        float textX = x + PAD + scene.measure().art() + GAP;
        float slot = x + scene.measure().cardWidth() - PAD - scene.measure().cardWaveSlot() - textX;
        if (slot <= MIN_SLOT) return;

        IslandTitles titles = scene.titles();
        if (IslandSettings.on(IslandFlag.TITLE)) {
            titles.swapped(scene.graphics(), titles.title, true, alpha, (Line line, float shown) -> Ink.line(
                    scene.graphics(), scene.font(), line, textX, y + TITLE_TOP, slot, TITLE_CARD_SCALE,
                    IslandPill.ink(shown), blur));
        }
        if (IslandSettings.on(IslandFlag.ARTIST)) {
            titles.swapped(scene.graphics(), titles.artist, false, alpha, (Line line, float shown) -> Ink.line(
                    scene.graphics(), scene.font(), line, textX, y + ARTIST_TOP, slot, ARTIST_SCALE,
                    IslandPill.ink(shown), blur));
        }
    }

    private static void progress(IslandScene scene, float x, float y, float alpha, float blur) {
        float fade = alpha * (1.0f - scene.measure().blind());
        if (fade <= 0.02f || !IslandModel.track().present()) return;

        float barX = x + PAD + scene.measure().art() + GAP;
        float span = x + scene.measure().cardWidth() - PAD - barX;
        if (span <= MIN_BAR) return;

        if (IslandSettings.on(IslandFlag.BAR) && !scene.titles().untimed()) {
            IslandBar.well(scene.graphics(), barX, y + BAR_TOP, span, BAR_HEIGHT, fade, blur);
            IslandBar.seekHead(scene.graphics(), barX + span * IslandProgress.value(),
                    y + BAR_TOP + BAR_HEIGHT / 2.0f, BAR_HEIGHT * SEEK_HEAD, fade);
        }
        if (IslandSettings.on(IslandFlag.TIME)) {
            Ink.label(scene.graphics(), scene.font(), scene.titles().timing.value(), Weight.REGULAR, barX,
                    y + TIME_TOP, TIME_SCALE,
                    IslandPill.ink(fade), blur);
        }
    }
}
