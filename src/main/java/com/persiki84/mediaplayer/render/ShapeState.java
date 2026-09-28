package com.persiki84.mediaplayer.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record ShapeState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        QuadArea quad,
        QuadArea shape,
        float pixels,
        float radius,
        float band,
        int topColor,
        int bottomColor,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer consumer) {
        corner(consumer, quad.left(), quad.top(), topColor);
        corner(consumer, quad.left(), quad.bottom(), bottomColor);
        corner(consumer, quad.right(), quad.bottom(), bottomColor);
        corner(consumer, quad.right(), quad.top(), topColor);
    }

    private void corner(VertexConsumer consumer, float x, float y, int color) {
        consumer.addVertexWith2DPose(pose, x, y)
                .setColor(color)
                .setUv((x - shape.centerX()) * pixels, (y - shape.centerY()) * pixels)
                .setUv1(QuadArea.quarter((shape.right() - shape.left()) * 0.5f * pixels),
                        QuadArea.quarter((shape.bottom() - shape.top()) * 0.5f * pixels))
                .setUv2(QuadArea.quarter(radius * pixels), QuadArea.quarter(band * pixels));
    }
}
