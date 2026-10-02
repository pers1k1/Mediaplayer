package com.persiki84.mediaplayer.screen;

import com.persiki84.mediaplayer.anim.Anim;
import com.persiki84.mediaplayer.anim.FrameClock;
import com.persiki84.mediaplayer.anim.Spring;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.config.IslandFlag;
import com.persiki84.mediaplayer.config.IslandSettings;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Paint;
import com.persiki84.mediaplayer.render.Weight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class GlassToggle extends AbstractWidget {
    private static final float TRACK_WIDTH = 18.0f;
    private static final float TRACK_HEIGHT = 10.0f;
    private static final float KNOB_INSET = 1.5f;
    private static final float LABEL_SCALE = 0.8f;
    private static final float KNOB_RESPONSE = 0.32f;
    private static final float KNOB_DAMPING = 0.72f;
    private static final int TRACK_OFF = 0x40FFFFFF;

    private final IslandFlag flag;
    private final Spring knob;
    private final Spring hover = new Spring(0.2f, 1.0f, 0.0f);

    GlassToggle(int x, int y, int width, int height, IslandFlag flag) {
        super(x, y, width, height, Component.translatable(flag.translationKey()));
        this.flag = flag;
        this.knob = new Spring(KNOB_RESPONSE, KNOB_DAMPING, IslandSettings.on(flag) ? 1.0f : 0.0f);
        setTooltip(Tooltip.create(Component.translatable(flag.hintKey())));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        IslandSettings.set(flag, !IslandSettings.on(flag));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        float delta = FrameClock.delta();
        float on = knob.to(IslandSettings.on(flag) ? 1.0f : 0.0f, delta);
        float lit = hover.to(isHoveredOrFocused() ? 1.0f : 0.0f, delta);
        Ink.label(graphics, Minecraft.getInstance().font, getMessage(), Weight.REGULAR, getX(),
                Ink.centerY(getY(), getHeight(), Ink.fit(LABEL_SCALE)), Ink.fit(LABEL_SCALE),
                Colors.mix(Palette.INK_DIM, Palette.INK, Anim.clamp01(lit)), 0.0f);
        track(graphics, on, lit);
    }

    // WHY: бегунок идёт пружиной с небольшим перелётом, как переключатель iOS: он дотягивается до
    // WHY: края и чуть отыгрывает, а заливка дорожки идёт за ним той же долей
    private void track(GuiGraphics graphics, float on, float lit) {
        float left = getX() + getWidth() - TRACK_WIDTH;
        float top = getY() + (getHeight() - TRACK_HEIGHT) / 2.0f;
        int fill = Colors.mix(TRACK_OFF, Palette.ACCENT, Anim.clamp01(on));
        Paint.shape(graphics, left, top, TRACK_WIDTH, TRACK_HEIGHT, TRACK_HEIGHT / 2.0f, fill,
                Colors.mix(fill, Palette.BLACK, 0.12f), 0.0f);
        float knobSize = TRACK_HEIGHT - KNOB_INSET * 2.0f;
        float travel = TRACK_WIDTH - KNOB_INSET * 2.0f - knobSize;
        float knobX = left + KNOB_INSET + travel * on;
        Paint.dot(graphics, knobX + knobSize / 2.0f, top + TRACK_HEIGHT / 2.0f, knobSize / 2.0f + 0.3f * lit,
                Palette.WHITE);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
