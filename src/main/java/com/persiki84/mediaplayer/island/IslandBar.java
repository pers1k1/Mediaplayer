package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.render.Paint;
import net.minecraft.client.gui.GuiGraphics;

final class IslandBar {
    private static final float FILL_LIFT = 0.30f;
    private static final float SHEEN_LIFT = 0.42f;
    private static final float SHEEN_ALPHA = 0.20f;
    private static final float SHEEN_SHARE = 0.55f;
    private static final float HEAD_LIGHT = 0.55f;
    private static final float WELL_ALPHA = 0.9f;

    private IslandBar() {}

    static void hair(GuiGraphics graphics, float x, float y, float span, float thick, int track, int fill,
                     float blur) {
        Paint.shape(graphics, x, y, span, thick, thick / 2.0f, track, track, blur);
        float filled = span * IslandProgress.value();
        if (filled > 0.0f) Paint.shape(graphics, x, y, filled, thick, thick / 2.0f, fill, fill, blur);
    }

    static void well(GuiGraphics graphics, float x, float y, float span, float thick, float fade, float blur) {
        float shown = fade * WELL_ALPHA;
        Paint.shape(graphics, x, y, span, thick, thick / 2.0f, Colors.alpha(Palette.WELL_TOP, shown),
                Colors.alpha(Palette.WELL_BOTTOM, shown), blur);
        float filled = Math.max(0.0f, span * Anim.clamp01(IslandProgress.value()));
        if (filled <= 0.0f) return;

        float width = Math.max(filled, Math.min(thick, span));
        float grow = Math.min(1.0f, filled / thick);
        Paint.shape(graphics, x, y, width, thick, thick / 2.0f,
                Colors.alpha(Colors.lighten(Palette.ACCENT, FILL_LIFT), fade * grow), Colors.alpha(Palette.ACCENT,
                        fade * grow), blur);
        int sheen = Colors.lighten(Palette.ACCENT, SHEEN_LIFT) & 0x00FFFFFF;
        Paint.shape(graphics, x, y, width, thick * SHEEN_SHARE, thick / 2.0f,
                Colors.withAlpha(sheen, SHEEN_ALPHA * fade * grow), Colors.withAlpha(sheen, 0.0f), blur);
    }

    // WHY: голова живёт только на перемотке: на обычном ходу она стояла бы на кромке заливки
    // WHY: всегда и читалась бы как лишняя точка в полосе
    static void seekHead(GuiGraphics graphics, float centerX, float centerY, float radius, float fade) {
        float surge = IslandProgress.surge();
        if (surge <= 0.02f) return;

        Paint.dot(graphics, centerX, centerY, radius * (0.6f + 0.4f * surge),
                Colors.alpha(Colors.mix(Palette.ACCENT, Palette.WHITE, HEAD_LIGHT), fade * surge));
    }
}
