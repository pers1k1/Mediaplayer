package com.persiki84.mediaplayer.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

// WHY: полоса повёрнутой картинки: четыре угла на экране произвольные (трапеция перспективы), а
// WHY: координаты скругления берутся из плоскости самой картинки, поэтому углы остаются круглыми
public record CardState(
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float[] corners,
        float[] texture,
        float[] flat,
        float halfPixels,
        float radiusPixels,
        float blur,
        int color,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    @Override
    public RenderPipeline pipeline() {
        return GlassPipelines.IMAGE;
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        for (int corner = 0; corner < 4; corner++) {
            consumer.addVertexWith2DPose(pose, corners[corner * 2], corners[corner * 2 + 1])
                    .setColor(color)
                    .setUv(texture[corner * 2], texture[corner * 2 + 1])
                    .setUv1(QuadArea.quarter(flat[corner * 2]), QuadArea.quarter(flat[corner * 2 + 1]))
                    .setUv2(QuadArea.quarter(halfPixels), QuadArea.quarter(halfPixels))
                    .setNormal(blur, 0.0f, 0.0f)
                    .setLineWidth(radiusPixels);
        }
    }
}
