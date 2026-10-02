package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Paint;
import com.persiki84.mediaplayer.render.Weight;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import static com.persiki84.mediaplayer.island.IslandMeasure.CAPSULE_GAP;
import static com.persiki84.mediaplayer.island.IslandMeasure.CAPSULE_PAD;
import static com.persiki84.mediaplayer.island.IslandMeasure.PAD;
import static com.persiki84.mediaplayer.island.IslandMeasure.PILL_HEIGHT;

// WHY: счётчик один и тот же в обоих состояниях и переезжает, а не гаснет: без музыки живёт внутри
// WHY: острова справа, с музыкой съезжает под него в свою капсулу. Вниз уходит с перелётом, вбок
// WHY: доезжает без него, поэтому он выпадает, а потом встаёт на место
final class IslandCounter {
    private static final float STAT_SCALE = 0.84f;
    private static final float UNIT_SCALE = 0.72f;
    private static final float UNIT_GAP = 2.5f;
    private static final float PAIR_GAP = 7.0f;
    private static final float BAND = 10.5f;
    private static final float GOOD_PING = 0.66f;
    private static final float SLOW_PING = 0.33f;
    private static final float MAX_RADIUS = 13.0f;
    private static final Component FPS_UNIT = Component.literal("FPS");
    private static final Component PING_UNIT = Component.literal("Ping");

    private IslandCounter() {}

    static float width(Font font) {
        float span = 0.0f;
        if (IslandSettings.on(IslandFlag.FPS)) {
            span += pairWidth(font, FPS_UNIT, IslandModel.frames()) + PAIR_GAP;
        }
        if (!IslandSettings.on(IslandFlag.PING)) return Math.max(0.0f, span - PAIR_GAP);

        return span + pairWidth(font, PING_UNIT, IslandModel.latency());
    }

    private static float pairWidth(Font font, Component unit, Component value) {
        return Ink.width(font, unit, Weight.REGULAR, Ink.fit(UNIT_SCALE)) + UNIT_GAP
                + Ink.width(font, value, Weight.SEMIBOLD, Ink.fit(STAT_SCALE));
    }

    static void draw(IslandScene scene, float x, float y, float width, float height, float alpha) {
        IslandMeasure measure = scene.measure();
        if (measure.capsule() <= 0.0f || alpha <= 0.02f) return;

        float media = measure.media();
        float capsuleLeft = x + (width - measure.capsule()) / 2.0f + CAPSULE_PAD;
        float statsX = Anim.lerp(x + width - PAD - measure.stats(), capsuleLeft, Anim.easeOut(media));
        float dropped = y + height + CAPSULE_GAP + PILL_HEIGHT / 2.0f;
        float centerY = Anim.lerp(y + height / 2.0f, dropped, Anim.easeOutBack(media));
        if (media > 0.02f) {
            Paint.glass(scene.graphics(), statsX - CAPSULE_PAD, centerY - PILL_HEIGHT / 2.0f, measure.capsule(),
                    PILL_HEIGHT, Math.min(PILL_HEIGHT / 2.0f, MAX_RADIUS), alpha * media);
        }
        stats(scene.graphics(), scene.font(), statsX, centerY, alpha);
    }

    private static void stats(GuiGraphics graphics, Font font, float x, float centerY, float alpha) {
        float unitY = Ink.centerY(centerY - BAND, BAND * 2.0f, Ink.fit(UNIT_SCALE));
        float valueY = Ink.centerY(centerY - BAND, BAND * 2.0f, Ink.fit(STAT_SCALE));
        float cursor = x;
        if (IslandSettings.on(IslandFlag.FPS)) {
            cursor = pair(graphics, font, FPS_UNIT, IslandModel.frames(), cursor, unitY, valueY,
                    IslandContent.ink(alpha), IslandContent.ink(alpha)) + PAIR_GAP;
        }
        if (IslandSettings.on(IslandFlag.PING)) {
            pair(graphics, font, PING_UNIT, IslandModel.latency(), cursor, unitY, valueY, IslandContent.ink(alpha),
                    Colors.alpha(pingTone(), alpha));
        }
    }

    private static float pair(GuiGraphics graphics, Font font, Component unit, Component value, float x,
                              float unitY, float valueY, int unitInk, int valueInk) {
        Ink.label(graphics, font, unit, Weight.REGULAR, x, unitY, Ink.fit(UNIT_SCALE), unitInk, 0.0f);
        float valueX = x + Ink.width(font, unit, Weight.REGULAR, Ink.fit(UNIT_SCALE)) + UNIT_GAP;
        Ink.label(graphics, font, value, Weight.SEMIBOLD, valueX, valueY, Ink.fit(STAT_SCALE), valueInk, 0.0f);
        return valueX + Ink.width(font, value, Weight.SEMIBOLD, Ink.fit(STAT_SCALE));
    }

    private static int pingTone() {
        float quality = IslandModel.quality();
        if (quality >= GOOD_PING) return Palette.INK;
        return quality >= SLOW_PING ? Palette.PING_SLOW : Palette.PING_BAD;
    }
}
