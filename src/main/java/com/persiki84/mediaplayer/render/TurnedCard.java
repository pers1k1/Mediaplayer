package com.persiki84.mediaplayer.render;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;

// WHY: картинка, повёрнутая вокруг вертикальной оси, рисуется вертикальными полосами с
// WHY: перспективой: точка на расстоянии x от оси уходит в глубину на x * sin(a) и делится на
// WHY: depth / (depth - x * sin(a)), поэтому ближний край выше дальнего. Полосы нужны, потому что
// WHY: один четырёхугольник с линейной текстурой ломал бы картинку по диагонали
public final class TurnedCard {
    private static final int STRIPS = 6;
    private static final float DEPTH_SHARE = 2.5f;
    private static final float FAINT = 0.004f;

    private TurnedCard() {}

    public record Face(GpuTextureView view, GpuSampler sampler, float radiusShare, float alpha, float blur) {}

    public static void draw(GuiGraphics graphics, Face face, float centerX, float centerY, float size, float angle) {
        if (face.alpha() <= FAINT || size <= 0.0f) return;

        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        float pixels = QuadArea.pixels(pose);
        for (int strip = 0; strip < STRIPS; strip++) {
            float from = -size / 2.0f + size * strip / STRIPS;
            strip(graphics, pose, face, centerX, centerY, size, angle, from, from + size / STRIPS, pixels);
        }
    }

    private static void strip(GuiGraphics graphics, Matrix3x2f pose, Face face, float centerX, float centerY,
                              float size, float angle, float from, float to, float pixels) {
        float half = size / 2.0f;
        float depth = size * DEPTH_SHARE;
        float nearScale = depth / (depth - from * (float) Math.sin(angle));
        float farScale = depth / (depth - to * (float) Math.sin(angle));
        float nearX = centerX + from * (float) Math.cos(angle) * nearScale;
        float farX = centerX + to * (float) Math.cos(angle) * farScale;
        float[] corners = {nearX, centerY - half * nearScale, nearX, centerY + half * nearScale,
                farX, centerY + half * farScale, farX, centerY - half * farScale};
        ScreenRectangle scissor = graphics.scissorStack.peek();
        float tallest = half * Math.max(nearScale, farScale);
        ScreenRectangle bounds = new QuadArea(Math.min(nearX, farX), centerY - tallest, Math.max(nearX, farX),
                centerY + tallest).bounds(pose, scissor);
        if (bounds == null) return;

        float u0 = (from + half) / size;
        float u1 = (to + half) / size;
        float[] texture = {u0, 0.0f, u0, 1.0f, u1, 1.0f, u1, 0.0f};
        float[] flat = {from * pixels, -half * pixels, from * pixels, half * pixels, to * pixels, half * pixels,
                to * pixels, -half * pixels};
        graphics.guiRenderState.submitGuiElement(new CardState(TextureSetup.singleTexture(face.view(), face.sampler()),
                pose, corners, texture, flat, half * pixels, face.radiusShare() * size * pixels, face.blur(),
                Colors.withAlpha(Palette.WHITE, face.alpha()), scissor, bounds));
    }
}
