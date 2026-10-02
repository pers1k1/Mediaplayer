package com.persiki84.mediaplayer.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.persiki84.mediaplayer.Mediaplayer;
import net.minecraft.client.renderer.RenderPipelines;

public final class GlassPipelines {
    public static final VertexFormat SHAPE_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("Local", VertexFormatElement.UV0)
            .add("HalfSize", VertexFormatElement.UV1)
            .add("Corner", VertexFormatElement.UV2)
            .build();

    public static final VertexFormat IMAGE_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("TexCoord", VertexFormatElement.UV0)
            .add("Local", VertexFormatElement.UV1)
            .add("HalfSize", VertexFormatElement.UV2)
            .add("Blur", VertexFormatElement.NORMAL)
            .padding(1)
            .add("Corner", VertexFormatElement.LINE_WIDTH)
            .build();

    public static final RenderPipeline GLASS = register("glass", SHAPE_FORMAT, true);
    public static final RenderPipeline SHAPE = register("shape", SHAPE_FORMAT, false);
    public static final RenderPipeline IMAGE = register("image", IMAGE_FORMAT, true);
    public static final RenderPipeline SOFT_TEXT = register("soft_text",
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, true);

    private GlassPipelines() {}

    public static void load() {
        Mediaplayer.LOGGER.debug("Pipelines ready: {}, {}, {}, {}", GLASS.getLocation(), SHAPE.getLocation(),
                IMAGE.getLocation(), SOFT_TEXT.getLocation());
    }

    private static RenderPipeline register(String name, VertexFormat format, boolean sampled) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                .withLocation(Mediaplayer.id("pipeline/" + name))
                .withVertexShader(Mediaplayer.id("core/" + name))
                .withFragmentShader(Mediaplayer.id("core/" + name))
                .withBlend(BlendFunction.TRANSLUCENT)
                .withVertexFormat(format, VertexFormat.Mode.QUADS)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
        if (sampled) builder.withSampler("Sampler0");
        return RenderPipelines.register(builder.build());
    }
}
