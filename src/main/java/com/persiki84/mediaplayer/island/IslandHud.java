package com.persiki84.mediaplayer.island;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.anim.FrameClock;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandPlacement;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.render.Paint;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import static com.persiki84.mediaplayer.island.IslandMeasure.CAPSULE_GAP;
import static com.persiki84.mediaplayer.island.IslandMeasure.CARD_HEIGHT;
import static com.persiki84.mediaplayer.island.IslandMeasure.PILL_HEIGHT;

public final class IslandHud {
    private static final IslandTitles titles = new IslandTitles();
    private static final IslandMeasure measure = new IslandMeasure();
    private static final IslandMotion motion = new IslandMotion();

    private static long preparedFrame = -1L;
    private static boolean primed;

    private IslandHud() {}

    public static void render(GuiGraphics graphics, DeltaTracker tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            IslandBounds.hide();
            return;
        }
        prepare(minecraft.font);
        place(new IslandScene(graphics, minecraft.font, titles, measure));
    }

    private static void prepare(Font font) {
        long frame = FrameClock.frame();
        if (frame == preparedFrame) return;

        preparedFrame = frame;
        IslandModel.advanceFrame();
        IslandGlyph.pulse(IslandModel.energy());
        float delta = FrameClock.delta();
        boolean announced = titles.refresh(IslandModel.track(), IslandModel.media() > 0.02f, delta);
        measure.measure(font, titles, IslandCounter.width(font));
        boolean card = IslandModel.carded() && measure.media() > 0.02f;
        if (!primed) {
            primed = true;
            motion.snap(card, measure);
            return;
        }
        if (announced && (motion.opened() || !IslandSettings.on(IslandFlag.CARD))) motion.pulse();
        motion.advance(card, measure, delta);
    }

    private static void place(IslandScene scene) {
        GuiGraphics graphics = scene.graphics();
        IslandPlacement placement = IslandSettings.placement();
        float scale = placement.scale();
        float span = Math.max(motion.width(), measure.capsule()) * scale;
        float tall = (motion.height() + tail()) * scale;
        float center = Anim.clamp(placement.centerShare() * graphics.guiWidth(), span / 2.0f,
                Math.max(span / 2.0f, graphics.guiWidth() - span / 2.0f));
        float top = Anim.clamp(placement.topShare() * graphics.guiHeight(), 0.0f,
                Math.max(0.0f, graphics.guiHeight() - CARD_HEIGHT * scale));
        IslandBounds.set(center - span / 2.0f, top, span, tall);
        graphics.pose().pushMatrix();
        graphics.pose().translate(center, top);
        graphics.pose().scale(scale, scale);
        try {
            paint(scene, -motion.width() / 2.0f, 0.0f);
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static float tail() {
        return measure.capsule() > 0.0f ? Anim.easeOutBack(measure.media()) * (CAPSULE_GAP + PILL_HEIGHT) : 0.0f;
    }

    private static void paint(IslandScene scene, float x, float y) {
        GuiGraphics graphics = scene.graphics();
        float width = motion.width();
        float height = motion.height();
        Paint.glass(graphics, x, y, width, height, motion.radius(), 1.0f);
        clip(graphics, x, y, width, height, () -> {
            layer(graphics, x + width / 2.0f, y + height / 2.0f, motion.pillScale(measure), () ->
                    IslandPill.draw(scene, x + (width - measure.pillWidth()) / 2.0f, y + (height - PILL_HEIGHT) / 2.0f,
                            motion.pillAlpha(), motion.pillBlur()));
            layer(graphics, x + width / 2.0f, y + height / 2.0f, motion.cardScale(measure), () ->
                    IslandCard.draw(scene, x + (width - measure.cardWidth()) / 2.0f, y + (height - CARD_HEIGHT) / 2.0f,
                            motion.cardAlpha(), motion.cardBlur()));
        });
        IslandCounter.draw(scene, x, y, width, height, 1.0f);
    }

    // WHY: содержимое каждого состояния стоит в своей итоговой раскладке и масштабируется вместе с
    // WHY: формой вокруг её центра, как у острова iPhone: оно растёт вместе со стеклом, а не выезжает
    private static void layer(GuiGraphics graphics, float pivotX, float pivotY, float scale, Runnable body) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(pivotX, pivotY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-pivotX, -pivotY);
        try {
            body.run();
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static void clip(GuiGraphics graphics, float x, float y, float width, float height, Runnable body) {
        graphics.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + width),
                (int) Math.ceil(y + height));
        try {
            body.run();
        } finally {
            graphics.disableScissor();
        }
    }

    // WHY: обложка живёт в том же месте, что и голова игрока, поэтому выключенная голова не должна
    // WHY: уносить её с собой
    static boolean showsArt(IslandMeasure current) {
        return IslandSettings.on(IslandFlag.COVER) && current.media() > 0.02f && current.blind() < 0.5f
                && IslandArt.ready();
    }
}
