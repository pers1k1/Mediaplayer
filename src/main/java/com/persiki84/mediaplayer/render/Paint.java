package com.persiki84.mediaplayer.render;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.persiki84.mediaplayer.color.Colors;
import com.persiki84.mediaplayer.color.Palette;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;

public final class Paint {
    private static final float GLASS_SLACK = 2.0f;
    private static final float GLASS_BAND = 11.0f;
    private static final float BAND_SHARE = 0.26f;
    private static final float BLUR_SHARE = 0.5f;
    private static final float BLUR_UNITS = 3.0f;
    private static final float EDGE_SLACK = 1.0f;
    private static final float FAINT = 0.004f;

    private Paint() {}

    public static void glass(GuiGraphics graphics, float x, float y, float width, float height, float radius,
                             float alpha) {
        glass(graphics, x, y, width, height, radius, alpha, 0.0f);
    }

    public static void glass(GuiGraphics graphics, float x, float y, float width, float height, float radius,
                             float alpha, float blur) {
        if (width <= 0.0f || height <= 0.0f || alpha <= FAINT) return;

        TextureSetup backdrop = GlassBackdrop.claim();
        if (backdrop == null) {
            shape(graphics, x, y, width, height, radius, Palette.SHADE, Palette.SHADE, blur);
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        float slack = GLASS_SLACK + blur * Math.min(width, height) * BLUR_SHARE;
        QuadArea quad = QuadArea.of(x, y, width, height, slack);
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = quad.bounds(pose, scissor);
        if (bounds == null) return;

        float corner = Math.min(radius, Math.min(width, height) / 2.0f);
        float band = Math.min(GLASS_BAND, Math.min(width, height) * BAND_SHARE);
        graphics.guiRenderState.submitGuiElement(new PaneState(backdrop, pose, quad,
                new QuadArea(x, y, x + width, y + height), QuadArea.pixels(pose), corner, band, blur,
                Colors.withAlpha(Palette.WHITE, alpha), scissor, bounds));
    }

    public static void shape(GuiGraphics graphics, float x, float y, float width, float height, float radius,
                             int top, int bottom, float blur) {
        strip(graphics, new QuadArea(x, y, x + width, y + height), y, y + height, radius, top, bottom, blur);
    }

    public static void ramp(GuiGraphics graphics, float x, float y, float width, float height, float radius,
                            int[] stops, float blur) {
        QuadArea shape = new QuadArea(x, y, x + width, y + height);
        float step = height / (stops.length - 1);
        for (int stop = 0; stop < stops.length - 1; stop++) {
            strip(graphics, shape, y + step * stop, y + step * (stop + 1), radius, stops[stop], stops[stop + 1], blur);
        }
    }

    public static void dot(GuiGraphics graphics, float centerX, float centerY, float radius, int color) {
        shape(graphics, centerX - radius, centerY - radius, radius * 2.0f, radius * 2.0f, radius, color, color, 0.0f);
    }

    // WHY: многоступенчатый градиент режется на горизонтальные полосы с общей формой: каждая полоса
    // WHY: считает расстояние до всей фигуры, поэтому скругление целое, а цвет идёт по ступеням
    private static void strip(GuiGraphics graphics, QuadArea shape, float from, float to, float radius,
                              int top, int bottom, float blur) {
        if (shape.right() <= shape.left() || to <= from || ((top | bottom) >>> 24) == 0) return;

        float band = blur * BLUR_UNITS;
        float slack = band + EDGE_SLACK;
        QuadArea quad = new QuadArea(shape.left() - slack, from <= shape.top() ? from - slack : from,
                shape.right() + slack, to >= shape.bottom() ? to + slack : to);
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = quad.bounds(pose, scissor);
        if (bounds == null) return;

        graphics.guiRenderState.submitGuiElement(new ShapeState(GlassPipelines.SHAPE, TextureSetup.noTexture(),
                pose, quad, shape, QuadArea.pixels(pose), radius, band, top, bottom, scissor, bounds));
    }

    public static void image(GuiGraphics graphics, GpuTextureView view, GpuSampler sampler, QuadArea quad,
                             ImageState.UvArea uv, float radius, float alpha, float blur) {
        if (alpha <= FAINT || quad.right() <= quad.left()) return;

        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        ScreenRectangle bounds = quad.bounds(pose, scissor);
        if (bounds == null) return;

        graphics.guiRenderState.submitGuiElement(new ImageState(TextureSetup.singleTexture(view, sampler), pose,
                quad, uv, QuadArea.pixels(pose), radius, blur, Colors.withAlpha(Palette.WHITE, alpha), scissor,
                bounds));
    }
}
