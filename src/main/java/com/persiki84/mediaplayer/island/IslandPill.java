package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Line;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import static com.persiki84.mediaplayer.island.IslandMeasure.FACE;
import static com.persiki84.mediaplayer.island.IslandMeasure.GAP;
import static com.persiki84.mediaplayer.island.IslandMeasure.NICK_SCALE;
import static com.persiki84.mediaplayer.island.IslandMeasure.PAD;
import static com.persiki84.mediaplayer.island.IslandMeasure.PILL_HEIGHT;
import static com.persiki84.mediaplayer.island.IslandMeasure.PILL_TIME_GAP;
import static com.persiki84.mediaplayer.island.IslandMeasure.STAT_INSET;
import static com.persiki84.mediaplayer.island.IslandMeasure.TIME_SCALE;
import static com.persiki84.mediaplayer.island.IslandMeasure.TITLE_PILL_SCALE;

final class IslandPill {
    private static final float TITLE_TOP = 2.2f;
    private static final float ROW_CENTER = 6.2f;
    private static final float TIME_TOP = 10.6f;
    private static final float BAR_CENTER = 13.4f;
    private static final float HAIRLINE = 1.4f;
    private static final float SEEK_HEAD = 1.6f;
    private static final float TRACK_SHADE = 0.16f;
    private static final float MIN_SLOT = 4.0f;

    private IslandPill() {}

    static void draw(IslandScene scene, float x, float y, float alpha, float blur) {
        if (alpha <= 0.02f) return;

        GuiGraphics graphics = scene.graphics();
        IslandMeasure measure = scene.measure();
        if (measure.face() > 0.0f) {
            IslandFace.draw(graphics, x + PAD + FACE / 2.0f, y + PILL_HEIGHT / 2.0f, FACE, alpha, blur,
                    IslandHud.showsArt(measure));
        }
        mainLine(scene, x, y, alpha, blur);
        hairline(scene, x, y, alpha, blur);
        float fade = alpha * measure.media();
        if (measure.waveSlot() > 0.0f) {
            IslandGlyph.draw(graphics, x + measure.pillWidth() - PAD - IslandGlyph.PILL_WIDTH / 2.0f,
                    y + Anim.lerp(ROW_CENTER, PILL_HEIGHT / 2.0f, measure.blind()), IslandGlyph.PILL_WIDTH,
                    IslandGlyph.PILL_HEIGHT, fade, blur);
        }
    }

    // WHY: ник и название стоят каждый в своём покое своим кеглем, а переход между ними идёт позой:
    // WHY: уходящая строка едет и растёт к месту приходящей, приходящая выходит из места уходящей
    private static void mainLine(IslandScene scene, float x, float y, float alpha, float blur) {
        GuiGraphics graphics = scene.graphics();
        Font font = scene.font();
        IslandMeasure measure = scene.measure();
        float media = measure.media();
        float textX = x + PAD + measure.face() + GAP;
        float idleReserve = measure.stats() > 0.0f ? measure.stats() + STAT_INSET : 0.0f;
        float slot = x + measure.pillWidth() - PAD - Anim.lerp(idleReserve, measure.waveSlot(), media) - textX;
        if (slot <= MIN_SLOT) return;

        float nickTop = Ink.centerY(y, PILL_HEIGHT, NICK_SCALE);
        float titleTop = Anim.lerp(y + TITLE_TOP, Ink.centerY(y, PILL_HEIGHT, TITLE_PILL_SCALE), measure.blind());
        if (IslandSettings.on(IslandFlag.NICK) && media < 0.98f) {
            float scale = NICK_SCALE * Anim.lerp(1.0f, TITLE_PILL_SCALE / NICK_SCALE, media);
            Ink.label(graphics, font, IslandModel.nick(), textX, Anim.lerp(nickTop, titleTop, media), scale,
                    ink(alpha * (1.0f - media)), blur);
        }
        if (!IslandSettings.on(IslandFlag.TITLE) || media <= 0.02f) return;

        float scale = TITLE_PILL_SCALE * Anim.lerp(NICK_SCALE / TITLE_PILL_SCALE, 1.0f, media);
        float top = Anim.lerp(nickTop, titleTop, media);
        scene.titles().swapped(graphics, scene.titles().title, true, alpha * media, (Line line, float shown) ->
                Ink.line(graphics, font, line, textX, top, slot, scale, ink(shown), blur));
    }

    private static void hairline(IslandScene scene, float x, float y, float alpha, float blur) {
        IslandMeasure measure = scene.measure();
        IslandTitles titles = scene.titles();
        float fade = alpha * measure.media() * (1.0f - measure.blind());
        if (fade <= 0.02f || !IslandModel.track().present()) return;

        float left = x + PAD + measure.face() + GAP;
        float timed = 0.0f;
        if (IslandSettings.on(IslandFlag.TIME)) {
            Ink.label(scene.graphics(), scene.font(), titles.pillRow.value(), left, y + TIME_TOP, TIME_SCALE,
                    ink(fade), blur);
            timed = titles.pillRow.width(scene.font()) * TIME_SCALE + PILL_TIME_GAP;
        }
        float span = x + measure.pillWidth() - PAD - left - timed;
        if (!IslandSettings.on(IslandFlag.BAR) || titles.untimed() || span <= MIN_SLOT) return;

        float top = y + BAR_CENTER - HAIRLINE / 2.0f;
        int track = Colors.withAlpha(Palette.WHITE, TRACK_SHADE * fade);
        int fill = Colors.alpha(Palette.ACCENT, fade);
        IslandBar.hair(scene.graphics(), left + timed, top, span, HAIRLINE, track, fill, blur);
        IslandBar.seekHead(scene.graphics(), left + timed + span * IslandProgress.value(), y + BAR_CENTER,
                HAIRLINE * SEEK_HEAD, fade);
    }

    static int ink(float alpha) {
        return Colors.alpha(Palette.INK, alpha);
    }
}
