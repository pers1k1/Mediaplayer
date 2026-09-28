package com.persiki84.mediaplayer.screen;

import com.persiki84.mediaplayer.anim.FrameClock;
import com.persiki84.mediaplayer.anim.Spring;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import com.persiki84.mediaplayer.render.Ink;
import com.persiki84.mediaplayer.render.Paint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class GlassButton extends AbstractWidget {
    private static final float LABEL_SCALE = 0.8f;
    private static final float PRESS_SWELL = 0.04f;
    private static final int FACE = 0x26FFFFFF;
    private static final int FACE_LIT = 0x40FFFFFF;

    private final Runnable action;
    private final Spring hover = new Spring(0.2f, 1.0f, 0.0f);
    private final Spring press = new Spring(0.35f, 0.6f, 0.0f);

    GlassButton(int x, int y, int width, int height, Component label, Runnable action) {
        super(x, y, width, height, label);
        this.action = action;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubled) {
        press.kick(-6.0f);
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        float delta = FrameClock.delta();
        float lit = hover.to(isHoveredOrFocused() ? 1.0f : 0.0f, delta);
        float swell = 1.0f + PRESS_SWELL * press.to(0.0f, delta);
        float width = getWidth() * swell;
        float height = getHeight() * swell;
        float x = getX() + (getWidth() - width) / 2.0f;
        float y = getY() + (getHeight() - height) / 2.0f;
        int face = Colors.mix(FACE, FACE_LIT, lit);
        Paint.shape(graphics, x, y, width, height, height / 2.0f, face, Colors.mix(face, Palette.BLACK, 0.2f), 0.0f);
        Font font = Minecraft.getInstance().font;
        float labelWidth = Ink.width(font, getMessage(), LABEL_SCALE);
        Ink.label(graphics, font, getMessage(), getX() + (getWidth() - labelWidth) / 2.0f,
                Ink.centerY(getY(), getHeight(), LABEL_SCALE), LABEL_SCALE, Palette.INK, 0.0f);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
