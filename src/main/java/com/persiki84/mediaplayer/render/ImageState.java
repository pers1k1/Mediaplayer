package com.persiki84.mediaplayer.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record ImageState(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        QuadArea quad,
        UvArea uv,
        float pixels,
        float radius,
        float blur,
        int color,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public record UvArea(float u0, float v0, float u1, float v1) {}

    @Override
    public RenderPipeline pipeline() {
        return GlassPipelines.IMAGE;
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        corner(consumer, quad.left(), quad.top(), uv.u0(), uv.v0());
        corner(consumer, quad.left(), quad.bottom(), uv.u0(), uv.v1());
        corner(consumer, quad.right(), quad.bottom(), uv.u1(), uv.v1());
        corner(consumer, quad.right(), quad.top(), uv.u1(), uv.v0());
    }

    private void corner(VertexConsumer consumer, float x, float y, float u, float v) {
        consumer.addVertexWith2DPose(pose, x, y)
                .setColor(color)
                .setUv(u, v)
                .setUv1(QuadArea.quarter((x - quad.centerX()) * pixels),
                        QuadArea.quarter((y - quad.centerY()) * pixels))
                .setUv2(QuadArea.quarter((quad.right() - quad.left()) * 0.5f * pixels),
                        QuadArea.quarter((quad.bottom() - quad.top()) * 0.5f * pixels))
                .setNormal(blur, 0.0f, 0.0f)
                .setLineWidth(radius * pixels);
    }
}
